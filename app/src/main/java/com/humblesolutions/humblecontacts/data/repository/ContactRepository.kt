package com.humblesolutions.humblecontacts.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.humblesolutions.humblecontacts.data.model.Contact
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

/**
 * Outcome of [ContactRepository.addContact] (ticket #27) — lets the UI tell a
 * duplicate apart from a genuine write failure, and name the field that matched.
 */
sealed interface AddContactResult {
    data class Success(val contactId: String) : AddContactResult
    /** A contact already matches on [field] ("email" or "phone") with [value]. */
    data class Duplicate(val field: String, val value: String, val existingId: String) : AddContactResult
    data class Error(val cause: Throwable) : AddContactResult
}

class  ContactRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val uid get() = auth.currentUser?.uid ?: ""

    private val storage = FirebaseStorage.getInstance()

    private val storageRef = storage.reference

    /**
     * Registers a realtime listener for the current user's contacts, newest first,
     * capped at [limit] documents. Sorting is done server-side via `orderBy`, and
     * paging is done by re-registering with a larger [limit] as the user scrolls
     * (ticket #25) — so large lists never load entirely into memory.
     *
     * Returns the [ListenerRegistration] so the caller can detach it. Callers
     * **must** hold the returned registration and call [ListenerRegistration.remove]
     * before re-registering (and when their scope is cleared); discarding it leaks
     * the listener, stacking duplicate callbacks and billable reads (ticket #9).
     *
     * NOTE: the `ownerId` equality + `createdAt` order requires the composite index
     * in `firestore.indexes.json`; the query errors until that index is deployed.
     */
    fun getContactsRealtime(
        limit: Long,
        onResult: (List<Contact>) -> Unit
    ): ListenerRegistration {

        Log.d("CONTACT_DEBUG", "Current UID = $uid, limit = $limit")

        return db.collection("contacts")
            .whereEqualTo("ownerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e(
                        "CONTACT_DEBUG",
                        error.message ?: "Unknown Firestore error"
                    )
                    return@addSnapshotListener
                }

                // Already ordered server-side; keep Firestore's order.
                val contacts = snapshot?.documents
                    ?.mapNotNull { doc ->
                        try {
                            doc.toObject(Contact::class.java)
                        } catch (e: Exception) {
                            Log.e("CONTACT_DEBUG", "Skipping ${doc.id}", e)
                            null
                        }
                    }
                    ?: emptyList()

                Log.d(
                    "CONTACT_DEBUG",
                    "Contacts found = ${contacts.size}"
                )

                onResult(contacts)
            }
    }

    /**
     * Adds a contact, distinguishing the three outcomes the UI must tell apart
     * (ticket #27): [AddContactResult.Success], [AddContactResult.Duplicate]
     * (naming the field that actually matched — email or phone, never "name"),
     * and [AddContactResult.Error] for a write/network failure — so a failure is
     * never mistaken for a duplicate and the save is never silently dropped.
     */
    suspend fun addContact(contact: Contact): AddContactResult {
        return try {
            if (contact.email.isNotBlank()) {
                val existingEmail = db.collection("contacts")
                    .whereEqualTo("ownerId", uid)
                    .whereEqualTo("email", contact.email)
                    .get()
                    .await()

                if (!existingEmail.isEmpty) {
                    return AddContactResult.Duplicate(
                        field = "email",
                        value = contact.email,
                        existingId = existingEmail.documents.first().id
                    )
                }
            }

            // Only match on phone when an actual number was entered. An empty field
            // can be stored as a bare dial code (e.g. "+91 "), which is non-blank but
            // carries no digits — matching on it would flag every phoneless contact
            // as a duplicate of the first. Require at least one digit.
            if (contact.phone.any { it.isDigit() }) {
                val existingPhone = db.collection("contacts")
                    .whereEqualTo("ownerId", uid)
                    .whereEqualTo("phone", contact.phone)
                    .get()
                    .await()

                if (!existingPhone.isEmpty) {
                    return AddContactResult.Duplicate(
                        field = "phone",
                        value = contact.phone,
                        existingId = existingPhone.documents.first().id
                    )
                }
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

            // Do NOT await the write's server acknowledgement. With offline
            // persistence the write is applied to the local cache immediately (and
            // the realtime listener reflects it right away), but the returned Task
            // only completes once the server confirms — so awaiting it hangs the UI
            // on "Saving…" until reconnect (ticket #28). Fire it and treat the local
            // commit as success; a genuine sync failure is logged.
            ref.set(finalContact)
                .addOnFailureListener { Log.e("CONTACT_DEBUG", "Contact failed to sync to server", it) }

            AddContactResult.Success(ref.id)
        } catch (e: Exception) {
            // Network / Firestore failure — surfaced as a real error so it's never
            // mistaken for a duplicate and the user can retry.
            Log.e("CONTACT_DEBUG", "addContact failed", e)
            AddContactResult.Error(e)
        }
    }

    suspend fun deleteContact(contactId: String) {
        db.collection("contacts").document(contactId).delete().await()

        // Best-effort: remove this contact's card image so it doesn't orphan in
        // Storage (privacy + cost). A Storage failure must not fail the delete —
        // the onDocumentDeleted Cloud Function is the server-side backstop.
        // Guard a blank uid: it would build the malformed path
        // `business_cards//$contactId.jpg` and target the wrong prefix. If we have
        // no signed-in uid the backstop function still covers the cleanup.
        val ownerUid = uid
        if (ownerUid.isBlank()) {
            Log.w("CONTACT_DEBUG", "No uid; leaving card image cleanup for $contactId to the backstop")
        } else {
            try {
                storageRef
                    .child("business_cards")
                    .child(ownerUid)
                    .child("$contactId.jpg")
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w("CONTACT_DEBUG", "Card image delete skipped for $contactId", e)
            }
        }
    }

    suspend fun toggleFavourite(contactId: String, isFavourite: Boolean) {
        db.collection("contacts").document(contactId)
            .update("favourite", isFavourite, "updatedAt", Timestamp.now())
            .await()
    }

    suspend fun updateContact(contact: Contact) {
        // Fire-and-forget for the same offline reason as addContact (#28): the
        // local cache updates instantly and the listener reflects it, but awaiting
        // the server ack would hang the edit screen on "Updating…" until reconnect.
        db.collection("contacts").document(contact.contactId)
            .set(contact.copy(updatedAt = com.google.firebase.Timestamp.now()))
            .addOnFailureListener { Log.e("CONTACT_DEBUG", "Contact update failed to sync", it) }
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

        // Best-effort: also clear this user's card images so none orphan in
        // Storage. Reuses the whole-folder helper (correct here, since every
        // contact is being removed). Non-fatal if it fails.
        try {
            deleteBusinessCardImages(ownerUid)
        } catch (e: Exception) {
            Log.w("CONTACT_DEBUG", "Bulk card image cleanup skipped for $ownerUid", e)
        }
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
     *
     * Each item delete is isolated: one failing object (already gone, transient
     * error) is logged and skipped so it can't abort the loop and leave the
     * remaining images orphaned.
     */
    suspend fun deleteBusinessCardImages(ownerUid: String = uid) {
        val folder = storageRef.child("business_cards").child(ownerUid)
        val listing = folder.listAll().await()
        listing.items.forEach { item ->
            try {
                item.delete().await()
            } catch (e: Exception) {
                Log.w("CONTACT_DEBUG", "Card image delete skipped for ${item.path}", e)
            }
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

        // Same digit guard as addContact: never match on a digit-less phone.
        val byPhone = if (contact.phone.any { it.isDigit() }) {
            db.collection("contacts")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("phone", contact.phone)
                .get().await()
        } else null

        // No duplicate actually present — fall back to a normal fresh insert.
        val existingId = byEmail?.documents?.firstOrNull()?.id
            ?: byPhone?.documents?.firstOrNull()?.id
            ?: return addContact(contact) is AddContactResult.Success

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
                // Overwrite industry only when the scan captured one, so a re-scan
                // can fill a previously empty industry without wiping an existing
                // value on a blank capture (#17).
                industry = contact.industry.ifBlank { existing.industry },
                email = contact.email,
                phone = contact.phone,
                linkedIn = contact.linkedIn,
                address = contact.address,
                meetingDate = contact.meetingDate ?: existing.meetingDate,
                entryMethod = contact.entryMethod,
                businessCardImage = newCardUrl ?: existing.businessCardImage,
                // Keep prior conversation history; append any note captured now.
                conversationNotes = existing.conversationNotes + contact.conversationNotes,
                // Union existing tags with any captured on this scan (case-insensitive,
                // preserving order) so replacing a duplicate never drops tags (#16).
                tags = (existing.tags + contact.tags)
                    .distinctBy { it.trim().lowercase() },
                updatedAt = Timestamp.now()
                // Preserved unchanged by copy(): contactId, ownerId, createdAt,
                // favourite, media, meetingLocation, eventName.
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