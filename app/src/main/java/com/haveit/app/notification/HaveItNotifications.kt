package com.haveit.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object HaveItNotifications {
    // Bumped whenever channel behavior changes: importance/sound/vibration are locked in at creation
    // time and can't be edited for users who already have an older channel installed.
    const val REMINDER_CHANNEL_ID = "habit_reminders_alarm_v3"
    private val LEGACY_REMINDER_CHANNEL_IDS =
        listOf("habit_reminders", "habit_reminders_alarm", "habit_reminders_alarm_v2")

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        LEGACY_REMINDER_CHANNEL_IDS.forEach { manager.deleteNotificationChannel(it) }
        if (manager.getNotificationChannel(REMINDER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "습관 리마인더",
                // HIGH so the full-screen intent can fire and the notification heads-up.
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "설정한 시간에 습관을 잊지 않도록 알람처럼 알려드려요"
                // The alarm ringtone and vibration are driven by AlarmService, not the channel, so the
                // channel itself stays silent — otherwise it would double up with a one-shot blip.
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }
}
