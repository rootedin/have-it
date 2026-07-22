package com.haveit.app.ui.weeklyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.data.repository.CheckInRepository
import com.haveit.app.data.repository.HabitRepository
import com.haveit.app.domain.schedule.HabitSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ReportPeriod { WEEK, MONTH }

data class DayBar(
    val label: String,
    /** Fraction of scheduled habits genuinely completed that day. */
    val completedRatio: Float,
    val scheduledCount: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
) {
    val hasData: Boolean get() = !isFuture && scheduledCount > 0
}

/** One cell in the month grid; [date] is null for leading blanks that pad the first week. */
data class MonthReportCell(
    val date: LocalDate?,
    val completedRatio: Float,
    val hasData: Boolean,
    val isToday: Boolean,
)

data class WeeklyReportUiState(
    val isLoading: Boolean = true,
    val hasAnyHabit: Boolean = false,
    val period: ReportPeriod = ReportPeriod.WEEK,

    // Week view
    val weekLabel: String = "",
    val isCurrentWeek: Boolean = true,
    val canGoNextWeek: Boolean = false,
    val bars: List<DayBar> = emptyList(),
    val weekAveragePercent: Int? = null,
    val weeklyHabitTotal: Int = 0,
    val weeklyHabitDone: Int = 0,

    // Month view
    val monthLabel: String = "",
    val isCurrentMonth: Boolean = true,
    val canGoNextMonth: Boolean = false,
    val monthCells: List<MonthReportCell> = emptyList(),
    val monthAveragePercent: Int? = null,
    val monthWeeklyHabitTotal: Int = 0,
    val monthWeeklyHabitDone: Int = 0,
)

private data class DayStats(
    val completedRatio: Float,
    val hasData: Boolean,
    val scheduledCount: Int,
)

class WeeklyReportViewModel(
    habitRepository: HabitRepository,
    checkInRepository: CheckInRepository,
) : ViewModel() {

    private val period = MutableStateFlow(ReportPeriod.WEEK)
    private val weekOffset = MutableStateFlow(0)
    private val monthOffset = MutableStateFlow(0)

    val uiState: StateFlow<WeeklyReportUiState> = combine(
        habitRepository.observeActiveHabits(),
        checkInRepository.observeAll(),
        period,
        weekOffset,
        monthOffset,
    ) { habits, checkIns, currentPeriod, weekOff, monthOff ->
        buildState(habits, checkIns, currentPeriod, weekOff, monthOff)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyReportUiState())

    fun selectPeriod(newPeriod: ReportPeriod) {
        period.value = newPeriod
    }

    fun previousWeek() { weekOffset.value -= 1 }
    fun nextWeek() { if (weekOffset.value < 0) weekOffset.value += 1 }

    fun previousMonth() { monthOffset.value -= 1 }
    fun nextMonth() { if (monthOffset.value < 0) monthOffset.value += 1 }

    private fun buildState(
        habits: List<HabitEntity>,
        checkIns: List<CheckInEntity>,
        period: ReportPeriod,
        weekOffset: Int,
        monthOffset: Int,
    ): WeeklyReportUiState {
        val today = LocalDate.now()
        val byHabitDay = checkIns.associateBy { it.habitId to it.epochDay }
        val dailyLike = habits.filter { it.frequency != HabitFrequency.WEEKLY }
        val weeklyHabits = habits.filter { it.frequency == HabitFrequency.WEEKLY }

        fun dayStats(date: LocalDate): DayStats {
            val isFuture = date.isAfter(today)
            val scheduled = dailyLike.filter {
                HabitSchedule.isActiveOn(it.createdAt, date) &&
                    HabitSchedule.isScheduledOn(it.frequency, it.customDays, date)
            }
            var completed = 0
            scheduled.forEach { habit ->
                val entry = byHabitDay[habit.id to date.toEpochDay()]
                if (entry?.completed == true) completed++
            }
            val n = scheduled.size
            return DayStats(
                completedRatio = if (n == 0) 0f else completed.toFloat() / n,
                hasData = !isFuture && n > 0,
                scheduledCount = n,
            )
        }

        fun weeklyHabitDoneInWeek(habit: HabitEntity, weekStart: LocalDate): Boolean =
            (0..6).any { off ->
                byHabitDay[habit.id to weekStart.plusDays(off.toLong()).toEpochDay()]?.completed == true
            }

        // ---- Week view ----
        val monday = today.with(DayOfWeek.MONDAY).plusWeeks(weekOffset.toLong())
        val bars = (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            val stats = dayStats(date)
            DayBar(
                label = HabitSchedule.DAY_LABELS[offset],
                completedRatio = stats.completedRatio,
                scheduledCount = stats.scheduledCount,
                isToday = date == today,
                isFuture = date.isAfter(today),
            )
        }
        val weekWithData = bars.filter { it.hasData }
        val dayFmt = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

        // ---- Month view ----
        val viewMonth = YearMonth.from(today).plusMonths(monthOffset.toLong())
        val firstOfMonth = viewMonth.atDay(1)
        val leadingBlanks = firstOfMonth.dayOfWeek.value - 1 // Monday=0
        val monthCells = buildList {
            repeat(leadingBlanks) { add(MonthReportCell(null, 0f, false, false)) }
            for (day in 1..viewMonth.lengthOfMonth()) {
                val date = viewMonth.atDay(day)
                val stats = dayStats(date)
                add(MonthReportCell(date, stats.completedRatio, stats.hasData, date == today))
            }
        }
        val monthWithData = monthCells.filter { it.hasData }

        // Weekly-habit completion for the month: a Monday-start week counts toward the month
        // that contains its Monday, and only once that week has actually started.
        val monthWeekStarts = buildList {
            var cursor = firstOfMonth.with(DayOfWeek.MONDAY)
            val monthEnd = viewMonth.atEndOfMonth()
            while (!cursor.isAfter(monthEnd)) {
                if (YearMonth.from(cursor) == viewMonth && !cursor.isAfter(today)) add(cursor)
                cursor = cursor.plusWeeks(1)
            }
        }
        val monthWeeklyHabitTotal = weeklyHabits.size * monthWeekStarts.size
        val monthWeeklyHabitDone = weeklyHabits.sumOf { habit ->
            monthWeekStarts.count { weekStart -> weeklyHabitDoneInWeek(habit, weekStart) }
        }

        return WeeklyReportUiState(
            isLoading = false,
            hasAnyHabit = habits.isNotEmpty(),
            period = period,

            weekLabel = "${monday.format(dayFmt)} – ${monday.plusDays(6).format(dayFmt)}",
            isCurrentWeek = weekOffset == 0,
            canGoNextWeek = weekOffset < 0,
            bars = bars,
            weekAveragePercent = weekWithData.takeIf { it.isNotEmpty() }
                ?.let { (it.map { b -> b.completedRatio }.average() * 100).roundToInt() },
            weeklyHabitTotal = weeklyHabits.size,
            weeklyHabitDone = weeklyHabits.count { habit -> weeklyHabitDoneInWeek(habit, monday) },

            monthLabel = viewMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)),
            isCurrentMonth = monthOffset == 0,
            canGoNextMonth = monthOffset < 0,
            monthCells = monthCells,
            monthAveragePercent = monthWithData.takeIf { it.isNotEmpty() }
                ?.let { (it.map { c -> c.completedRatio }.average() * 100).roundToInt() },
            monthWeeklyHabitTotal = monthWeeklyHabitTotal,
            monthWeeklyHabitDone = monthWeeklyHabitDone,
        )
    }

    companion object {
        fun factory(app: HaveItApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WeeklyReportViewModel(app.container.habitRepository, app.container.checkInRepository)
            }
        }
    }
}
