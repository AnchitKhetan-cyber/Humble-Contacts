package com.humblesolutions.humblecontacts.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.humblesolutions.humblecontacts.data.model.Contact
import java.util.Calendar

class HomeViewModel : ViewModel() {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth
    private val uid  get() = auth.currentUser?.uid ?: ""

    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set

    // Derived stats
    val totalContacts: Int get() = contacts.size
    val thisMonthCount: Int get() {
        val cal = Calendar.getInstance()
        val year  = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        return contacts.count { contact ->
            contact.createdAt?.toDate()?.let {
                val c = Calendar.getInstance().apply { time = it }
                c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
            } ?: false
        }
    }
    val uniqueEventsCount: Int get() =
        contacts.map { it.eventName }.filter { it.isNotBlank() }.toSet().size

    // 5 most recently added contacts
    val recentContacts: List<Contact> get() =
        contacts.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(5)

    init {
        db.collection("contacts")
            .whereEqualTo("ownerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                contacts  = snapshot?.toObjects(Contact::class.java) ?: emptyList()
                isLoading = false
            }
    }
}