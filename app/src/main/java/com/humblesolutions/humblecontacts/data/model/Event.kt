package com.humblesolutions.humblecontacts.data.model

// Event.kt
data class Event(
    val eventId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val location: String = "",
    val category: String = "",
    val contactCount: Int = 0,
    val eventDate: com.google.firebase.Timestamp? = null,
    val createdAt: com.google.firebase.Timestamp? = null
)