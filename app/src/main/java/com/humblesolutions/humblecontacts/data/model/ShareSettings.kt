package com.humblesolutions.humblecontacts.data.model

data class ShareSettings(
    val sharePhone: Boolean = false,
    val shareEmail: Boolean = false,
    val shareCompany: Boolean = false,
    val shareLinkedIn: Boolean = false
)