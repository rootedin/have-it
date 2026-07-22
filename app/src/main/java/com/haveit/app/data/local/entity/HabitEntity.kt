package com.haveit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HabitFrequency { DAILY, WEEKLY, CUSTOM_DAYS }

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val frequency: HabitFrequency,
    val customDays: List<Int>?,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val reminderSnoozeMinutes: Int,
    val reminderSnoozeMaxCount: Int,
    val createdAt: Long,
    val archivedAt: Long?,
) {
    companion object {
        /** Repeat interval and give-up point for the "still not done" nag, until changed per habit. */
        const val DEFAULT_SNOOZE_MINUTES = 10
        const val DEFAULT_SNOOZE_MAX_COUNT = 5
    }
}
