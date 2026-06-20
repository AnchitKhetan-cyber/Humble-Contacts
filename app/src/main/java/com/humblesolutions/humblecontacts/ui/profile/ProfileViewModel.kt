package com.humblesolutions.humblecontacts.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.humblesolutions.humblecontacts.data.auth.AuthRepository
import com.humblesolutions.humblecontacts.data.repository.ContactRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val contactRepository = ContactRepository()
    private val authRepository = AuthRepository()

    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                // Delete all contacts
                contactRepository.deleteAllContacts()

                // Delete user document
                contactRepository.deleteUserDocument()

                // Delete Firebase Authentication account
                authRepository.deleteCurrentUser()

                onSuccess()

            } catch (e: Exception) {
                android.util.Log.e("DELETE_ACCOUNT", "Delete failed", e)

                when (e) {
                    is FirebaseAuthRecentLoginRequiredException ->
                        onError("REAUTH_REQUIRED")

                    else ->
                        onError(e.message ?: "Something went wrong")
                }
            }
        }
    }
}