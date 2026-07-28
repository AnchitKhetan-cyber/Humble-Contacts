package com.humblesolutions.humblecontacts.ui.profile

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.humblesolutions.humblecontacts.data.auth.AuthRepository
import com.humblesolutions.humblecontacts.data.auth.AuthResult
import com.humblesolutions.humblecontacts.data.repository.ContactRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val contactRepository = ContactRepository()
    private val authRepository = AuthRepository()

    // ── Passthroughs the delete UI needs to gather a re-auth credential ──────────

    fun currentProviderId(): String? = authRepository.currentProviderId()

    fun buildEmailCredential(password: String): AuthCredential? =
        authRepository.buildEmailCredential(password)

    fun buildGoogleCredential(idToken: String): AuthCredential =
        authRepository.buildGoogleCredential(idToken)

    fun buildPhoneCredential(verificationId: String, otp: String): AuthCredential =
        authRepository.buildPhoneCredential(verificationId, otp)

    fun sendReauthOtp(
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): Boolean = authRepository.sendReauthOtp(activity, callbacks)

    /**
     * Deletes the account, but only after a fresh re-authentication succeeds.
     *
     * Flow:
     *  1. Re-authenticate with [credential]. If this fails/cancels, NOTHING is
     *     deleted and [onError] is called — the user stays signed in with data intact.
     *  2. Capture the uid (before Auth deletion nulls out `currentUser`).
     *  3. Delete the Auth account FIRST. If that throws, [onError] and stop — no data
     *     is ever deleted while the Auth account still exists.
     *  4. Once the account is gone, best-effort clean up the Firestore user doc,
     *     contacts, and Storage business-card images. Cleanup failures are logged but
     *     do not turn a real account deletion into a reported failure.
     */
    fun deleteAccount(
        credential: AuthCredential,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // 1. Re-authenticate — gate everything behind this.
            when (val result = authRepository.reauthenticate(credential)) {
                is AuthResult.Error -> {
                    Log.e("DELETE_ACCOUNT", "Re-auth failed: ${result.message}")
                    onError(result.message)
                    return@launch
                }
                else -> Unit
            }

            // 2. Snapshot the uid before the Auth account disappears.
            val uid = authRepository.currentUser?.uid
            if (uid == null) {
                onError("Something went wrong. Please try again.")
                return@launch
            }

            // 3. Delete the Auth account first.
            try {
                Log.d("DELETE_ACCOUNT", "Deleting auth account...")
                authRepository.deleteCurrentUser()
            } catch (e: Exception) {
                Log.e("DELETE_ACCOUNT", "Auth delete failed — no data deleted", e)
                onError(e.message ?: "Something went wrong. Please try again.")
                return@launch
            }

            // 4. Account is gone — best-effort data cleanup.
            runCatching {
                Log.d("DELETE_ACCOUNT", "Deleting user document...")
                contactRepository.deleteUserDocument(uid)
            }.onFailure { Log.e("DELETE_ACCOUNT", "User doc cleanup failed", it) }

            runCatching {
                Log.d("DELETE_ACCOUNT", "Deleting contacts...")
                contactRepository.deleteAllContacts(uid)
            }.onFailure { Log.e("DELETE_ACCOUNT", "Contacts cleanup failed", it) }

            runCatching {
                Log.d("DELETE_ACCOUNT", "Deleting business-card images...")
                contactRepository.deleteBusinessCardImages(uid)
            }.onFailure { Log.e("DELETE_ACCOUNT", "Storage cleanup failed", it) }

            Log.d("DELETE_ACCOUNT", "Finished")
            onSuccess()
        }
    }
}
