package com.haveit.app.domain.schedule

import com.haveit.app.data.local.entity.HabitFrequency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Shared scheduling rules: which calendar days a habit is "due" on. */
object HabitSchedule {

    /** Days before the habit was created are never due (no missed/frozen state for them). */
    fun isActiveOn(createdAtMillis: Long, date: LocalDate): Boolean =
        !date.isBefore(Instant.ofEpochMilli(createdAtMillis).atZone(ZoneId.systemDefault()).toLocalDate())

    val DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

    fun isScheduledOn(frequency: HabitFrequency, customDays: List<Int>?, date: LocalDate): Boolean =
        when (frequency) {
            HabitFrequency.DAILY -> true
            // Weekly habits can be completed on any day of the week.
            HabitFrequency.WEEKLY -> true
            HabitFrequency.CUSTOM_DAYS -> customDays.orEmpty().contains(date.dayOfWeek.value - 1)
        }

    fun label(frequency: HabitFrequency, customDays: List<Int>?): String = when (frequency) {
        HabitFrequency.DAILY -> "매일"
        HabitFrequency.WEEKLY -> "주 1회"
        HabitFrequency.CUSTOM_DAYS ->
            customDays.orEmpty().sorted().joinToString("·") { DAY_LABELS[it] }.ifEmpty { "요일 미지정" }
    }
}
