package com.apptesters.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Channel ids must match CHANNEL_FOR_TYPE in the send-push Edge Function. */
object Notifications {
    const val CH_REMINDERS = "reminders"
    const val CH_PROGRESS = "progress"
    const val CH_SOCIAL = "social"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Daily check-in reminders and nudges" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "Test progress", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Testers joining, leaving and 14-day progress" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SOCIAL, "New apps", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "New apps looking for testers" }
        )
    }

    fun channelForType(type: String?): String = when (type) {
        "remind", "streak_risk", "nudge" -> CH_REMINDERS
        "new_apps", "weekly" -> CH_SOCIAL
        else -> CH_PROGRESS
    }
}
