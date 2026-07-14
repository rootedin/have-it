package com.haveit.app.data.repository

import com.haveit.app.data.local.dao.CheckInDao
import com.haveit.app.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

class CheckInRepository(private val checkInDao: CheckInDao) {
    fun observeAll(): Flow<List<CheckInEntity>> = checkInDao.observeAll()

    fun observeForHabit(habitId: String): Flow<List<CheckInEntity>> = checkInDao.observeForHabit(habitId)

    suspend fun getForHabitInRange(habitId: String, startEpochDay: Long, endEpochDay: Long): List<CheckInEntity> =
        checkInDao.getForHabitInRange(habitId, startEpochDay, endEpochDay)

    suspend fun getForHabitOnDay(habitId: String, epochDay: Long): CheckInEntity? =
        checkInDao.getForHabitOnDay(habitId, epochDay)

    suspend fun upsert(checkIn: CheckInEntity) = checkInDao.upsert(checkIn)

    suspend fun deleteForDay(habitId: String, epochDay: Long) = checkInDao.deleteForDay(habitId, epochDay)

    suspend fun getAll(): List<CheckInEntity> = checkInDao.getAll()
    suspend fun upsertAll(checkIns: List<CheckInEntity>) = checkInDao.upsertAll(checkIns)
    suspend fun clear() = checkInDao.clear()
}
