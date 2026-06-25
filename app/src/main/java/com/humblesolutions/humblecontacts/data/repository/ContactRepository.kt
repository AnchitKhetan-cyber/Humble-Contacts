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

    suspend fun deleteAllContacts() {

        val snapshot = db.collection("contacts")
            .whereEqualTo("ownerId", uid)
            .get()
            .await()

        val batch = db.batch()

        snapshot.documents.forEach { document ->
            batch.delete(document.reference)
        }

        batch.commit().await()
    }

    suspend fun deleteUserDocument() {
        db.collection("users")
            .document(uid)
            .delete()
            .await()
    }

    suspend fun replaceContact(contact: Contact): Boolean {
        // Find existing by email or phone and delete it
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

        val existingId = byEmail?.documents?.firstOrNull()?.id
            ?: byPhone?.documents?.firstOrNull()?.id

        if (existingId != null) {
            db.collection("contacts").document(existingId).delete().await()
        }

        // Now insert as fresh contact (reuse addContact logic)
        return addContact(contact)
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