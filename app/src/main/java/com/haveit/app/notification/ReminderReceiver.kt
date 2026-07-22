package com.haveit.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haveit.app.HaveItApplication
import com.haveit.app.domain.schedule.HabitSchedule
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires when a habit's exact alarm goes off (the initial reminder or a snooze re-check). It hands off
 * to [AlarmService] for the ring/vibration/full-screen UI, then lines up the next occurrence.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val isSnooze = intent.action == ACTION_SNOOZE_CHECK
        val snoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 1)
        val app = context.applicationContext as? HaveItApplication ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val habit = app.container.habitRepository.getById(habitId) ?: return@launch
                if (habit.archivedAt != null) return@launch

                val settings = app.container.userSettingsRepository.settings.first()
                val today = LocalDate.now()
                val alreadyDone = app.container.checkInRepository
                    .getForHabitOnDay(habitId, today.toEpochDay())
                    ?.completed == true
                val dueToday = HabitSchedule.isScheduledOn(habit.frequency, habit.customDays, today)
                // Snooze re-checks stop once the per-habit cap is reached; the initial fire always rings.
                val withinSnoozeCap = !isSnooze || snoozeCount < habit.reminderSnoozeMaxCount

                if (settings.notificationsEnabled && dueToday && !alreadyDone && withinSnoozeCap) {
                    AlarmService.start(context, habit, settings.alarmSoundUri)
                    if (habit.reminderSnoozeMaxCount > 0) {
                        ReminderScheduler(context).scheduleSnooze(
                            habitId = habit.id,
                            habitName = habit.name,
                            snoozeCount = if (isSnooze) snoozeCount + 1 else 1,
                            intervalMinutes = habit.reminderSnoozeMinutes,
                        )
                    }
                }

                // Only the initial fire advances to the next scheduled day; snooze re-checks don't.
                if (!isSnooze) {
                    ReminderScheduler(context).reschedule(habit)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.haveit.app.REMINDER_FIRE"
        const val ACTION_SNOOZE_CHECK = "com.haveit.app.REMINDER_SNOOZE_CHECK"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_SNOOZE_COUNT = "snooze_count"
    }
}
