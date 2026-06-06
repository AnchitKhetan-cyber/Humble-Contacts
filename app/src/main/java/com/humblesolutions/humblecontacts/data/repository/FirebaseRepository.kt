package com.humblesolutions.humblecontacts.data.repository


import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

data class Contact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = ""
)

object FirebaseRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private fun contactsCollection() =
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("contacts")

    fun addContact(contact: Contact, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val ref = contactsCollection().document()
        ref.set(contact.copy(id = ref.id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getContacts(onResult: (List<Contact>) -> Unit, onFailure: (Exception) -> Unit) {
        contactsCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onFailure(error); return@addSnapshotListener }
                val contacts = snapshot?.documents?.mapNotNull {
                    it.toObject(Contact::class.java)
                } ?: emptyList()
                onResult(contacts)
            }
    }

    fun deleteContact(contactId: String, onSuccess: () -> Unit) {
        contactsCollection().document(contactId).delete()
            .addOnSuccessListener { onSuccess() }
    }

    fun updateContact(contact: Contact, onSuccess: () -> Unit) {
        contactsCollection().document(contact.id).set(contact)
            .addOnSuccessListener { onSuccess() }
    }
}