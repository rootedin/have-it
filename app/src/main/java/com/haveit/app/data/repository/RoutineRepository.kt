package com.haveit.app.data.repository

import com.haveit.app.data.local.dao.RoutineDao
import com.haveit.app.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val routineDao: RoutineDao) {
    fun observeAll(): Flow<List<RoutineEntity>> = routineDao.observeAll()
    suspend fun getAll(): List<RoutineEntity> = routineDao.getAll()
    suspend fun upsert(routine: RoutineEntity) = routineDao.upsert(routine)
    suspend fun reorder(routines: List<RoutineEntity>) = routineDao.updateAll(routines)
    suspend fun delete(routine: RoutineEntity) = routineDao.delete(routine)
    suspend fun clear() = routineDao.clear()
}
