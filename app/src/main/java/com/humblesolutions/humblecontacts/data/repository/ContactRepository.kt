package com.humblesolutions.humblecontacts.data.repository

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
        db.collection("contacts")
            .whereEqualTo("ownerId", uid)
            .orderBy("meetingDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val contacts = snapshot?.toObjects(Contact::class.java) ?: emptyList()
                onResult(contacts)
            }
    }

    suspend fun addContact(contact: Contact): String {
        val ref = db.collection("contacts").document()
        ref.set(contact.copy(
            contactId = ref.id,
            ownerId = uid,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )).await()
        return ref.id
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