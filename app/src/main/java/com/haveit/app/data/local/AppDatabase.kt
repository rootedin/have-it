package com.haveit.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.haveit.app.data.local.dao.CheckInDao
import com.haveit.app.data.local.dao.HabitDao
import com.haveit.app.data.local.dao.RoutineDao
import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.local.entity.RoutineEntity

@Database(
    entities = [HabitEntity::class, CheckInEntity::class, RoutineEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun checkInDao(): CheckInDao
    abstract fun routineDao(): RoutineDao

    companion object {
        const val DATABASE_NAME = "have_it.db"
    }
}
