package com.haveit.app.ui.weeklyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.data.repository.CheckInRepository
import com.haveit.app.data.repository.HabitRepository
import com.haveit.app.domain.schedule.HabitSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DayBar(
    val label: String,
    /** Fraction of scheduled habits genuinely completed that day. */
    val completedRatio: Float,
    /** Fraction protected by a freeze card (stacked on top of completed). */
    val frozenRatio: Float,
    val scheduledCount: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
) {
    val hasData: Boolean get() = !isFuture && scheduledCount > 0
    val totalRatio: Float get() = completedRatio + frozenRatio
}

data class WeeklyReportUiState(
    val isLoading: Boolean = true,
    val weekLabel: String = "",
    val bars: List<DayBar> = emptyList(),
    val averagePercent: Int? = null,
    val frozenDays: Int = 0,
    val weeklyHabitTotal: Int = 0,
    val weeklyHabitDone: Int = 0,
    val hasAnyHabit: Boolean = false,
)

class WeeklyReportViewModel(
    habitRepository: HabitRepository,
    checkInRepository: CheckInRepository,
) : ViewModel() {

    val uiState: StateFlow<WeeklyReportUiState> = combine(
        habitRepository.observeActiveHabits(),
        checkInRepository.observeAll(),
    ) { habits, checkIns ->
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val byHabitDay = checkIns.associateBy { it.habitId to it.epochDay }

        val dailyLike = habits.filter { it.frequency != HabitFrequency.WEEKLY }
        val bars = (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            val isFuture = date.isAfter(today)
            val scheduled = dailyLike.filter {
                HabitSchedule.isActiveOn(it.createdAt, date) &&
                    HabitSchedule.isScheduledOn(it.frequency, it.customDays, date)
            }
            var completed = 0
            var frozen = 0
            scheduled.forEach { habit ->
                val entry = byHabitDay[habit.id to date.toEpochDay()]
                when {
                    entry?.completed == true -> completed++
                    entry?.usedFreezeCard == true -> frozen++
                }
            }
            val n = scheduled.size
            DayBar(
                label = HabitSchedule.DAY_LABELS[offset],
                completedRatio = if (n == 0) 0f else completed.toFloat() / n,
                frozenRatio = if (n == 0) 0f else frozen.toFloat() / n,
                scheduledCount = n,
                isToday = date == today,
                isFuture = isFuture,
            )
        }

        val withData = bars.filter { it.hasData }
        val weeklyHabits = habits.filter { it.frequency == HabitFrequency.WEEKLY }
        val fmt = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

        WeeklyReportUiState(
            isLoading = false,
            weekLabel = "${monday.format(fmt)} – ${monday.plusDays(6).format(fmt)}",
            bars = bars,
            averagePercent = withData.takeIf { it.isNotEmpty() }
                ?.let { (it.map { b -> b.totalRatio }.average() * 100).roundToInt() },
            frozenDays = bars.count { it.frozenRatio > 0f },
            weeklyHabitTotal = weeklyHabits.size,
            weeklyHabitDone = weeklyHabits.count { habit ->
                (0..6).any { off ->
                    byHabitDay[habit.id to monday.plusDays(off.toLong()).toEpochDay()]
                        ?.let { it.completed || it.usedFreezeCard } == true
                }
            },
            hasAnyHabit = habits.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyReportUiState())

    companion object {
        fun factory(app: HaveItApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WeeklyReportViewModel(app.container.habitRepository, app.container.checkInRepository)
            }
        }
    }
}
