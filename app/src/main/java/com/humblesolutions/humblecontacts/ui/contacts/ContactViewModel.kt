// ui/contacts/ContactViewModel.kt
package com.humblesolutions.humblecontacts.ui.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.data.model.ContactNote
import com.humblesolutions.humblecontacts.data.repository.ContactRepository
import com.humblesolutions.humblecontacts.utils.ContactExporter
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import com.humblesolutions.humblecontacts.utils.GeminiError
import okhttp3.Address

class ContactViewModel : ViewModel() {
    private val repo = ContactRepository()

    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isParsingCard by mutableStateOf(false)
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

    fun parseBusinessCard(
        ocrText: String,
        onResult: (ContactInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {

            try {

                val contact = BusinessCardParser.parse(ocrText)

                if (
                    contact.name.isBlank() &&
                    contact.company.isBlank() &&
                    contact.phone.isBlank() &&
                    contact.email.isBlank() &&
                    contact.linkedin.isBlank()
                ) {
                    onError(
                        "No contact details could be extracted. Please try a clearer image."
                    )
                    return@launch
                }

                onResult(contact)

            } catch (e: Exception) {

                onError(
                    GeminiError.getMessage(e)
                )
            }
        }
    }

    fun addContact(
        fullName: String,
        jobRole: String,
        company: String,
        email: String,
        phone: String,
        linkedIn: String,
        address: String,
        notes: String,
        imageUri: Uri?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {

            val contact = Contact(
                fullName = fullName,
                jobRole = jobRole,
                company = company,
                email = email,
                phone = phone,
                linkedIn = linkedIn,
                address = address,
                businessCardImage = imageUri?.toString() ?: "",
                conversationNotes =
                    if (notes.isBlank()) {
                        emptyList()
                    } else {
                        listOf(
                            ContactNote(
                                text = notes.trim(),
                                createdAt = Timestamp.now()
                            )
                        )
                    },
                meetingDate = Timestamp.now(),
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

    fun replaceContact(
        fullName: String,
        jobRole: String,
        company: String,
        email: String,
        phone: String,
        linkedIn: String,
        address: String,
        notes: String,
        imageUri: Uri?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val contact = Contact(
                fullName = fullName,
                jobRole = jobRole,
                company = company,
                email = email,
                phone = phone,
                linkedIn = linkedIn,
                address = address,
                businessCardImage = imageUri?.toString() ?: "",
                conversationNotes = if (notes.isBlank()) emptyList()
                else listOf(ContactNote(text = notes.trim(), createdAt = Timestamp.now())),
                meetingDate = Timestamp.now(),
                entryMethod = "business_card"
            )
            repo.replaceContact(contact)
            onDone()
        }
    }
}