package com.haveit.app.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.haveit.app.HaveItApplication
import com.haveit.app.MainActivity
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.domain.schedule.HabitSchedule
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val app = context.applicationContext as? HaveItApplication ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val habit = app.container.habitRepository.getById(habitId)
                if (habit != null && habit.archivedAt == null) {
                    val notificationsOn =
                        app.container.userSettingsRepository.settings.first().notificationsEnabled
                    val today = LocalDate.now()
                    val alreadyDone = app.container.checkInRepository
                        .getForHabitOnDay(habitId, today.toEpochDay())
                        ?.let { it.completed || it.usedFreezeCard } == true
                    val dueToday = HabitSchedule.isScheduledOn(habit.frequency, habit.customDays, today)

                    if (notificationsOn && dueToday && !alreadyDone) {
                        postNotification(context, habit)
                    }
                    // Always line up the next occurrence.
                    ReminderScheduler(context).reschedule(habit)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun postNotification(context: Context, habit: HabitEntity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        HaveItNotifications.ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            habit.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, HaveItNotifications.REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("${habit.icon} ${habit.name}")
            .setContentText("오늘도 작게 한 번, 해볼까요?")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(habit.id.hashCode(), notification)
    }

    companion object {
        const val ACTION_FIRE = "com.haveit.app.REMINDER_FIRE"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
    }
}
