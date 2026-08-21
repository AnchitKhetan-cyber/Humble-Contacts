package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

@Keep
data class UserProfile(
    val userId: String = "",

    // Google Sign-In
    val name: String = "",
    val email: String = "",
    val profilePhotoUrl: String = "",

    // User details
    val profession: String = "",
    val company: String = "",
    val phone: String = "",
    val countryCode: String = "+91",
    val linkedInUrl: String = "",
    val address: String = "",
    val bio: String = "",

    // App settings
    val authProviders: List<String> = emptyList(),
    val shareSettings: ShareSettings = ShareSettings(),
    // One-time migration marker (#20): false on legacy profiles whose share flags
    // predate the privacy toggles (all stored false). getCurrentUserProfile()
    // flips those to the opt-out defaults once and sets this true.
    val shareSettingsInitialized: Boolean = false,
    val stats: UserStats = UserStats(),
    val visitingCard: VisitingCard = VisitingCard(),

    // Metadata
    var isProfileCompleted: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)