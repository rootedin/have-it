package com.haveit.app.data.local

import androidx.room.TypeConverter
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.data.local.entity.TimeOfDay

class Converters {

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? = value?.joinToString(separator = ",")

    @TypeConverter
    fun toIntList(value: String?): List<Int>? =
        value?.takeIf { it.isNotEmpty() }?.split(",")?.map { it.toInt() }

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        value.takeIf { it.isNotEmpty() }?.split(",") ?: emptyList()

    @TypeConverter
    fun fromHabitFrequency(value: HabitFrequency): String = value.name

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)

    @TypeConverter
    fun fromTimeOfDay(value: TimeOfDay): String = value.name

    @TypeConverter
    fun toTimeOfDay(value: String): TimeOfDay = TimeOfDay.valueOf(value)
}
