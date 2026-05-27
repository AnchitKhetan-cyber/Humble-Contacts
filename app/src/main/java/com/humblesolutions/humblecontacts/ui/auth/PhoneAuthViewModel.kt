package com.humblesolutions.humblecontacts.ui.auth

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PhoneAuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    var state by mutableStateOf<AuthState>(AuthState.Idle)
        private set

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Kept so the UI can re-trigger with the same number
    private var lastActivity: Activity? = null
    private var lastPhoneNumber: String? = null

    fun sendOtp(phoneNumber: String, activity: Activity) {
        lastActivity    = activity
        lastPhoneNumber = phoneNumber
        state           = AuthState.Loading
        requestOtp(phoneNumber, activity, token = null)
    }

    fun resendOtp() {
        val activity = lastActivity    ?: return
        val phone    = lastPhoneNumber ?: return
        state        = AuthState.Loading
        requestOtp(phone, activity, token = resendToken)
    }

    /** Clears a visible error so the UI can reset the banner. */
    fun clearError() {
        if (state is AuthState.Error) state = AuthState.Idle
    }

    private fun requestOtp(
        phoneNumber: String,
        activity: Activity,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval / instant verify (some Google-Play-certified devices)
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                state = AuthState.Error(e.message ?: "Verification failed. Please try again.")
            }

            override fun onCodeSent(
                verificationId: String,
                newToken: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken          = newToken
                state                = AuthState.CodeSent
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        // Pass the resend token when available so Firebase doesn't re-send a CAPTCHA
        if (token != null) builder.setForceResendingToken(token)

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun verifyOtp(otpCode: String) {
        val verificationId = storedVerificationId ?: return
        val credential     = PhoneAuthProvider.getCredential(verificationId, otpCode)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        state = AuthState.Loading
        auth.signInWithCredential(credential)
            .addOnSuccessListener { state = AuthState.Success }
            .addOnFailureListener { state = AuthState.Error(it.message ?: "Incorrect OTP. Please try again.") }
    }
}

sealed class AuthState {
    object Idle     : AuthState()
    object Loading  : AuthState()
    object CodeSent : AuthState()
    object Success  : AuthState()
    data class Error(val message: String) : AuthState()
}