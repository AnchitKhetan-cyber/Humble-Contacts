package com.humblesolutions.humblecontacts.ui.profile

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.ListenerRegistration
import com.humblesolutions.humblecontacts.data.auth.AuthRepository
import com.humblesolutions.humblecontacts.data.auth.AuthResult
import com.humblesolutions.humblecontacts.data.auth.GoogleSignInHelper
import com.humblesolutions.humblecontacts.data.auth.PendingDeletionStore
import com.humblesolutions.humblecontacts.data.repository.ContactRepository
import kotlinx.coroutines.launch

/** A verification channel the deletion gate can require. */
enum class DeleteChannel { EMAIL, PHONE }

class ProfileViewModel : ViewModel() {

    private val contactRepository = ContactRepository()
    private val authRepository = AuthRepository()

    val currentEmail: String? get() = authRepository.currentEmail?.takeIf { it.isNotBlank() }
    val currentPhoneNumber: String? get() = authRepository.currentPhoneNumber?.takeIf { it.isNotBlank() }

    fun currentProviderId(): String? = authRepository.currentProviderId()

    /**
     * Which channels must be verified before deletion, based on what's on the account.
     * Deletion requires completing ALL of them (e.g. email link AND phone OTP when both
     * are present).
     */
    fun requiredChannels(): Set<DeleteChannel> = buildSet {
        if (currentEmail != null) add(DeleteChannel.EMAIL)
        if (currentPhoneNumber != null) add(DeleteChannel.PHONE)
    }

    // ── Email confirmation link ──────────────────────────────────────────────────

    /** Sends the confirmation link and records that a deletion is in progress. */
    fun sendEmailLink(
        context: Context,
        onSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = currentEmail
        if (email == null) {
            onError("No email on file for this account.")
            return
        }
        viewModelScope.launch {
            when (val result = authRepository.sendReauthEmailLink(email)) {
                is AuthResult.Error -> onError(result.message)
                else -> {
                    val store = PendingDeletionStore(context)
                    store.inProgress = true
                    store.email = email
                    onSent()
                }
            }
        }
    }

    /**
     * Completes the email side when the confirmation link returns via deep link.
     * For Email/Password accounts the link re-authenticates (refreshing recency); for
     * Google-only accounts it counts as confirmation only (delete credential comes from
     * the silent Google re-auth in [finalizeDeletion]).
     */
    fun completeEmailLink(
        context: Context,
        link: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val store = PendingDeletionStore(context)
        val email = store.email ?: currentEmail
        if (email == null || !authRepository.isReauthEmailLink(link)) {
            onError("This confirmation link is invalid or has expired.")
            return
        }
        viewModelScope.launch {
            if (currentProviderId() == "email") {
                when (val result = authRepository.reauthenticateWithEmailLink(email, link)) {
                    is AuthResult.Error -> {
                        onError(result.message)
                        return@launch
                    }
                    else -> Unit
                }
            }
            // For non-email providers the valid link is confirmation enough.
            store.emailVerified = true
            onDone()
        }
    }

    /**
     * Marks the email channel satisfied without a link — used when the confirmation
     * link can't be sent (e.g. email-link sign-in is disabled for the project) and the
     * account's identity is proven another way (silent Google re-auth, or phone OTP).
     */
    fun skipEmailChannel(context: Context) {
        PendingDeletionStore(context).emailVerified = true
    }

    // ── Cloud Function deletion verification (Google sign-in) ────────────────────

    /** Requests the confirmation email (via Cloud Function) and marks deletion in progress. */
    fun requestDeletionEmail(
        context: Context,
        onSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = authRepository.requestAccountDeletionEmail()) {
                is AuthResult.Error -> onError(result.message)
                else -> {
                    PendingDeletionStore(context).inProgress = true
                    onSent()
                }
            }
        }
    }

    /**
     * Listens for the emailed confirmation to be tapped; on confirmation marks the email
     * channel verified. Returns the registration so the UI can detach it.
     */
    fun observeDeletionConfirmed(
        context: Context,
        onConfirmed: () -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? =
        authRepository.observeDeletionConfirmed(
            onConfirmed = {
                PendingDeletionStore(context).emailVerified = true
                onConfirmed()
            },
            onError = onError
        )

    /**
     * Fallback for Email/Password accounts when the confirmation link can't be sent:
     * re-authenticate with the account password instead, which also refreshes recency.
     */
    fun reauthWithPassword(
        context: Context,
        password: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = authRepository.buildEmailCredential(password)
        if (credential == null) {
            onError("No email on file for this account.")
            return
        }
        viewModelScope.launch {
            when (val result = authRepository.reauthenticate(credential)) {
                is AuthResult.Error -> onError(result.message)
                else -> {
                    PendingDeletionStore(context).emailVerified = true
                    onDone()
                }
            }
        }
    }

    // ── Phone OTP ────────────────────────────────────────────────────────────────

    fun sendPhoneOtp(
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): Boolean = authRepository.sendReauthOtp(activity, callbacks)

    /** Verifies an auto-retrieved phone credential (instant verification, no typed code). */
    fun completePhoneCredential(
        context: Context,
        credential: PhoneAuthCredential,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = authRepository.reauthenticate(credential)) {
                is AuthResult.Error -> onError(result.message)
                else -> {
                    PendingDeletionStore(context).phoneVerified = true
                    onDone()
                }
            }
        }
    }

    /** Verifies the entered OTP by re-authenticating with the phone credential. */
    fun completePhoneOtp(
        context: Context,
        verificationId: String,
        otp: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val credential = authRepository.buildPhoneCredential(verificationId, otp)
            when (val result = authRepository.reauthenticate(credential)) {
                is AuthResult.Error -> onError(result.message)
                else -> {
                    PendingDeletionStore(context).phoneVerified = true
                    onDone()
                }
            }
        }
    }

    // ── Finalize ────────────────────────────────────────────────────────────────

    /**
     * Runs after every required channel is verified. For Google-only accounts it first
     * re-authenticates silently (no picker) to obtain the delete credential; then it
     * deletes the Auth account FIRST, followed by best-effort cleanup of the Firestore
     * user doc, contacts, and Storage business-card images. `onSuccess` fires only once
     * the Auth account is actually gone.
     */
    fun finalizeDeletion(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Google-only accounts: the email link/OTP were confirmation only — get the
            // real delete credential by silently re-authenticating the logged-in account.
            if (currentProviderId() == "google") {
                when (val google = GoogleSignInHelper(context).signInSilently()) {
                    is GoogleSignInHelper.GoogleSignInResult.Success -> {
                        val credential = authRepository.buildGoogleCredential(google.idToken)
                        when (val reauth = authRepository.reauthenticate(credential)) {
                            is AuthResult.Error -> { onError(reauth.message); return@launch }
                            else -> Unit
                        }
                    }
                    is GoogleSignInHelper.GoogleSignInResult.Cancelled -> {
                        onError("Re-authentication was cancelled. Your account was not deleted.")
                        return@launch
                    }
                    is GoogleSignInHelper.GoogleSignInResult.Error -> {
                        onError(google.message)
                        return@launch
                    }
                }
            }

            // Snapshot the uid before the Auth account disappears.
            val uid = authRepository.currentUser?.uid
            if (uid == null) {
                onError("Something went wrong. Please try again.")
                return@launch
            }

            // Remove the deletion-request doc while still authenticated (best-effort).
            authRepository.deleteDeletionRequest(uid)

            // Delete the Auth account first — no data is removed unless this succeeds.
            try {
                Log.d("DELETE_ACCOUNT", "Deleting auth account...")
                authRepository.deleteCurrentUser()
            } catch (e: Exception) {
                Log.e("DELETE_ACCOUNT", "Auth delete failed — no data deleted", e)
                onError(e.message ?: "Something went wrong. Please try again.")
                return@launch
            }

            // Account is gone — best-effort data cleanup.
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

            PendingDeletionStore(context).clear()
            Log.d("DELETE_ACCOUNT", "Finished")
            onSuccess()
        }
    }

    /** Abandons an in-progress deletion (user cancelled), clearing persisted gate state. */
    fun cancelDeletion(context: Context) {
        PendingDeletionStore(context).clear()
    }
}
