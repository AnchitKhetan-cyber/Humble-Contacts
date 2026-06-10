package com.humblesolutions.humblecontacts.ui.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.data.repository.ContactRepository
import kotlinx.coroutines.launch

class NfcViewModel : ViewModel() {

    private val repo = ContactRepository()

    fun saveReceivedContact(
        payload: NfcContactPayload
    ) {
        viewModelScope.launch {

            repo.addContact(
                Contact(
                    fullName = payload.name,
                    email = payload.email,
                    phone = payload.phone,
                    meetingDate = Timestamp.now(),
                    entryMethod = "nfc"
                )
            )
        }
    }
}