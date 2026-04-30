package com.humblesolutions.humblecontacts.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
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

    val currentUser get() = auth.currentUser
    val isLoggedIn  get() = auth.currentUser != null

    // ── Email Sign-In ──────────────────────────────────────────────────────────

    suspend fun signInWithEmail(email: String, password: String): AuthResult<Unit> =
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Email Register ─────────────────────────────────────────────────────────

    suspend fun registerWithEmail(email: String, password: String): AuthResult<Unit> =
        runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
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
            AuthResult.Success(Unit)
        }.getOrElse { AuthResult.Error(mapError(it)) }

    // ── Phone OTP ──────────────────────────────────────────────────────────────

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): AuthResult<Unit> =
        runCatching {
            auth.signInWithCredential(credential).await()
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
}


