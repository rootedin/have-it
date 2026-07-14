package com.haveit.app.data.repository

import com.haveit.app.data.local.dao.HabitDao
import com.haveit.app.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    fun observeActiveHabits(): Flow<List<HabitEntity>> = habitDao.observeActiveHabits()
    fun observeArchived(): Flow<List<HabitEntity>> = habitDao.observeArchived()
    suspend fun getById(habitId: String): HabitEntity? = habitDao.getById(habitId)
    fun observeById(habitId: String): Flow<HabitEntity?> = habitDao.observeById(habitId)
    suspend fun getAll(): List<HabitEntity> = habitDao.getAll()
    suspend fun upsert(habit: HabitEntity) = habitDao.upsert(habit)
    suspend fun update(habit: HabitEntity) = habitDao.update(habit)
    suspend fun delete(habit: HabitEntity) = habitDao.delete(habit)
    suspend fun clear() = habitDao.clear()
}
