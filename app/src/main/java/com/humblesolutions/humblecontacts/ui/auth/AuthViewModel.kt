package com.humblesolutions.humblecontacts.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humblesolutions.humblecontacts.data.auth.AuthRepository
import com.humblesolutions.humblecontacts.data.auth.AuthResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

enum class AuthTab { SIGN_IN, REGISTER }
enum class AuthMethod { EMAIL, PHONE }

data class AuthUiState(
    val tab:             AuthTab     = AuthTab.SIGN_IN,
    val method:          AuthMethod  = AuthMethod.EMAIL,

    // Fields
    val name:            String  = "",
    val email:           String  = "",
    val password:        String  = "",
    val confirmPassword: String  = "",
    val phone:           String  = "",
    val phoneTouched: Boolean = false,
    val otp:             String  = "",

    // OTP flow
    val verificationId:   String?  = null,
    val otpSent:          Boolean  = false,
    val otpResendSeconds: Int      = 0,

    // Inline field validation errors (null = no error shown)
    val nameError:        String?  = null,
    val emailError:       String?  = null,
    val passwordError:    String?  = null,
    val confirmError:     String?  = null,
    val phoneError:       String?  = null,
    val otpError:         String?  = null,

    // Password strength 0-4
    val passwordStrength: Int      = 0,

    // Async state
    val isLoading:        Boolean  = false,
    val errorMessage:     String?  = null,

    // Password visibility toggles
    val passwordVisible:  Boolean  = false,
    val confirmVisible:   Boolean  = false,
)

// ── One-shot events ───────────────────────────────────────────────────────────

// In your AuthEvent sealed class — add one new event:
sealed class AuthEvent {
    object NavigateToHome           : AuthEvent()
    object NavigateToForgotPassword : AuthEvent()
    object LaunchGoogleSignIn       : AuthEvent()   // ← ADD THIS
    data class ShowSnackbar(val message: String) : AuthEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val isLoggedIn get() = repository.isLoggedIn

    // ── Field updates ──────────────────────────────────────────────────────────

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update {
            val confirmError = if (it.confirmPassword.isNotEmpty() && it.confirmPassword != value)
                "Passwords do not match"
            else
                null
            it.copy(
                password         = value,
                passwordError    = null,
                errorMessage     = null,
                passwordStrength = calcStrength(value),
                confirmError     = confirmError
            )
        }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update {
            val confirmError = if (value.isNotEmpty() && value != it.password)
                "Passwords do not match"
            else
                null
            it.copy(confirmPassword = value, confirmError = confirmError)
        }



    fun onPhoneChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                phone = digitsOnly.take(10),
                phoneTouched = true,        // mark as touched on first input
                phoneError = null,
                errorMessage = null
            )
        }
    }

    fun onPhoneFocusLost() {
        val state = _uiState.value
        if (!state.phoneTouched) return     // never interacted, skip validation

        val phoneError = when {
            state.phone.isEmpty() -> "Phone number is required"
            state.phone.length < 10 -> "Phone number must be 10 digits"
            else -> null
        }
        _uiState.update { it.copy(phoneError = phoneError) }
    }

    fun onOtpChange(value: String) =
        _uiState.update { it.copy(otp = value, otpError = null) }

    fun onTabChange(tab: AuthTab) =
        _uiState.update {
            it.copy(
                tab           = tab,
                errorMessage  = null,
                nameError     = null,
                emailError    = null,
                passwordError = null,
                confirmError  = null
            )
        }

    fun onMethodChange(method: AuthMethod) =
        _uiState.update { it.copy(method = method, errorMessage = null, otpSent = false) }

    fun togglePasswordVisibility() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun toggleConfirmVisibility() =
        _uiState.update { it.copy(confirmVisible = !it.confirmVisible) }

    fun dismissError() =
        _uiState.update { it.copy(errorMessage = null) }

    // ── Email submit ──────────────────────────────────────────────────────────

    fun submitEmail() {
        if (!validateEmail() || !validatePassword()) return

        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.signInWithEmail(state.email.trim(), state.password)
            _uiState.update { it.copy(isLoading = false) }
            handleAuthResult(result)
        }
    }

    // ── Google ────────────────────────────────────────────────────────────────

    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.signInWithGoogle(idToken)
            _uiState.update { it.copy(isLoading = false) }
            handleAuthResult(result)
        }
    }

    // ── Phone OTP ─────────────────────────────────────────────────────────────

    fun onVerificationIdReceived(verificationId: String) {
        _uiState.update { it.copy(verificationId = verificationId, otpSent = true, isLoading = false) }
        startResendCountdown()
    }

    fun onAutoVerification(credential: com.google.firebase.auth.PhoneAuthCredential) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.signInWithPhoneCredential(credential)
            _uiState.update { it.copy(isLoading = false) }
            handleAuthResult(result)
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.otp.length < 6) {
            _uiState.update { it.copy(otpError = "Enter the 6-digit OTP") }
            return
        }
        val vid = state.verificationId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, otpError = null) }
            val result = repository.verifyOtp(vid, state.otp.trim())
            _uiState.update { it.copy(isLoading = false) }
            handleAuthResult(result)
        }
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    fun forgotPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Enter your email first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = repository.sendPasswordReset(email)) {
                is AuthResult.Success ->
                    _events.send(AuthEvent.ShowSnackbar("Reset link sent to $email"))
                is AuthResult.Error ->
                    _uiState.update { it.copy(errorMessage = r.message) }
                else -> Unit
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun signOut() = repository.signOut()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun handleAuthResult(result: AuthResult<*>) {
        when (result) {
            is AuthResult.Success -> _events.send(AuthEvent.NavigateToHome)
            is AuthResult.Error   -> _uiState.update { it.copy(errorMessage = result.message) }
            else                  -> Unit
        }
    }

    private fun validateName(): Boolean {
        val name = _uiState.value.name.trim()
        return when {
            name.isBlank() -> {
                _uiState.update { it.copy(nameError = "Full name is required") }; false
            }
            name.split(" ").size < 2 -> {
                _uiState.update { it.copy(nameError = "Enter first and last name") }; false
            }
            else -> true
        }
    }

    private fun validateEmail(): Boolean {
        val email = _uiState.value.email.trim()
        return when {
            email.isBlank() -> {
                _uiState.update { it.copy(emailError = "Email is required") }; false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.update { it.copy(emailError = "Enter a valid email") }; false
            }
            else -> true
        }
    }

    private fun validatePassword(): Boolean {
        val pw = _uiState.value.password
        return when {
            pw.isBlank() -> {
                _uiState.update { it.copy(passwordError = "Password is required") }; false
            }
            pw.length < 8 -> {
                _uiState.update { it.copy(passwordError = "Minimum 8 characters") }; false
            }
            else -> true
        }
    }

    private fun validateConfirm(): Boolean {
        val s = _uiState.value
        return if (s.password != s.confirmPassword) {
            _uiState.update { it.copy(confirmError = "Passwords do not match") }; false
        } else true
    }

    private fun calcStrength(pw: String): Int {
        var score = 0
        if (pw.length >= 8) score++
        if (pw.any { it.isUpperCase() }) score++
        if (pw.any { it.isDigit() }) score++
        if (pw.any { !it.isLetterOrDigit() }) score++
        return score
    }

    private fun startResendCountdown() {
        viewModelScope.launch {
            repeat(60) { i ->
                _uiState.update { it.copy(otpResendSeconds = 60 - i) }
                delay(1_000)
            }
            _uiState.update { it.copy(otpResendSeconds = 0) }
        }
    }

    fun clearForm() {
        _uiState.update { AuthUiState() }   // resets to blank state
    }

    fun submitRegister() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (!validateName() || !validateEmail() || !validatePassword() || !validateConfirm()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val result = repository.registerWithEmail(state.email.trim(), state.password)
            if (result is AuthResult.Success && state.name.isNotBlank()) {
                repository.updateDisplayName(state.name.trim())
            }

            _uiState.update { it.copy(isLoading = false) }
            handleAuthResult(result)
        }
    }

    // In AuthViewModel — replace the onGoogleIdToken function and add a trigger:

    // Called by the UI button — sends a one-shot event to launch the picker
    fun onGoogleSignInClicked() {
        viewModelScope.launch {
            _events.send(AuthEvent.LaunchGoogleSignIn)
        }
    }

    fun onGoogleSignInError(message: String) {
        _uiState.update {
            it.copy(
                isLoading    = false,
                errorMessage = message
            )
        }
    }


    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val repository: AuthRepository = AuthRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == AuthViewModel::class.java)
            return AuthViewModel(repository) as T
        }
    }
}