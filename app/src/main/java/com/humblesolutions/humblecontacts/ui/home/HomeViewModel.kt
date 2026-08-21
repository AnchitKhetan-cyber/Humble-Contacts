package com.humblesolutions.humblecontacts.ui.home

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.data.repository.AddContactResult
import com.humblesolutions.humblecontacts.data.repository.ContactRepository
import com.humblesolutions.humblecontacts.utils.ContactExporter
import com.humblesolutions.humblecontacts.utils.ContactImporter
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val uid get() = auth.currentUser?.uid ?: ""

    private val repo = ContactRepository()

    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set


    val totalContacts: Int
        get() = contacts.size

    val thisMonthCount: Int
        get() {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)

            return contacts.count { contact ->
                contact.createdAt?.toDate()?.let {
                    val c = Calendar.getInstance().apply { time = it }
                    c.get(Calendar.YEAR) == year &&
                            c.get(Calendar.MONTH) == month
                } ?: false
            }
        }

    val uniqueEventsCount: Int
        get() = contacts
            .map { it.eventName }
            .filter { it.isNotBlank() }
            .toSet()
            .size

    val recentContacts: List<Contact>
        get() = contacts.take(5)

    @RequiresApi(Build.VERSION_CODES.Q)
    fun exportContacts(context: Context) {
        val success = ContactExporter.exportToCsv(context, contacts)

        Toast.makeText(
            context,
            if (success)
                "${contacts.size} contacts exported to Downloads"
            else
                "Export failed",
            Toast.LENGTH_LONG
        ).show()
    }

    var isImporting by mutableStateOf(false)
        private set

    /**
     * Imports contacts from a picked CSV [uri]: reads the file, parses it
     * ([ContactImporter], header-name mapped), then inserts each contact via the
     * repository — which skips duplicates by email/phone. Reports how many were
     * imported vs skipped; the realtime listener refreshes the list.
     */
    fun importContacts(context: Context, uri: Uri) {
        if (isImporting) return
        isImporting = true
        viewModelScope.launch {
            val text = try {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            } catch (e: Exception) {
                android.util.Log.e("HOME_DEBUG", "Failed to read CSV", e)
                null
            }

            if (text.isNullOrBlank()) {
                isImporting = false
                Toast.makeText(context, "Couldn't read that file", Toast.LENGTH_LONG).show()
                return@launch
            }

            val result = ContactImporter.parse(text)

            var imported = 0
            var duplicates = 0
            for (contact in result.contacts) {
                // addContact now returns a sealed result (#27): count a Success as
                // imported and anything else (duplicate or error) as skipped.
                when (repo.addContact(contact)) {
                    is AddContactResult.Success -> imported++
                    else -> duplicates++
                }
            }

            val skipped = duplicates + result.skippedRows
            isImporting = false
            Toast.makeText(
                context,
                if (imported == 0 && skipped == 0)
                    "No contacts found in that file"
                else
                    "Imported $imported contact${if (imported == 1) "" else "s"}" +
                        if (skipped > 0) " ($skipped skipped)" else "",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    init {
        db.collection("contacts")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                contacts = snapshot?.documents
                    ?.mapNotNull { doc ->
                        try {
                            doc.toObject(Contact::class.java)
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "HOME_DEBUG",
                                "Skipping invalid contact ${doc.id}",
                                e
                            )
                            null
                        }
                    }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()

                isLoading = false
            }
    }
}