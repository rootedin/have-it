package com.haveit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    /** Execution order == membership; there is no separate habitIds field by design. */
    val orderedHabitIds: List<String>,
)
