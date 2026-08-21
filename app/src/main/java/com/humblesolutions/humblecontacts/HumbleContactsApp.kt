package com.humblesolutions.humblecontacts

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore

/**
 * Application entry point. Configures Firestore's offline disk cache **explicitly**
 * before any Firestore access (ticket #28), so contacts captured offline — the
 * app's primary environment is a conference hall with flaky wifi — queue locally
 * and sync on reconnect, and reads serve from the cache when offline.
 *
 * (Persistence is on by default on Android; setting it here makes the intent
 * explicit, future-proof, and safe from a silent regression.)
 */
class HumbleContactsApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Firebase.firestore.firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder().build()
                )
                .build()
    }
}
