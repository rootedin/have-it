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
    version = 5,
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

        /** Removes the freeze card feature's usedFreezeCard column. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `check_ins_new` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `note` TEXT, " +
                        "PRIMARY KEY(`id`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO `check_ins_new` (`id`, `habitId`, `epochDay`, `completed`, `note`) " +
                        "SELECT `id`, `habitId`, `epochDay`, `completed`, `note` FROM `check_ins`",
                )
                db.execSQL("DROP TABLE `check_ins`")
                db.execSQL("ALTER TABLE `check_ins_new` RENAME TO `check_ins`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_habitId` ON `check_ins` (`habitId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_check_ins_habitId_epochDay` " +
                        "ON `check_ins` (`habitId`, `epochDay`)",
                )
            }
        }

        /** Drops the note column (the "오늘의 한 줄" note feature was removed). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `check_ins_new` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `completed` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO `check_ins_new` (`id`, `habitId`, `epochDay`, `completed`) " +
                        "SELECT `id`, `habitId`, `epochDay`, `completed` FROM `check_ins`",
                )
                db.execSQL("DROP TABLE `check_ins`")
                db.execSQL("ALTER TABLE `check_ins_new` RENAME TO `check_ins`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_habitId` ON `check_ins` (`habitId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_check_ins_habitId_epochDay` " +
                        "ON `check_ins` (`habitId`, `epochDay`)",
                )
            }
        }
    }
}
