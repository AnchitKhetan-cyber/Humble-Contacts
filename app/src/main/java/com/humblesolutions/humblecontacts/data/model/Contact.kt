package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

@Keep
data class Contact(
    val contactId: String = "",
    val ownerId: String = "",
    val fullName: String = "",
    val jobRole: String = "",
    val company: String = "",
    val industry: String = "",
    val email: String = "",
    val phone: String = "",
    val linkedIn: String = "",
    val address: String = "",
    val businessCardImage: String = "",
    val media: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val favourite: Boolean = false,
    val meetingDate: Timestamp? = null,
    val meetingLocation: String = "",
    val eventName: String = "",
    val conversationNotes: List<ContactNote> = emptyList(),
    val entryMethod: String = "manual",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val initials: String
        get() = fullName
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

    @get:Exclude
    val metOn: String
        get() = meetingDate?.toDate()?.let {
            java.text.SimpleDateFormat(
                "MMM dd, yyyy",
                java.util.Locale.getDefault()
            ).format(it)
        } ?: ""
}