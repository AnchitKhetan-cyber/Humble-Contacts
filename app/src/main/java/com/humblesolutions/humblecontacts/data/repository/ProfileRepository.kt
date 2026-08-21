package com.humblesolutions.humblecontacts.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.humblecontacts.data.model.ShareSettings
import com.humblesolutions.humblecontacts.data.model.UserProfile
import kotlinx.coroutines.tasks.await

class ProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val usersCollection
        get() = firestore.collection("users")

    suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null

        val snapshot = usersCollection
            .document(uid)
            .get()
            .await()

        val profile = snapshot.toObject(UserProfile::class.java) ?: return null

        // One-time migration (#20): profiles created before the privacy toggles
        // stored every share flag as false. Since those were never user-editable,
        // flip them to the opt-out defaults (share on) so existing cards keep
        // sharing, and mark the profile initialised so this runs only once.
        if (!profile.shareSettingsInitialized) {
            val migrated = ShareSettings()
            usersCollection.document(uid).update(
                mapOf(
                    "shareSettings" to migrated,
                    "shareSettingsInitialized" to true
                )
            ).await()
            return profile.copy(
                shareSettings = migrated,
                shareSettingsInitialized = true
            )
        }

        return profile
    }

    /** Persists the user's per-field share (privacy) settings (#20). */
    suspend fun updateShareSettings(shareSettings: ShareSettings) {
        val uid = auth.currentUser?.uid ?: return
        usersCollection.document(uid).update(
            mapOf(
                "shareSettings" to shareSettings,
                "shareSettingsInitialized" to true,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    suspend fun saveProfile(
        name: String? = null,
        email: String? = null,
        profession: String,
        company: String,
        countryCode: String,
        phone: String,
        linkedInUrl: String,
        address: String,
        bio: String
    ) {

        val firebaseUser = auth.currentUser ?: return

        val resolvedName = name ?: firebaseUser.displayName ?: ""
        val resolvedEmail = email ?: firebaseUser.email ?: ""

        if (name != null && name.isNotBlank() && name != firebaseUser.displayName) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
        }

        val existingProfile = getCurrentUserProfile()

        val profile = UserProfile(

            userId = firebaseUser.uid,

            name = resolvedName,

            email = resolvedEmail,

            profilePhotoUrl = firebaseUser.photoUrl?.toString() ?: "",

            profession = profession,

            company = company,

            countryCode = countryCode,

            phone = phone,

            linkedInUrl = linkedInUrl,

            address = address,

            bio = bio,

            authProviders =
                existingProfile?.authProviders
                    ?: listOf("google"),

            shareSettings =
                existingProfile?.shareSettings
                    ?: UserProfile().shareSettings,

            // Carry the migration marker so this full .set() doesn't reset it to
            // false and re-trigger the migration (which would wipe the user's
            // per-field share choices). New profiles start initialised (#20).
            shareSettingsInitialized =
                existingProfile?.shareSettingsInitialized ?: true,

            stats =
                existingProfile?.stats
                    ?: UserProfile().stats,

            visitingCard =
                existingProfile?.visitingCard
                    ?: UserProfile().visitingCard,

            isProfileCompleted = true,

            createdAt =
                existingProfile?.createdAt
                    ?: Timestamp.now(),

            updatedAt = Timestamp.now()
        )

        usersCollection
            .document(firebaseUser.uid)
            .set(profile)
            .await()
    }

    suspend fun skipProfile() {

        val firebaseUser = auth.currentUser ?: return

        val existingProfile = getCurrentUserProfile()

        val profile = UserProfile(

            userId = firebaseUser.uid,

            // Prefer the already-saved values; Firebase Auth has no email for phone
            // users, so falling back to it here would wipe a saved email.
            name = existingProfile?.name?.takeIf { it.isNotBlank() }
                ?: firebaseUser.displayName ?: "",

            email = existingProfile?.email?.takeIf { it.isNotBlank() }
                ?: firebaseUser.email ?: "",

            profilePhotoUrl = existingProfile?.profilePhotoUrl?.takeIf { it.isNotBlank() }
                ?: firebaseUser.photoUrl?.toString() ?: "",

            profession = existingProfile?.profession ?: "",

            company = existingProfile?.company ?: "",

            countryCode = existingProfile?.countryCode ?: "+91",

            phone = existingProfile?.phone ?: "",

            linkedInUrl = existingProfile?.linkedInUrl ?: "",

            address = existingProfile?.address ?: "",

            bio = existingProfile?.bio ?: "",

            authProviders =
                existingProfile?.authProviders
                    ?: listOf("google"),

            shareSettings =
                existingProfile?.shareSettings
                    ?: UserProfile().shareSettings,

            // Carry the migration marker so this full .set() doesn't reset it to
            // false and re-trigger the migration (which would wipe the user's
            // per-field share choices). New profiles start initialised (#20).
            shareSettingsInitialized =
                existingProfile?.shareSettingsInitialized ?: true,

            stats =
                existingProfile?.stats
                    ?: UserProfile().stats,

            visitingCard =
                existingProfile?.visitingCard
                    ?: UserProfile().visitingCard,

            // Change to false if you want to show onboarding again next login.
            isProfileCompleted = true,

            createdAt =
                existingProfile?.createdAt
                    ?: Timestamp.now(),

            updatedAt = Timestamp.now()
        )

        usersCollection
            .document(firebaseUser.uid)
            .set(profile)
            .await()
    }

    suspend fun updateProfile(
        name: String? = null,
        email: String? = null,
        profession: String,
        company: String,
        countryCode: String,
        phone: String,
        linkedInUrl: String,
        address: String,
        bio: String
    ) {
        val firebaseUser = auth.currentUser ?: return

        // Keep the Firebase Auth display name in sync when it changes.
        if (name != null && name.isNotBlank() && name != firebaseUser.displayName) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
        }

        val updates = mutableMapOf<String, Any>(
            "profession" to profession,
            "company" to company,
            "countryCode" to countryCode,
            "phone" to phone,
            "linkedInUrl" to linkedInUrl,
            "address" to address,
            "bio" to bio,
            "profileCompleted" to true,
            "updatedAt" to Timestamp.now()
        )

        // Only phone users edit their name/email here; persist them when provided.
        if (name != null) updates["name"] = name
        if (email != null) updates["email"] = email

        usersCollection
            .document(firebaseUser.uid)
            .update(updates)
            .await()
    }

    suspend fun isProfileCompleted(): Boolean {

        val uid = auth.currentUser?.uid ?: return false

        val snapshot = usersCollection.document(uid).get().await()

        android.util.Log.d("PROFILE", "UID = $uid")
        android.util.Log.d("PROFILE", "Exists = ${snapshot.exists()}")
        android.util.Log.d("PROFILE", "Data = ${snapshot.data}")
        android.util.Log.d(
            "PROFILE",
            "isProfileCompleted = ${snapshot.getBoolean("profileCompleted")}"
        )

        return snapshot.getBoolean("profileCompleted") == true
    }
}