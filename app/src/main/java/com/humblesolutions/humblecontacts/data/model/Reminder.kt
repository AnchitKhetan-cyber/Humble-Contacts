package com.humblesolutions.humblecontacts.data.model

// Reminder.kt
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