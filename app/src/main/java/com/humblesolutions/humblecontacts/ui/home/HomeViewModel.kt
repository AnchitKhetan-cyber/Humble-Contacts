package com.humblesolutions.humblecontacts.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.utils.ContactExporter
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
    val recentContacts: List<Contact>
        get() = contacts.take(5)

    val followUps: List<Contact>
        get() = contacts
            .filter {
                it.conversationNotes.isNotBlank()
            }
            .take(5)

    fun exportContacts(
        context: Context
    ) {

        val success = ContactExporter.exportToCsv(
            context,
            contacts
        )

        Toast.makeText(
            context,
            if (success)
                "${contacts.size} contacts exported to Downloads"
            else
                "Export failed",
            Toast.LENGTH_LONG
        ).show()
    }

    init {

        db.collection("contacts")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                contacts = snapshot
                    ?.toObjects(Contact::class.java)
                    ?.sortedByDescending {
                        it.createdAt?.seconds ?: 0L
                    }
                    ?: emptyList()

                isLoading = false
            }
    }

}