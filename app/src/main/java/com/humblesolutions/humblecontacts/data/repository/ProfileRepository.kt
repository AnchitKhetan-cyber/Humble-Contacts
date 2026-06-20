package com.humblesolutions.humblecontacts.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun saveProfile(
        profession: String,
        company: String,
        countryCode: String,
        phone: String,
        linkedInUrl: String,
        address: String,
        bio: String
    ) {

        val firebaseUser = auth.currentUser ?: return

        val existingProfile = getCurrentUserProfile()

        val profile = UserProfile(

            userId = firebaseUser.uid,

            name = firebaseUser.displayName ?: "",

            email = firebaseUser.email ?: "",

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

            name = firebaseUser.displayName ?: "",

            email = firebaseUser.email ?: "",

            profilePhotoUrl = firebaseUser.photoUrl?.toString() ?: "",

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
        profession: String,
        company: String,
        countryCode: String,
        phone: String,
        linkedInUrl: String,
        address: String,
        bio: String
    ) {
        val uid = auth.currentUser?.uid ?: return

        usersCollection
            .document(uid)
            .update(
                mapOf(
                    "profession" to profession,
                    "company" to company,
                    "countryCode" to countryCode,
                    "phone" to phone,
                    "linkedInUrl" to linkedInUrl,
                    "address" to address,
                    "bio" to bio,
                    "ProfileCompleted" to true,
                    "updatedAt" to Timestamp.now()
                )
            )
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