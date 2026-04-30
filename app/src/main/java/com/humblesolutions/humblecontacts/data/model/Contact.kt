package com.humblesolutions.humblecontacts.data.model

import java.time.LocalDate

data class Contact(
    val id: String,
    val firstName: String,
    val lastName: String,
    val role: String?          = null,
    val company: String?       = null,
    val phone: String?         = null,
    val email: String?         = null,
    val linkedIn: String?      = null,
    val website: String?       = null,

    // Meeting context
    val meetingPlace: String?  = null,
    val meetingDate: LocalDate? = null,
    val eventName: String?     = null,
    val tags: List<String>     = emptyList(),

    // Notes & media
    val notes: String?         = null,
    val hasVoiceNote: Boolean  = false,
    val hasSelfie: Boolean     = false,
    val hasBusinessCard: Boolean = false,

    // App metadata
    val isFavorite: Boolean    = false,
    val followUpDue: LocalDate? = null,
    val addedDate: LocalDate   = LocalDate.now(),

    // Avatar colour index (0–4 cycles through brand palette)
    val avatarColorIndex: Int  = 0,
) {
    val fullName: String get() = "$firstName $lastName".trim()
    val initials: String get() = buildString {
        if (firstName.isNotBlank()) append(firstName.first().uppercaseChar())
        if (lastName.isNotBlank())  append(lastName.first().uppercaseChar())
    }
}