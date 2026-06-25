package com.humblesolutions.humblecontacts.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.humblecontacts.data.repository.ProfileRepository
import com.humblesolutions.humblecontacts.notifications.NotificationHelper
import com.humblesolutions.humblecontacts.ui.auth.CountryCode
import com.humblesolutions.humblecontacts.ui.auth.countryCodes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompleteProfileViewModel(
    application: Application,
    private val repository: ProfileRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CompleteProfileUiState())
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<CompleteProfileEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                val isPhoneUser = firebaseUser?.providerData
                    ?.any { it.providerId == "phone" } == true

                val profile = repository.getCurrentUserProfile()

                _uiState.update {
                    it.copy(
                        photoUrl    = profile?.profilePhotoUrl ?: firebaseUser?.photoUrl?.toString() ?: "",
                        name        = profile?.name ?: firebaseUser?.displayName ?: "",
                        email       = profile?.email ?: firebaseUser?.email ?: "",
                        isPhoneUser = isPhoneUser,
                        nameInput   = profile?.name?.takeIf { n -> n.isNotBlank() }
                            ?: firebaseUser?.displayName ?: "",
                        emailInput  = profile?.email?.takeIf { e -> e.isNotBlank() }
                            ?: firebaseUser?.email ?: "",
                        profession  = profile?.profession ?: "",
                        company     = profile?.company ?: "",
                        countryCode = profile?.countryCode?.let { dc ->
                            countryCodes.firstOrNull { c -> c.dialCode == dc }
                        } ?: countryCodes.first { c -> c.dialCode == "+91" },
                        phone       = profile?.phone ?: "",
                        linkedInUrl = profile?.linkedInUrl ?: "",
                        address     = profile?.address ?: "",
                        bio         = profile?.bio ?: ""
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Unable to load profile.")
                }
            }
        }
    }

    fun onNameInputChange(value: String) {
        _uiState.update {
            it.copy(nameInput = value, nameError = null)
        }
    }

    fun onEmailInputChange(value: String) {
        _uiState.update {
            it.copy(emailInput = value, emailError = null)
        }
    }

    fun onProfessionChange(value: String) {
        _uiState.update {
            it.copy(
                profession = value,
                professionError = null
            )
        }
    }

    fun onCompanyChange(value: String) {
        _uiState.update {
            it.copy(
                company = value,
                errorMessage = null
            )
        }
    }

    fun onCountryCodeChange(country: CountryCode) {
        _uiState.update {
            it.copy(countryCode = country)
        }
    }

    fun onPhoneChange(value: String) {
        _uiState.update {
            it.copy(
                phone = value.filter(Char::isDigit),
                phoneError = null
            )
        }
    }

    fun onLinkedInChange(value: String) {
        _uiState.update {
            it.copy(linkedInUrl = value)
        }
    }

    fun onAddressChange(value: String) {
        _uiState.update {
            it.copy(address = value)
        }
    }

    fun onBioChange(value: String) {

        _uiState.update {

            it.copy(
                bio = value.take(250)
            )

        }

    }

    fun dismissError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    fun saveProfile() {

        if (_uiState.value.isLoading) return
        val state = _uiState.value

        var nameError: String? = null
        var emailError: String? = null
        var professionError: String? = null
        var phoneError: String? = null

        if (state.isPhoneUser) {
            when {
                state.nameInput.isBlank() -> nameError = "Name is required."
                state.nameInput.trim().length < 2 -> nameError = "Name is too short."
            }
            when {
                state.emailInput.isBlank() -> emailError = "Email is required."
                !android.util.Patterns.EMAIL_ADDRESS.matcher(state.emailInput.trim()).matches() ->
                    emailError = "Enter a valid email address."
            }
        }

        when {

            state.profession.isBlank() ->
                professionError = "Profession is required."

            state.profession.length < 2 ->
                professionError = "Profession is too short."
        }

        when {

            state.phone.isBlank() ->
                phoneError = "Phone number is required."

            state.phone.length != 10 ->
                phoneError = "Enter a valid 10-digit phone number."
        }

        if (nameError != null || emailError != null || professionError != null || phoneError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    professionError = professionError,
                    phoneError = phoneError
                )
            }
            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(isLoading = true)
            }

            try {

                if (!validateLinkedIn()) {

                    _uiState.update {
                        it.copy(isLoading = false)
                    }

                    return@launch
                }

                repository.saveProfile(
                    name = if (state.isPhoneUser) state.nameInput.trim() else null,
                    email = if (state.isPhoneUser) state.emailInput.trim() else null,
                    profession = state.profession.trim(),
                    company = state.company.trim(),
                    countryCode = state.countryCode.dialCode,
                    phone = state.phone.trim(),
                    linkedInUrl = if (state.linkedInUrl.isBlank()) {
                        ""
                    } else {
                        "https://www.linkedin.com/in/${state.linkedInUrl.trim()}"
                    },
                    address = state.address.trim(),
                    bio = state.bio.trim()
                )

                NotificationHelper.notifyAction(
                    getApplication(),
                    "Profile Saved!",
                    "Your profile has been completed successfully."
                )

                _events.send(CompleteProfileEvent.NavigateHome)

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Something went wrong."
                    )
                }

            }
        }
    }

    fun skipProfile() {

        if (_uiState.value.isLoading) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(isLoading = true)
            }

            try {

                repository.skipProfile()

                _events.send(CompleteProfileEvent.NavigateHome)

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unable to skip profile."
                    )
                }

            }
        }
    }

    fun updateProfile() {

        if (_uiState.value.isLoading) return

        val state = _uiState.value

        viewModelScope.launch {

            _uiState.update {
                it.copy(isLoading = true)
            }

            try {

                repository.updateProfile(
                    profession = state.profession,
                    company = state.company,
                    countryCode = state.countryCode.dialCode,
                    phone = state.phone,
                    linkedInUrl = if (state.linkedInUrl.isBlank()) {
                        ""
                    } else {
                        "https://www.linkedin.com/in/${state.linkedInUrl.trim()}"
                    },
                    address = state.address,
                    bio = state.bio
                )

                NotificationHelper.notifyAction(
                    getApplication(),
                    "Profile Updated!",
                    "Your profile changes have been saved."
                )

                _events.send(CompleteProfileEvent.NavigateHome)

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }

            }

        }

    }

    companion object {

        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return CompleteProfileViewModel(
                        application,
                        ProfileRepository()
                    ) as T
                }
            }
    }


    private fun validateLinkedIn(): Boolean {

        val url = uiState.value.linkedInUrl.trim()

        if (url.isBlank()) return true

        val valid =
            url.startsWith("https://linkedin.com/") ||
                    url.startsWith("https://www.linkedin.com/") ||
                    url.startsWith("linkedin.com/") ||
                    url.startsWith("www.linkedin.com/")

        if (!valid) {
            _uiState.update {
                it.copy(
                    errorMessage = "Enter a valid LinkedIn profile URL."
                )
            }
        }

        return valid
    }
}