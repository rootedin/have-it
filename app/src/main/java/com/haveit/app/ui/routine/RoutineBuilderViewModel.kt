package com.haveit.app.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.local.entity.RoutineEntity
import com.haveit.app.data.repository.HabitRepository
import com.haveit.app.data.repository.RoutineRepository
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineBuilderUiState(
    val routines: List<RoutineEntity> = emptyList(),
    val habitsById: Map<String, HabitEntity> = emptyMap(),
    val activeHabits: List<HabitEntity> = emptyList(),
)

class RoutineBuilderViewModel(
    private val routineRepository: RoutineRepository,
    private val habitRepository: HabitRepository,
) : ViewModel() {

    val uiState: StateFlow<RoutineBuilderUiState> = combine(
        routineRepository.observeAll(),
        habitRepository.observeActiveHabits(),
    ) { routines, habits ->
        RoutineBuilderUiState(
            routines = routines,
            habitsById = habits.associateBy { it.id },
            activeHabits = habits,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineBuilderUiState())

    fun save(id: String?, name: String, orderedHabitIds: List<String>) {
        viewModelScope.launch {
            val routines = uiState.value.routines
            val sortOrder = routines.find { it.id == id }?.sortOrder
                ?: ((routines.maxOfOrNull { it.sortOrder } ?: -1) + 1)
            routineRepository.upsert(
                RoutineEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    name = name.trim().ifBlank { "새 루틴" },
                    sortOrder = sortOrder,
                    orderedHabitIds = orderedHabitIds,
                ),
            )
        }
    }

    fun moveRoutine(routine: RoutineEntity, delta: Int) {
        viewModelScope.launch {
            val ordered = uiState.value.routines.sortedBy { it.sortOrder }
            val index = ordered.indexOfFirst { it.id == routine.id }
            val target = index + delta
            if (index < 0 || target < 0 || target >= ordered.size) return@launch
            val reordered = ordered.toMutableList()
            reordered.add(target, reordered.removeAt(index))
            routineRepository.reorder(reordered.mapIndexed { i, r -> r.copy(sortOrder = i) })
        }
    }

    fun delete(routine: RoutineEntity) {
        viewModelScope.launch { routineRepository.delete(routine) }
    }

    companion object {
        fun factory(app: HaveItApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RoutineBuilderViewModel(app.container.routineRepository, app.container.habitRepository)
            }
        }
    }
}
