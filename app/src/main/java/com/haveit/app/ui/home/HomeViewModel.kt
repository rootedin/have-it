package com.haveit.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.data.local.entity.RoutineEntity
import com.haveit.app.data.repository.CheckInRepository
import com.haveit.app.data.repository.HabitRepository
import com.haveit.app.data.repository.RoutineRepository
import com.haveit.app.data.settings.UserSettingsRepository
import com.haveit.app.domain.schedule.HabitSchedule
import com.haveit.app.domain.streak.StreakCalculator
import com.haveit.app.widget.HabitWidget
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayHabitUi(
    val habit: HabitEntity,
    val doneToday: Boolean,
    val frozenToday: Boolean,
    val doneThisWeek: Boolean,
    val streak: Int,
) {
    val isWeekly: Boolean get() = habit.frequency == HabitFrequency.WEEKLY
    val checked: Boolean get() = if (isWeekly) doneToday || doneThisWeek else doneToday || frozenToday
}

data class HomeSection(val title: String?, val items: List<TodayHabitUi>)

data class HomeUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val sections: List<HomeSection> = emptyList(),
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val freezeCardsAvailable: Int = 0,
) {
    val isEmpty: Boolean get() = totalCount == 0
}

class HomeViewModel(
    application: Application,
    habitRepository: HabitRepository,
    routineRepository: RoutineRepository,
    private val checkInRepository: CheckInRepository,
    settingsRepository: UserSettingsRepository,
) : AndroidViewModel(application) {

    val uiState: StateFlow<HomeUiState> = combine(
        habitRepository.observeActiveHabits(),
        checkInRepository.observeAll(),
        routineRepository.observeAll(),
        settingsRepository.settings,
    ) { habits, checkIns, routines, settings ->
        val today = LocalDate.now()
        val mondayEpoch = today.with(DayOfWeek.MONDAY).toEpochDay()
        val grouped = checkIns.groupBy { it.habitId }

        val todays = habits
            .filter { HabitSchedule.isScheduledOn(it.frequency, it.customDays, today) }
            .associateWith { habit ->
                val list = grouped[habit.id].orEmpty()
                val todayEntry = list.find { it.epochDay == today.toEpochDay() }
                TodayHabitUi(
                    habit = habit,
                    doneToday = todayEntry?.completed == true,
                    frozenToday = todayEntry?.usedFreezeCard == true,
                    doneThisWeek = list.any {
                        it.completed && it.epochDay >= mondayEpoch && it.epochDay <= today.toEpochDay()
                    },
                    streak = StreakCalculator.currentStreak(habit.frequency, habit.customDays, list, today),
                )
            }

        val sections = buildSections(todays, routines)
        val allItems = sections.flatMap { it.items }

        HomeUiState(
            isLoading = false,
            date = today,
            sections = sections,
            doneCount = allItems.count { it.checked },
            totalCount = allItems.size,
            freezeCardsAvailable = settings.freezeCardsAvailable,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildSections(
        todays: Map<HabitEntity, TodayHabitUi>,
        routines: List<RoutineEntity>,
    ): List<HomeSection> {
        val byId = todays.mapKeys { it.key.id }
        if (routines.isEmpty()) {
            return listOf(HomeSection(null, todays.values.toList())).filter { it.items.isNotEmpty() }
        }
        val used = mutableSetOf<String>()
        val sections = mutableListOf<HomeSection>()
        routines.forEach { routine ->
            val items = routine.orderedHabitIds.mapNotNull { id -> byId[id] }
            items.forEach { used.add(it.habit.id) }
            if (items.isNotEmpty()) {
                sections.add(HomeSection(routine.name, items))
            }
        }
        val leftovers = todays.values.filter { it.habit.id !in used }
        if (leftovers.isNotEmpty()) {
            val title = if (sections.isEmpty()) null else "기타"
            sections.add(HomeSection(title, leftovers))
        }
        return sections
    }

    fun toggleToday(item: TodayHabitUi) {
        viewModelScope.launch {
            val epochDay = LocalDate.now().toEpochDay()
            val existing = checkInRepository.getForHabitOnDay(item.habit.id, epochDay)
            if (existing?.completed == true) {
                checkInRepository.deleteForDay(item.habit.id, epochDay)
            } else {
                checkInRepository.upsert(
                    CheckInEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        habitId = item.habit.id,
                        epochDay = epochDay,
                        completed = true,
                        usedFreezeCard = existing?.usedFreezeCard ?: false,
                        note = existing?.note,
                    ),
                )
            }
            HabitWidget.refresh(getApplication())
        }
    }

    companion object {
        fun factory(app: HaveItApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    app,
                    app.container.habitRepository,
                    app.container.routineRepository,
                    app.container.checkInRepository,
                    app.container.userSettingsRepository,
                )
            }
        }
    }
}
