package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep

@Keep
data class ShareSettings(
    // Opt-out model: fields are shared by default; the user turns off what they
    // want kept private (ticket #20). A one-time migration flips legacy profiles
    // (stored all-false, from before the toggles existed) to these defaults.
    val sharePhone: Boolean = true,
    val shareEmail: Boolean = true,
    val shareCompany: Boolean = true,
    val shareLinkedIn: Boolean = true
)