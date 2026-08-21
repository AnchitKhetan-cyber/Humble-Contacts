package com.humblesolutions.humblecontacts.notifications

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.humblesolutions.humblecontacts.data.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HumbleFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // TODO: send token to your backend / Firestore user doc if needed
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Respect the user's notification preference
        CoroutineScope(Dispatchers.IO).launch {
            val enabled = SettingsPreferences(applicationContext).notificationsEnabled.first()
            if (!enabled) return@launch

            // Guard: POST_NOTIFICATIONS permission (Android 13+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return@launch
            }

            val data        = message.data
            val notification = message.notification

            val title = data["title"] ?: notification?.title ?: "HumbleContacts"
            val body  = data["body"]  ?: notification?.body  ?: ""

            val notifId = System.currentTimeMillis().toInt()

            NotificationHelper.showGeneralNotification(
                applicationContext, notifId, title, body
            )
        }
    }
}
