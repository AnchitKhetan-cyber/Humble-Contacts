package com.humblesolutions.humblecontacts.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.humblesolutions.humblecontacts.MainActivity
import com.humblesolutions.humblecontacts.R

object NotificationHelper {

    const val CHANNEL_REMINDERS = "humble_reminders"
    const val CHANNEL_GENERAL   = "humble_general"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Follow-up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to follow up with your contacts"
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }
        )
    }

    fun showReminderNotification(
        context: Context,
        id: Int,
        contactName: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Follow up with $contactName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
