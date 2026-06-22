package com.humblesolutions.humblecontacts.data.auth

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ─── Result wrapper ───────────────────────────────────────────────────────────

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

// ─── Repository ───────────────────────────────────────────────────────────────

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    private val firestore = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser
    val isLoggedIn  get() = auth.currentUser != null

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
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
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

                    "shareSettings" to hashMapOf(
                        "sharePhone" to false,
                        "shareEmail" to false,
                        "shareCompany" to false,
                        "shareLinkedIn" to false
                    ),

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

