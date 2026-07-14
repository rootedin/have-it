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
    val triggerSentence: String?,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val createdAt: Long,
    val archivedAt: Long?,
)
