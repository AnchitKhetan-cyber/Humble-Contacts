package com.humblesolutions.humblecontacts.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.humblesolutions.humblecontacts.data.model.Contact
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class  ContactRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val uid get() = auth.currentUser?.uid ?: ""

    private val storage = FirebaseStorage.getInstance()

    private val storageRef = storage.reference

    fun getContactsRealtime(onResult: (List<Contact>) -> Unit) {

        Log.d("CONTACT_DEBUG", "Current UID = $uid")

        db.collection("contacts")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e(
                        "CONTACT_DEBUG",
                        error.message ?: "Unknown Firestore error"
                    )
                    return@addSnapshotListener
                }

                val contacts = snapshot?.documents
                    ?.mapNotNull { doc ->
                        try {
                            doc.toObject(Contact::class.java)
                        } catch (e: Exception) {
                            Log.e("CONTACT_DEBUG", "Skipping ${doc.id}", e)
                            null
                        }
                    }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()

                Log.d(
                    "CONTACT_DEBUG",
                    "Contacts found = ${contacts.size}"
                )

                onResult(contacts)
            }
    }

    suspend fun addContact(contact: Contact): Boolean {

        val existingEmail =
            if (contact.email.isNotBlank()) {
                db.collection("contacts")
                    .whereEqualTo("ownerId", uid)
                    .whereEqualTo("email", contact.email)
                    .get()
                    .await()
            } else null

        if (existingEmail != null && !existingEmail.isEmpty) {
            return false
        }

        val existingPhone =
            if (contact.phone.isNotBlank()) {
                db.collection("contacts")
                    .whereEqualTo("ownerId", uid)
                    .whereEqualTo("phone", contact.phone)
                    .get()
                    .await()
            } else null

        if (existingPhone != null && !existingPhone.isEmpty) {
            return false
        }

        val ref = db.collection("contacts").document()

        val contactWithId = contact.copy(
            contactId = ref.id,
            ownerId = uid,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        var finalContact = contactWithId

        if (contact.businessCardImage.isNotBlank()) {

            val imageUrl = uploadBusinessCard(
                ownerId = uid,
                contactId = contactWithId.contactId,
                imageUri = Uri.parse(contact.businessCardImage)
            )

            finalContact = contactWithId.copy(
                businessCardImage = imageUrl
            )
        }

        ref.set(
            finalContact
        ).await()

        return true
    }

    suspend fun deleteContact(contactId: String) {
        db.collection("contacts").document(contactId).delete().await()
    }

    suspend fun toggleFavourite(contactId: String, isFavourite: Boolean) {
        db.collection("contacts").document(contactId)
            .update("favourite", isFavourite, "updatedAt", Timestamp.now())
            .await()
    }

    suspend fun updateContact(contact: Contact) {
        db.collection("contacts").document(contact.contactId)
            .set(contact.copy(updatedAt = com.google.firebase.Timestamp.now()))
            .await()
    }

    // NOTE on account deletion: these three take an explicit [ownerUid] because
    // account deletion removes the Firebase Auth user FIRST, after which
    // `auth.currentUser` is null and the `uid` getter would return "" — targeting
    // the wrong document paths. Callers must capture the uid before Auth deletion.

    suspend fun deleteAllContacts(ownerUid: String = uid) {

        val snapshot = db.collection("contacts")
            .whereEqualTo("ownerId", ownerUid)
            .get()
            .await()

        val batch = db.batch()

        snapshot.documents.forEach { document ->
            batch.delete(document.reference)
        }

        batch.commit().await()
    }

    suspend fun deleteUserDocument(ownerUid: String = uid) {
        db.collection("users")
            .document(ownerUid)
            .delete()
            .await()
    }

    /**
     * Best-effort deletion of the user's uploaded business-card images at
     * `business_cards/<uid>/…`. Cloud Storage has no folder delete, so we list
     * the folder and delete each file. Callers should treat failures as non-fatal.
     */
    suspend fun deleteBusinessCardImages(ownerUid: String = uid) {
        val folder = storageRef.child("business_cards").child(ownerUid)
        val listing = folder.listAll().await()
        listing.items.forEach { item ->
            item.delete().await()
        }
    }

    /**
     * Replace a duplicate contact by **updating the existing document in place**,
     * merging only the newly captured fields.
     *
     * The old behaviour deleted the original and called [addContact], which minted
     * a fresh `contactId` and dropped everything not on the incoming object — notes,
     * media, favourite, tags — and broke any QR/deep link to the old id (ticket #8).
     *
     * Here we locate the duplicate, then run a single Firestore transaction that
     * reads the existing document and writes a merged copy: the scanned fields
     * overwrite, while `contactId`, `ownerId`, `createdAt`, `favourite`, `tags`,
     * `media` and the prior conversation notes are preserved. Because it is one
     * transactional write (no delete step), there is no window where the contact is
     * missing — a mid-way failure leaves the original intact.
     */
    suspend fun replaceContact(contact: Contact): Boolean {
        // Locate the existing duplicate the same way addContact flags one.
        val byEmail = if (contact.email.isNotBlank()) {
            db.collection("contacts")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("email", contact.email)
                .get().await()
        } else null

        val byPhone = if (contact.phone.isNotBlank()) {
            db.collection("contacts")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("phone", contact.phone)
                .get().await()
        } else null

        // No duplicate actually present — fall back to a normal fresh insert.
        val existingId = byEmail?.documents?.firstOrNull()?.id
            ?: byPhone?.documents?.firstOrNull()?.id
            ?: return addContact(contact)

        val ref = db.collection("contacts").document(existingId)

        // Upload a freshly scanned business card first — outside the transaction,
        // since Cloud Storage isn't transactional and the transaction body may be
        // retried. Key it to the preserved id so the storage path stays stable.
        val newCardUrl = if (contact.businessCardImage.isNotBlank()) {
            uploadBusinessCard(
                ownerId = uid,
                contactId = existingId,
                imageUri = Uri.parse(contact.businessCardImage)
            )
        } else null

        return db.runTransaction { transaction ->
            val existing = transaction.get(ref).toObject(Contact::class.java)
                ?: return@runTransaction false

            val merged = existing.copy(
                // Newly captured fields overwrite.
                fullName = contact.fullName,
                jobRole = contact.jobRole,
                company = contact.company,
                email = contact.email,
                phone = contact.phone,
                linkedIn = contact.linkedIn,
                address = contact.address,
                meetingDate = contact.meetingDate ?: existing.meetingDate,
                entryMethod = contact.entryMethod,
                businessCardImage = newCardUrl ?: existing.businessCardImage,
                // Keep prior conversation history; append any note captured now.
                conversationNotes = existing.conversationNotes + contact.conversationNotes,
                updatedAt = Timestamp.now()
                // Preserved unchanged by copy(): contactId, ownerId, createdAt,
                // favourite, tags, media, industry, meetingLocation, eventName.
            )

            transaction.set(ref, merged)
            true
        }.await()
    }


    suspend fun uploadBusinessCard(
        ownerId: String,
        contactId: String,
        imageUri: Uri
    ): String {

        val imageRef = storageRef
            .child("business_cards")
            .child(ownerId)
            .child("$contactId.jpg")

        imageRef.putFile(imageUri).await()

        val downloadUrl = imageRef.downloadUrl.await().toString()

        return downloadUrl
    }
}