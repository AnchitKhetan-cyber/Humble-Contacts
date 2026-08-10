package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep

// Reminder.kt
@Keep
data class Reminder(
    val reminderId: String = "",
    val ownerId: String = "",
    val contactId: String = "",
    val contactName: String = "",
    val message: String = "",
    val status: String = "pending",
    val scheduledAt: com.google.firebase.Timestamp? = null,
    val createdAt: com.google.firebase.Timestamp? = null
)