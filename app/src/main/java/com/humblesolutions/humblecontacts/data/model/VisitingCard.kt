package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

/**
 * The user's digital visiting card (#64).
 *
 * Only card-*specific* data lives here — the display fields (name, company,
 * role, phone, email, LinkedIn, address) are read live from [UserProfile] so
 * there is one source of truth and nothing drifts out of sync.
 *
 * ## Field shape
 * This is the single agreed shape for the `visitingCard` map, matched by three
 * places that must stay in step: this model, the registration seed in
 * `AuthRepository`, and the editor (`VisitingCardViewModel`).
 *
 * ## Legacy compatibility
 * Profiles created before #64 stored the look under the Firestore key
 * `cardTheme`. [template] keeps that key via [PropertyName] so those documents
 * deserialize with no migration and no data loss. Every field is defaulted so a
 * partial/old `visitingCard` map never fails to deserialize.
 */
@Keep
data class VisitingCard(
    // ── Look ──────────────────────────────────────────────────────────────
    // Persisted under the legacy key `cardTheme` for backward compatibility.
    @get:PropertyName("cardTheme")
    @set:PropertyName("cardTheme")
    var template: String = "",
    val accentColor: String = "",
    val background: String = "",
    val fontStyle: String = "",

    // ── Card-specific content ─────────────────────────────────────────────
    val headline: String = "",
    val bio: String = "",
    val websiteUrl: String = "",
    val portfolioUrl: String = ""
)
