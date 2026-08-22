// ui/contacts/ContactViewModel.kt
package com.humblesolutions.humblecontacts.ui.contacts

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.data.model.ContactNote
import com.humblesolutions.humblecontacts.data.repository.ContactRepository
import com.humblesolutions.humblecontacts.data.repository.AddContactResult
import com.humblesolutions.humblecontacts.notifications.NotificationHelper
import com.humblesolutions.humblecontacts.utils.ContactExporter
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import com.humblesolutions.humblecontacts.utils.GeminiError

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ContactRepository()

    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isParsingCard by mutableStateOf(false)
        private set

    // Cursor pagination (ticket #25): a single realtime listener over the newest
    // [currentLimit] contacts. Scrolling near the end grows the limit and
    // re-registers the listener, so large lists never load entirely into memory.
    var isLoadingMore by mutableStateOf(false)
        private set

    // Whether the last page came back full (so more may exist). Once a snapshot
    // returns fewer than the limit, we've reached the end.
    var hasMore by mutableStateOf(true)
        private set

    private val pageSize = 50L
    private var currentLimit = pageSize

    // Holds the single active Firestore listener. Detached before re-registering
    // and in onCleared() so listeners never stack (ticket #9).
    private var contactsListener: ListenerRegistration? = null

    init {
        registerContactsListener()
    }

    private fun registerContactsListener() {
        // Detach any existing listener before attaching a new one, so exactly one
        // is ever active regardless of how many times this is called.
        contactsListener?.remove()
        contactsListener = repo.getContactsRealtime(currentLimit) { result ->
            contacts = result
            // A full page means there may be more to load; a short page is the end.
            hasMore = result.size.toLong() >= currentLimit
            isLoading = false
            isLoadingMore = false
        }
    }

    /**
     * Loads the next page by growing the listener's limit and re-registering it
     * (keeping exactly one listener, per #9). No-op while a load is in flight or
     * once the end has been reached.
     */
    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        currentLimit += pageSize
        registerContactsListener()
    }

    fun toggleFavourite(contact: Contact) {
        viewModelScope.launch {
            repo.toggleFavourite(contact.contactId, !contact.favourite)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repo.deleteContact(contact.contactId)
            NotificationHelper.notifyAction(
                getApplication(),
                "Contact Deleted",
                "${contact.fullName} has been removed from your contacts."
            )
        }
    }

    fun refreshContacts() {
        isLoading = true
        // Reset to the first page on an explicit refresh.
        currentLimit = pageSize
        hasMore = true
        registerContactsListener()
    }

    override fun onCleared() {
        super.onCleared()
        contactsListener?.remove()
        contactsListener = null
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
                    onError("No contact details could be extracted. Please try a clearer image.")
                    return@launch
                }
                onResult(contact)
            } catch (e: Exception) {
                onError(GeminiError.getMessage(e))
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
        tags: List<String> = emptyList(),
        industry: String = "",
        // A visiting card reconstructed from a scanned QR (#64) — stored in the
        // new contact's Media once it's created.
        cardMediaUri: Uri? = null,
        onResult: (AddContactResult) -> Unit
    ) {
        viewModelScope.launch {
            val contact = Contact(
                fullName = fullName,
                jobRole = jobRole,
                company = company,
                industry = industry,
                email = email,
                phone = phone,
                linkedIn = linkedIn,
                address = address,
                businessCardImage = imageUri?.toString() ?: "",
                tags = tags,
                conversationNotes =
                    if (notes.isBlank()) emptyList()
                    else listOf(ContactNote(text = notes.trim(), createdAt = Timestamp.now())),
                meetingDate = Timestamp.now(),
                entryMethod = "business_card"
            )

            val result = repo.addContact(contact)

            if (result is AddContactResult.Success) {
                NotificationHelper.notifyAction(
                    getApplication(),
                    "Contact Added!",
                    "$fullName has been added to your contacts."
                )

                // Store the scanned visiting card in the new contact's Media
                // (#64). Best-effort and in the background so it never blocks the
                // save; the ViewModel scope outlives the screen.
                if (cardMediaUri != null) {
                    launch { repo.addMediaImage(result.contactId, cardMediaUri) }
                }
            }

            onResult(result)
        }
    }

    /**
     * Updates an existing contact from the edit form. Starts from [original] so
     * everything the form does not edit — contactId, ownerId, createdAt,
     * favourite, media, businessCardImage, meetingDate/location, eventName — is
     * preserved. A non-blank [newNote] is appended to the existing conversation
     * notes rather than replacing them.
     */
    fun updateContact(
        original: Contact,
        fullName: String,
        jobRole: String,
        company: String,
        industry: String,
        email: String,
        phone: String,
        linkedIn: String,
        address: String,
        newNote: String,
        tags: List<String>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val updated = original.copy(
                fullName = fullName,
                jobRole = jobRole,
                company = company,
                industry = industry,
                email = email,
                phone = phone,
                linkedIn = linkedIn,
                address = address,
                tags = tags,
                conversationNotes =
                    if (newNote.isBlank()) original.conversationNotes
                    else original.conversationNotes +
                        ContactNote(text = newNote.trim(), createdAt = Timestamp.now())
            )
            repo.updateContact(updated)
            onDone()
        }
    }

    fun filtered(
        searchQuery: String,
        selectedFilter: String,
        selectedValue: String? = null
    ): List<Contact> {
        return contacts.filter { contact ->
            // Search across the fields people actually recall someone by, incl.
            // conversation notes (ticket #29). See [contactMatchesQuery] for the
            // predicate and the pagination/indexed-search follow-up note.
            val matchesSearch = contactMatchesQuery(contact, searchQuery)

            // "By …" chips filter to a value the user picked from that chip's
            // dropdown. With no value chosen yet the dimension matches nothing,
            // so the list stays empty until a value is selected.
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Favourites" -> contact.favourite
                "By Industry" -> selectedValue != null && contact.industry == selectedValue
                "By Event" -> selectedValue != null && contact.eventName == selectedValue
                "By Date" -> selectedValue != null && contact.metOn == selectedValue
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    /** Distinct non-blank industries across the user's contacts, sorted. */
    fun industries(): List<String> =
        contacts.map { it.industry }.filter { it.isNotBlank() }.distinct().sorted()

    /** Distinct non-blank event names across the user's contacts, sorted. */
    fun events(): List<String> =
        contacts.map { it.eventName }.filter { it.isNotBlank() }.distinct().sorted()

    /** Distinct meeting dates (formatted via [Contact.metOn]), newest first. */
    fun meetingDates(): List<String> =
        contacts.filter { it.meetingDate != null }
            .sortedByDescending { it.meetingDate?.seconds ?: 0L }
            .map { it.metOn }
            .filter { it.isNotBlank() }
            .distinct()

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
        tags: List<String> = emptyList(),
        industry: String = "",
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val contact = Contact(
                fullName = fullName,
                jobRole = jobRole,
                company = company,
                industry = industry,
                email = email,
                phone = phone,
                linkedIn = linkedIn,
                address = address,
                businessCardImage = imageUri?.toString() ?: "",
                tags = tags,
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
