package com.humblesolutions.humblecontacts.ui.profile

import com.humblesolutions.humblecontacts.ui.auth.CountryCode
import com.humblesolutions.humblecontacts.ui.auth.countryCodes

data class CompleteProfileUiState(

    // Google account information
    val photoUrl: String = "",
    val name: String = "",
    val email: String = "",

    // Editable fields
    val profession: String = "",
    val company: String = "",
    val countryCode: CountryCode = countryCodes.first { it.dialCode == "+91" },
    val phone: String = "",
    val linkedInUrl: String = "",
    val address: String = "",
    val bio: String = "",

    // Validation
    val professionError: String? = null,
    val phoneError: String? = null,

    // Loading
    val isLoading: Boolean = false,

    // General error
    val errorMessage: String? = null
)