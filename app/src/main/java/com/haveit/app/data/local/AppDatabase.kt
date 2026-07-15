package com.haveit.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.haveit.app.data.local.dao.CheckInDao
import com.haveit.app.data.local.dao.HabitDao
import com.haveit.app.data.local.dao.RoutineDao
import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.local.entity.RoutineEntity

@Database(
    entities = [HabitEntity::class, CheckInEntity::class, RoutineEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun checkInDao(): CheckInDao
    abstract fun routineDao(): RoutineDao

    companion object {
        const val DATABASE_NAME = "have_it.db"

        /** Drops timeOfDay (manual time-of-day picker removed) in favor of a user-controlled sortOrder. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routines_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, `orderedHabitIds` TEXT NOT NULL, PRIMARY KEY(`id`))",
                )
                // Seed sortOrder from the current alphabetical position so existing routines don't jump around.
                db.execSQL(
                    "INSERT INTO `routines_new` (`id`, `name`, `sortOrder`, `orderedHabitIds`) " +
                        "SELECT `id`, `name`, " +
                        "(SELECT COUNT(*) FROM `routines` r2 WHERE r2.`name` < `routines`.`name`), " +
                        "`orderedHabitIds` FROM `routines`",
                )
                db.execSQL("DROP TABLE `routines`")
                db.execSQL("ALTER TABLE `routines_new` RENAME TO `routines`")
            }
        }

        /** Adds per-habit snooze settings. Existing habits get the old hardcoded behavior as their default. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE habits ADD COLUMN reminderSnoozeMinutes INTEGER NOT NULL " +
                        "DEFAULT ${HabitEntity.DEFAULT_SNOOZE_MINUTES}",
                )
                db.execSQL(
                    "ALTER TABLE habits ADD COLUMN reminderSnoozeMaxCount INTEGER NOT NULL " +
                        "DEFAULT ${HabitEntity.DEFAULT_SNOOZE_MAX_COUNT}",
                )
            }
        }
    }
}
