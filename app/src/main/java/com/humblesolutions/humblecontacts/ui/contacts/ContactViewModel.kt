// ui/contacts/ContactViewModel.kt
package com.humblesolutions.humblecontacts.ui.contacts

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.data.repository.ContactRepository
import com.humblesolutions.humblecontacts.utils.ContactExporter
import kotlinx.coroutines.launch

class ContactViewModel : ViewModel() {
    private val repo = ContactRepository()

    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        repo.getContactsRealtime { result ->
            contacts = result
            isLoading = false
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch { repo.deleteContact(contact.contactId) }
    }

    fun refreshContacts() {
        isLoading = true

        repo.getContactsRealtime { result ->
            contacts = result
            isLoading = false
        }
    }

    fun addContact(
        fullName: String,
        jobRole: String,
        company: String,
        email: String,
        phone: String,
        notes: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {

            val contact = Contact(
                fullName = fullName,
                jobRole = jobRole,
                company = company,
                email = email,
                phone = phone,
                conversationNotes = notes,
                meetingDate = com.google.firebase.Timestamp.now(),
                entryMethod = "business_card"
            )

            val added = repo.addContact(contact)

            onResult(added)
        }
    }

    fun filtered(
        searchQuery: String,
        selectedFilter: String
    ): List<Contact> {

        return contacts.filter { contact ->

            val matchesSearch =
                searchQuery.isBlank() ||
                        contact.fullName.contains(searchQuery, ignoreCase = true) ||
                        contact.company.contains(searchQuery, ignoreCase = true) ||
                        contact.jobRole.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "All" -> true
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }
}