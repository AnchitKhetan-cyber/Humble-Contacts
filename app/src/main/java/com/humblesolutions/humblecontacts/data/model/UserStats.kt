package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep

@Keep
data class UserStats(
    val totalContacts: Int = 0,
    val totalEvents: Int = 0,
    val totalReminders: Int = 0
)