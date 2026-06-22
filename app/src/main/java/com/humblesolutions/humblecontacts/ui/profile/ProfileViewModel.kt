package com.humblesolutions.humblecontacts.ui.profile

import android.util.Log
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
                Log.d("DELETE_ACCOUNT", "Deleting contacts...")
                contactRepository.deleteAllContacts()

                Log.d("DELETE_ACCOUNT", "Deleting user document...")
                contactRepository.deleteUserDocument()

                Log.d("DELETE_ACCOUNT", "Deleting auth account...")
                authRepository.deleteCurrentUser()

                Log.d("DELETE_ACCOUNT", "Finished")

                onSuccess()

            } catch (e: Exception) {
                Log.e("DELETE_ACCOUNT", "Delete failed", e)

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