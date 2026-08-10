package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class ContactNote(
    val text: String = "",
    val createdAt: Timestamp? = null
)