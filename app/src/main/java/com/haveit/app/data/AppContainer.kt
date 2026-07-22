package com.haveit.app.data

import android.content.Context
import androidx.room.Room
import com.haveit.app.data.local.AppDatabase
import com.haveit.app.data.repository.CheckInRepository
import com.haveit.app.data.repository.HabitRepository
import com.haveit.app.data.repository.RoutineRepository
import com.haveit.app.data.settings.UserSettingsRepository

/** Manual, Hilt-free dependency container — appropriately sized for a small solo project. */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    ).addMigrations(
        AppDatabase.MIGRATION_1_2,
        AppDatabase.MIGRATION_2_3,
        AppDatabase.MIGRATION_3_4,
        AppDatabase.MIGRATION_4_5,
    ).build()

    val habitRepository = HabitRepository(database.habitDao())
    val checkInRepository = CheckInRepository(database.checkInDao())
    val routineRepository = RoutineRepository(database.routineDao())
    val userSettingsRepository = UserSettingsRepository(context.applicationContext)
}
