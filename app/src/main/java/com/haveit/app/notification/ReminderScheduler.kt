package com.haveit.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.domain.schedule.HabitSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules one-shot exact alarms per habit reminder. When an alarm fires,
 * [ReminderReceiver] posts the notification and asks this scheduler for the next slot,
 * so a single pending alarm per habit is always enough.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun reschedule(habit: HabitEntity) {
        cancel(habit.id)
        if (habit.archivedAt != null) return
        val hour = habit.reminderHour ?: return
        val minute = habit.reminderMinute ?: return
        val triggerAt = nextTriggerMillis(habit.frequency.name, habit.customDays, hour, minute)
        val pending = pendingIntent(habit.id, habit.name)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(habitId: String) {
        alarmManager.cancel(pendingIntent(habitId, null))
    }

    private fun pendingIntent(habitId: String, habitName: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            data = android.net.Uri.parse("haveit://reminder/$habitId")
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            if (habitName != null) putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habitName)
        }
        return PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Next date-time strictly after [from] on which this habit is scheduled, in epoch millis. */
        fun nextTriggerMillis(
            frequencyName: String,
            customDays: List<Int>?,
            hour: Int,
            minute: Int,
            from: LocalDateTime = LocalDateTime.now(),
        ): Long {
            val frequency = com.haveit.app.data.local.entity.HabitFrequency.valueOf(frequencyName)
            var date: LocalDate = from.toLocalDate()
            repeat(8) {
                val candidate = date.atTime(hour, minute)
                if (candidate.isAfter(from) &&
                    HabitSchedule.isScheduledOn(frequency, customDays, date)
                ) {
                    return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                date = date.plusDays(1)
            }
            // Fallback (shouldn't happen): tomorrow at the requested time.
            return from.toLocalDate().plusDays(1).atTime(hour, minute)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
}
