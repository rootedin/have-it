package com.haveit.app.domain.streak

import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.domain.schedule.HabitSchedule
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Only scheduled days count. For daily/custom_days habits, unscheduled days are skipped
 * entirely rather than breaking the streak. For weekly habits, the scheduling unit is the
 * calendar week rather than a specific day. A freeze-carded day keeps the streak alive
 * without being a genuine completion.
 */
object StreakCalculator {

    fun currentStreak(
        frequency: HabitFrequency,
        customDays: List<Int>?,
        checkIns: List<CheckInEntity>,
        today: LocalDate = LocalDate.now(),
    ): Int {
        val byEpochDay = checkIns.associateBy { it.epochDay }
        return if (frequency == HabitFrequency.WEEKLY) {
            weeklyStreak(byEpochDay, today)
        } else {
            dailyStreak(
                isScheduled = { date -> isScheduledDay(frequency, customDays, date) },
                byEpochDay = byEpochDay,
                today = today,
            )
        }
    }

    private fun isScheduledDay(frequency: HabitFrequency, customDays: List<Int>?, date: LocalDate): Boolean =
        HabitSchedule.isScheduledOn(frequency, customDays, date)

    private fun dailyStreak(
        isScheduled: (LocalDate) -> Boolean,
        byEpochDay: Map<Long, CheckInEntity>,
        today: LocalDate,
    ): Int {
        fun isDone(date: LocalDate) =
            byEpochDay[date.toEpochDay()]?.let { it.completed || it.usedFreezeCard } == true

        // Today not being done yet shouldn't zero out a streak still active as of yesterday.
        var cursor = if (isScheduled(today) && !isDone(today)) today.minusDays(1) else today

        var streak = 0
        while (today.toEpochDay() - cursor.toEpochDay() <= MAX_LOOKBACK_DAYS) {
            if (isScheduled(cursor)) {
                if (!isDone(cursor)) break
                streak++
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun weeklyStreak(byEpochDay: Map<Long, CheckInEntity>, today: LocalDate): Int {
        fun weekHasCompletion(weekStart: LocalDate): Boolean =
            (0..6).any { offset ->
                byEpochDay[weekStart.plusDays(offset.toLong()).toEpochDay()]
                    ?.let { it.completed || it.usedFreezeCard } == true
            }

        // The current, still-in-progress week shouldn't break the streak before it's over.
        var weekStart = today.with(DayOfWeek.MONDAY)
        if (!weekHasCompletion(weekStart)) {
            weekStart = weekStart.minusWeeks(1)
        }

        var streak = 0
        while (today.toEpochDay() - weekStart.toEpochDay() <= MAX_LOOKBACK_DAYS && weekHasCompletion(weekStart)) {
            streak++
            weekStart = weekStart.minusWeeks(1)
        }
        return streak
    }

    private const val MAX_LOOKBACK_DAYS = 3650L
}
