package com.humblesolutions.humblecontacts.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.humblesolutions.humblecontacts.data.model.Contact
import kotlinx.coroutines.tasks.await

class ContactRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val uid get() = auth.currentUser?.uid ?: ""

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

                val contacts =
                    snapshot?.toObjects(Contact::class.java)
                        ?.sortedByDescending { it.meetingDate }
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

        ref.set(
            contact.copy(
                contactId = ref.id,
                ownerId = uid,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        ).await()

        return true
    }

    suspend fun deleteContact(contactId: String) {
        db.collection("contacts").document(contactId).delete().await()
    }

    suspend fun updateContact(contact: Contact) {
        db.collection("contacts").document(contact.contactId)
            .set(contact.copy(updatedAt = com.google.firebase.Timestamp.now()))
            .await()
    }
}