package com.haveit.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object HaveItNotifications {
    const val REMINDER_CHANNEL_ID = "habit_reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(REMINDER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "습관 리마인더",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "설정한 시간에 습관을 잊지 않도록 알려드려요"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
