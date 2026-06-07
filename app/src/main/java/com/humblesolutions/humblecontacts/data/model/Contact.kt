package com.humblesolutions.humblecontacts.data.model

import com.google.firebase.Timestamp

data class Contact(
    val contactId: String = "",
    val ownerId: String = "",
    val fullName: String = "",
    val jobRole: String = "",
    val company: String = "",
    val industry: String = "",
    val email: String = "",
    val phone: String = "",
    val tags: List<String> = emptyList(),
    val isFavourite: Boolean = false,
    val meetingDate: Timestamp? = null,
    val meetingLocation: String = "",
    val eventName: String = "",
    val conversationNotes: String = "",
    val entryMethod: String = "manual",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    // Computed helpers to avoid changing your UI code much
    val initials: String get() = fullName
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    val metOn: String get() = meetingDate
        ?.toDate()
        ?.let {
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it)
        } ?: ""
}