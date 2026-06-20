package com.humblesolutions.humblecontacts.data.model

import com.google.firebase.Timestamp

data class ContactNote(
    val text: String = "",
    val createdAt: Timestamp? = null
)