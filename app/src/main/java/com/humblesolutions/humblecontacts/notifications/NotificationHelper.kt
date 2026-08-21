package com.humblesolutions.humblecontacts.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.humblesolutions.humblecontacts.MainActivity
import com.humblesolutions.humblecontacts.R

object NotificationHelper {

    const val CHANNEL_GENERAL   = "humble_general"

    fun createChannels(context: Context) {
        // NotificationChannel only exists on API 26+ (Android 8.0). On API 24–25 the
        // concept doesn't exist, so skip channel creation entirely — guarding here
        // protects every caller (e.g. MainActivity.onCreate) from crashing on 7.x.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = Settings.System.DEFAULT_NOTIFICATION_URI
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General app notifications"
                setSound(soundUri, audioAttr)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
        )
    }

    fun notifyAction(
        context: Context,
        title: String,
        body: String
    ) = showGeneralNotification(context, (title + body).hashCode(), title, body)

    fun showGeneralNotification(
        context: Context,
        id: Int,
        title: String,
        body: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
