package com.humblesolutions.humblecontacts.data.model

import java.time.LocalDate

data class Contact(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phone: String? = null,
    val company: String? = null,
    val role: String? = null,
    val avatarUrl: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val metAt: String? = null,           // "Tech Summit 2025"
    val metOn: LocalDate? = null,
    val notes: String? = null,
    val linkedIn: String? = null,
    val lastFollowUp: LocalDate? = null,
    val followUpDue: LocalDate? = null,
    val interactionCount: Int = 0,
) {
    val fullName: String get() = "$firstName $lastName".trim()
    val initials: String get() = buildString {
        firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
        lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
    }.ifEmpty { fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?" }
}

data class NetworkStats(
    val totalContacts: Int,
    val newThisMonth: Int,
    val followUpsDue: Int,
    val meetingsThisWeek: Int,
)

data class RecentActivity(
    val contactId: String,
    val contactName: String,
    val action: ActivityType,
    val timestamp: Long,
)

enum class ActivityType { ADDED, FOLLOWED_UP, SHARED_CARD, NOTED }