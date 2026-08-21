package com.humblesolutions.humblecontacts.data.auth

import android.app.Activity
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

// ─── Result wrapper ───────────────────────────────────────────────────────────

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

// ─── Repository ────────────────────────────────────────────────────────────

class AuthRepository {

    companion object {
        /** App package, used for the email-link ActionCodeSettings. */
        private const val ANDROID_PACKAGE_NAME = "com.humblesolutions.humblecontacts"

        /**
         * Continue URL for the deletion confirmation email link. MUST be an authorized
         * domain in the Firebase console that hosts Android App Links (assetlinks.json)
         * and is registered in the manifest intent-filter below. Replace this
         * placeholder with your real hosted domain before the email link can work.
         */
        private const val EMAIL_LINK_CONTINUE_URL =
            "https://humblecontacts.example.com/finishDelete"
    }

    private val auth = FirebaseAuth.getInstance()

    private val firestore = FirebaseFirestore.getInstance()

    private val functions = FirebaseFunctions.getInstance()

    val currentUser get() = auth.currentUser
    val isLoggedIn  get() = auth.currentUser != null

    val currentEmail       get() = auth.currentUser?.email
    val currentPhoneNumber get() = auth.currentUser?.phoneNumber

    // ── Re-authentication (required before destructive account ops) ──────────────

    /**
     * Which sign-in provider the current user actually used, normalised to
     * "email" / "google" / "phone" (same mapping as [syncUserDocument]).
     * Returns null if there is no signed-in user or no recognised provider.
     */
    fun currentProviderId(): String? =
        auth.currentUser?.providerData
            ?.mapNotNull { provider ->
                when (provider.providerId) {
                    "password"   -> "email"
                    "google.com" -> "google"
                    "phone"      -> "phone"
                    else         -> null
                }
            }
            ?.firstOrNull()

    fun buildEmailCredential(password: String): AuthCredential? {
        val email = auth.currentUser?.email ?: return null
        return EmailAuthProvider.getCredential(email, password)
    }

    fun buildGoogleCredential(idToken: String): AuthCredential =
        GoogleAuthProvider.getCredential(idToken, null)

    fun buildPhoneCredential(verificationId: String, otp: String): AuthCredential =
        PhoneAuthProvider.getCredential(verificationId, otp)

    /**
     * Sends an OTP to the current user's phone number so we can re-authenticate
     * them before deletion. Verification callbacks are driven by the caller (UI).
     */
    fun sendReauthOtp(
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): Boolean {
        val phone = auth.currentUser?.phoneNumber ?: return false
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        return true
    }

    /**
     * Re-authenticates the current user with a fresh credential. This MUST succeed
     * before any account/data deletion so that [deleteCurrentUser] cannot fail with
     * [FirebaseAuthRecentLoginRequiredException] after data has already been wiped.
     *
     * A successful call refreshes the login recency on the [currentUser] itself, so
     * the subsequent [deleteCurrentUser] does not need the credential passed to it.
     */
    suspend fun reauthenticate(credential: AuthCredential): AuthResult<Unit> =
        runCatching {
            val user = auth.currentUser ?: throw Exception("No signed-in user to re-authenticate")
            user.reauthenticate(credential).await()
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Email confirmation link (deletion gate) ──────────────────────────────────
    //
    // NOTE (setup required to function at runtime): the email link relies on
    //   1. "Email link (passwordless sign-in)" enabled for the Email/Password
    //      provider in the Firebase console,
    //   2. an authorized domain you host that serves an Android App Links
    //      assetlinks.json (Firebase Dynamic Links / *.page.link was shut down in
    //      2025 and cannot be used), and
    //   3. [EMAIL_LINK_CONTINUE_URL] pointing at that domain.
    // Until that is configured, sending/handling the link will not work end-to-end.

    /** Sends a one-time confirmation link to [email] to gate account deletion. */
    suspend fun sendReauthEmailLink(email: String): AuthResult<Unit> =
        runCatching {
            val settings = ActionCodeSettings.newBuilder()
                .setUrl(EMAIL_LINK_CONTINUE_URL)
                .setHandleCodeInApp(true)
                .setAndroidPackageName(ANDROID_PACKAGE_NAME, true, null)
                .build()
            auth.sendSignInLinkToEmail(email, settings).await()
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    /** Whether [link] is a Firebase email sign-in/confirmation link. */
    fun isReauthEmailLink(link: String): Boolean = auth.isSignInWithEmailLink(link)

    /**
     * Completes the email side of the deletion gate.
     *
     * For accounts with the Email/Password provider linked, the email link is a valid
     * re-auth credential, so this re-authenticates and refreshes login recency. For
     * accounts where email/password is NOT a linked provider (e.g. Google-only), the
     * link cannot re-authenticate; callers should treat a `true` [isReauthEmailLink]
     * as confirmation only and obtain the delete credential elsewhere (silent Google).
     */
    suspend fun reauthenticateWithEmailLink(email: String, link: String): AuthResult<Unit> =
        runCatching {
            val user = auth.currentUser ?: throw Exception("No signed-in user to re-authenticate")
            val credential = EmailAuthProvider.getCredentialWithLink(email, link)
            user.reauthenticate(credential).await()
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Email Sign-In ──────────────────────────────────────────────────────────

    suspend fun signInWithEmail(email: String, password: String): AuthResult<Unit> =
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await()

            syncUserDocument()

            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Email Register ─────────────────────────────────────────────────────────

    suspend fun registerWithEmail(email: String, password: String): AuthResult<Unit> =
        runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()

            syncUserDocument()

            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Update display name ────────────────────────────────────────────────────

    suspend fun updateDisplayName(name: String): AuthResult<Unit> =
        runCatching {
            val request = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            auth.currentUser?.updateProfile(request)?.await()
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Google Sign-In ─────────────────────────────────────────────────────────

    suspend fun signInWithGoogle(idToken: String): AuthResult<Unit> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()

            syncUserDocument()

            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Phone OTP ──────────────────────────────────────────────────────────────

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): AuthResult<Unit> =
        runCatching {
            auth.signInWithCredential(credential).await()

            syncUserDocument()

            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    suspend fun verifyOtp(verificationId: String, otp: String): AuthResult<Unit> =
        runCatching {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            auth.signInWithCredential(credential).await()
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Password Reset ─────────────────────────────────────────────────────────

    suspend fun sendPasswordReset(email: String): AuthResult<Unit> =
        runCatching {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Sign Out ───────────────────────────────────────────────────────────────

    fun signOut() = auth.signOut()

    // ── Error mapping ──────────────────────────────────────────────────────────

    private fun mapError(e: Throwable): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
        is FirebaseAuthUserCollisionException      -> "An account with this email already exists"
        is FirebaseAuthWeakPasswordException       -> "Password is too weak — use 8+ characters"
        is FirebaseFunctionsException              -> mapFunctionsError(e)
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }

    /**
     * Turns a Cloud Function failure into something a user can act on. The generic
     * INTERNAL code (an unhandled server error, e.g. the confirmation email couldn't be
     * sent) surfaces as the raw string "internal" otherwise; give it a real message.
     */
    private fun mapFunctionsError(e: FirebaseFunctionsException): String = when (e.code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED ->
            "Please sign in again and retry."
        FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
            "No email address is associated with this account."
        FirebaseFunctionsException.Code.NOT_FOUND ->
            "Account deletion is temporarily unavailable. Please try again later."
        else ->
            "We couldn't send the confirmation email. Please try again later."
    }

    // ── Cloud Function deletion verification (Google sign-in) ────────────────────

    /**
     * Asks the `requestAccountDeletion` Cloud Function to email a one-time confirmation
     * link to the account email. The function stores the token at `account_deletions/{uid}`.
     */
    suspend fun requestAccountDeletionEmail(): AuthResult<Unit> =
        runCatching {
            functions.getHttpsCallable("requestAccountDeletion").call().await()
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    /**
     * Listens for the deletion request to be confirmed (the user tapped the emailed link,
     * which flipped `confirmed: true`). Returns the registration so the caller can detach it.
     */
    fun observeDeletionConfirmed(
        onConfirmed: () -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("account_deletions").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Could not verify confirmation.")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists() &&
                    snapshot.getBoolean("confirmed") == true
                ) {
                    onConfirmed()
                }
            }
    }

    /** Best-effort removal of the deletion-request doc (while still authenticated). */
    suspend fun deleteDeletionRequest(uid: String) {
        runCatching {
            firestore.collection("account_deletions").document(uid).delete().await()
        }
    }

    suspend fun deleteCurrentUser() {
        val user = auth.currentUser ?: throw Exception("Current user is null")

        Log.d("AUTH_DELETE", "UID = ${user.uid}")
        Log.d("AUTH_DELETE", "Email = ${user.email}")

        try {
            user.delete().await()
            Log.d("AUTH_DELETE", "User deleted")
        } catch (e: Exception) {
            Log.e("AUTH_DELETE", "Delete failed", e)
            throw e
        }
    }

    private suspend fun syncUserDocument() {

        val user = auth.currentUser ?: return

        val providerIds = user.providerData
            .mapNotNull { provider ->
                when (provider.providerId) {
                    "password" -> "email"
                    "google.com" -> "google"
                    "phone" -> "phone"
                    else -> null
                }
            }
            .distinct()

        val userRef = firestore.collection("users").document(user.uid)

        val snapshot = userRef.get().await()

        if (!snapshot.exists()) {

            userRef.set(
                hashMapOf(
                    "userId" to user.uid,
                    "name" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "phone" to (user.phoneNumber ?: ""),
                    "company" to "",
                    "profession" to "",
                    "profilePhotoUrl" to (user.photoUrl?.toString() ?: ""),
                    "linkedInUrl" to "",
                    "isProfileCompleted" to false,

                    "authProviders" to providerIds,

                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now(),

                    // Opt-out default: new users share everything and turn off
                    // what they want private (#20). Marked initialised so the
                    // migration in getCurrentUserProfile() skips them.
                    "shareSettings" to hashMapOf(
                        "sharePhone" to true,
                        "shareEmail" to true,
                        "shareCompany" to true,
                        "shareLinkedIn" to true
                    ),
                    "shareSettingsInitialized" to true,

                    "stats" to hashMapOf(
                        "totalContacts" to 0,
                        "totalEvents" to 0,
                        "totalReminders" to 0
                    ),

                    "visitingCard" to hashMapOf(
                        "headline" to "",
                        "bio" to "",
                        "websiteUrl" to "",
                        "portfolioUrl" to "",
                        "cardTheme" to ""
                    )
                )
            ).await()

        } else {

            userRef.update(
                mapOf(
                    "name" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "phone" to (user.phoneNumber ?: ""),
                    "profilePhotoUrl" to (user.photoUrl?.toString() ?: ""),
                    "authProviders" to providerIds,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
        }
    }
}

