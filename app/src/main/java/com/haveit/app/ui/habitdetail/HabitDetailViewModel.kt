package com.haveit.app.ui.habitdetail

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
import com.haveit.app.data.repository.CheckInRepository
import com.haveit.app.data.repository.HabitRepository
import com.haveit.app.data.settings.UserSettings
import com.haveit.app.data.settings.UserSettingsRepository
import com.haveit.app.domain.schedule.HabitSchedule
import com.haveit.app.domain.streak.StreakCalculator
import com.haveit.app.notification.ReminderScheduler
import com.haveit.app.widget.HabitWidget
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DayMark { DONE, FROZEN, MISSED, PENDING, OFF }

data class RecentDay(val date: LocalDate, val mark: DayMark)

/** One cell in the month heatmap; [date] is null for leading blanks that pad the first week. */
data class HeatCell(val date: LocalDate?, val mark: DayMark, val isToday: Boolean)

data class NoteEntry(val date: LocalDate, val note: String)

data class DetailUiState(
    val isLoading: Boolean = true,
    val habit: HabitEntity? = null,
    val streak: Int = 0,
    val completionLabel: String = "",
    val completionText: String = "",
    val recentDays: List<RecentDay> = emptyList(),
    val freezeCandidate: LocalDate? = null,
    val freezeCardsAvailable: Int = 0,
    val monthLabel: String = "",
    val monthCells: List<HeatCell> = emptyList(),
    val canGoNextMonth: Boolean = false,
    val todayNote: String = "",
    val pastNotes: List<NoteEntry> = emptyList(),
)

class HabitDetailViewModel(
    application: Application,
    private val habitId: String,
    private val habitRepository: HabitRepository,
    private val checkInRepository: CheckInRepository,
    private val settingsRepository: UserSettingsRepository,
) : AndroidViewModel(application) {

    private val monthOffset = MutableStateFlow(0)

    val uiState: StateFlow<DetailUiState> = combine(
        habitRepository.observeById(habitId),
        checkInRepository.observeForHabit(habitId),
        settingsRepository.settings,
        monthOffset,
    ) { habit, checkIns, settings, offset -> buildState(habit, checkIns, settings, offset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun previousMonth() { monthOffset.value -= 1 }
    fun nextMonth() { if (monthOffset.value < 0) monthOffset.value += 1 }

    private fun buildState(
        habit: HabitEntity?,
        checkIns: List<CheckInEntity>,
        settings: UserSettings,
        offset: Int,
    ): DetailUiState {
        if (habit == null) return DetailUiState(isLoading = false)

        val today = LocalDate.now()
        val byDay = checkIns.associateBy { it.epochDay }
        fun markFor(date: LocalDate): DayMark {
            val entry = byDay[date.toEpochDay()]
            return when {
                entry?.completed == true -> DayMark.DONE
                entry?.usedFreezeCard == true -> DayMark.FROZEN
                date.isAfter(today) -> DayMark.OFF
                !HabitSchedule.isActiveOn(habit.createdAt, date) -> DayMark.OFF
                habit.frequency == HabitFrequency.WEEKLY ->
                    if (date == today) DayMark.PENDING else DayMark.OFF
                !HabitSchedule.isScheduledOn(habit.frequency, habit.customDays, date) -> DayMark.OFF
                date == today -> DayMark.PENDING
                else -> DayMark.MISSED
            }
        }
        fun doneOn(date: LocalDate) =
            byDay[date.toEpochDay()]?.let { it.completed || it.usedFreezeCard } == true

        val recentDays = (13 downTo 0).map { offsetDays ->
            RecentDay(today.minusDays(offsetDays.toLong()), markFor(today.minusDays(offsetDays.toLong())))
        }

        // Month heatmap
        val viewMonth = YearMonth.from(today).plusMonths(offset.toLong())
        val firstOfMonth = viewMonth.atDay(1)
        val leadingBlanks = (firstOfMonth.dayOfWeek.value - 1) // Monday=0
        val cells = buildList {
            repeat(leadingBlanks) { add(HeatCell(null, DayMark.OFF, false)) }
            for (day in 1..viewMonth.lengthOfMonth()) {
                val date = viewMonth.atDay(day)
                add(HeatCell(date, markFor(date), date == today))
            }
        }

        val streak = StreakCalculator.currentStreak(habit.frequency, habit.customDays, checkIns, today)

        val (completionLabel, completionText) = if (habit.frequency == HabitFrequency.WEEKLY) {
            val monday = today.with(DayOfWeek.MONDAY)
            val doneWeeks = (0..3).count { weeksBack ->
                val weekStart = monday.minusWeeks(weeksBack.toLong())
                (0..6).any { doneOn(weekStart.plusDays(it.toLong())) }
            }
            "최근 4주" to "${doneWeeks}/4주"
        } else {
            var scheduled = 0
            var done = 0
            for (i in 0..29) {
                val date = today.minusDays(i.toLong())
                if (HabitSchedule.isActiveOn(habit.createdAt, date) &&
                    HabitSchedule.isScheduledOn(habit.frequency, habit.customDays, date)
                ) {
                    scheduled++
                    if (doneOn(date)) done++
                }
            }
            "최근 30일 완료율" to if (scheduled == 0) "-" else "${done * 100 / scheduled}%"
        }

        val freezeCandidate = if (habit.frequency == HabitFrequency.WEEKLY) {
            null
        } else {
            (1..7).map { today.minusDays(it.toLong()) }
                .firstOrNull {
                    HabitSchedule.isActiveOn(habit.createdAt, it) &&
                        HabitSchedule.isScheduledOn(habit.frequency, habit.customDays, it)
                }
                ?.takeIf { !doneOn(it) }
        }

        val noteFmt = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
        val todayNote = byDay[today.toEpochDay()]?.note.orEmpty()
        val pastNotes = checkIns
            .filter { !it.note.isNullOrBlank() && it.epochDay != today.toEpochDay() }
            .sortedByDescending { it.epochDay }
            .take(5)
            .map { NoteEntry(LocalDate.ofEpochDay(it.epochDay), it.note!!) }

        return DetailUiState(
            isLoading = false,
            habit = habit,
            streak = streak,
            completionLabel = completionLabel,
            completionText = completionText,
            recentDays = recentDays,
            freezeCandidate = freezeCandidate,
            freezeCardsAvailable = settings.freezeCardsAvailable,
            monthLabel = viewMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)),
            monthCells = cells,
            canGoNextMonth = offset < 0,
            todayNote = todayNote,
            pastNotes = pastNotes,
        )
    }

    fun useFreezeCard(date: LocalDate) {
        viewModelScope.launch {
            if (settingsRepository.tryUseFreezeCard()) {
                val existing = checkInRepository.getForHabitOnDay(habitId, date.toEpochDay())
                checkInRepository.upsert(
                    CheckInEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        habitId = habitId,
                        epochDay = date.toEpochDay(),
                        completed = false,
                        usedFreezeCard = true,
                        note = existing?.note,
                    ),
                )
                HabitWidget.refresh(getApplication())
            }
        }
    }

    fun saveTodayNote(text: String) {
        viewModelScope.launch {
            val epochDay = LocalDate.now().toEpochDay()
            val existing = checkInRepository.getForHabitOnDay(habitId, epochDay)
            val trimmed = text.trim().ifBlank { null }
            if (existing == null && trimmed == null) return@launch
            checkInRepository.upsert(
                CheckInEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    habitId = habitId,
                    epochDay = epochDay,
                    completed = existing?.completed ?: false,
                    usedFreezeCard = existing?.usedFreezeCard ?: false,
                    note = trimmed,
                ),
            )
        }
    }

    fun archive(onDone: () -> Unit) {
        viewModelScope.launch {
            uiState.value.habit?.let {
                habitRepository.update(it.copy(archivedAt = System.currentTimeMillis()))
                ReminderScheduler(getApplication()).cancel(it.id)
                HabitWidget.refresh(getApplication())
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            uiState.value.habit?.let {
                habitRepository.delete(it)
                ReminderScheduler(getApplication()).cancel(it.id)
                HabitWidget.refresh(getApplication())
            }
            onDone()
        }
    }

    companion object {
        fun factory(app: HaveItApplication, habitId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    HabitDetailViewModel(
                        app,
                        habitId,
                        app.container.habitRepository,
                        app.container.checkInRepository,
                        app.container.userSettingsRepository,
                    )
                }
            }
    }
}
