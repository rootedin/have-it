package com.haveit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haveit.app.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins")
    fun observeAll(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId ORDER BY epochDay ASC")
    fun observeForHabit(habitId: String): Flow<List<CheckInEntity>>

    @Query(
        "SELECT * FROM check_ins WHERE habitId = :habitId " +
            "AND epochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY epochDay ASC",
    )
    suspend fun getForHabitInRange(habitId: String, startEpochDay: Long, endEpochDay: Long): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getForHabitOnDay(habitId: String, epochDay: Long): CheckInEntity?

    @Query("SELECT * FROM check_ins")
    suspend fun getAll(): List<CheckInEntity>

    @Query("DELETE FROM check_ins")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkIn: CheckInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(checkIns: List<CheckInEntity>)

    @Query("DELETE FROM check_ins WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteForDay(habitId: String, epochDay: Long)
}
