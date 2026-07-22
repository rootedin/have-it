package com.haveit.app.ui.addhabit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.data.repository.HabitRepository
import com.haveit.app.notification.ReminderScheduler
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddHabitFormState(
    val loaded: Boolean = false,
    val name: String = "",
    val emoji: String = "",
    val colorHex: String = "",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val customDays: Set<Int> = emptySet(),
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val reminderSnoozeMinutes: Int = HabitEntity.DEFAULT_SNOOZE_MINUTES,
    val reminderSnoozeMaxCount: Int = HabitEntity.DEFAULT_SNOOZE_MAX_COUNT,
)

class AddHabitViewModel(
    application: Application,
    private val habitRepository: HabitRepository,
    private val editingHabitId: String?,
) : AndroidViewModel(application) {

    val isEditing: Boolean = editingHabitId != null

    // Preserved across an edit so we don't clobber creation time / archive state.
    private var loadedCreatedAt: Long = System.currentTimeMillis()
    private var loadedArchivedAt: Long? = null

    private val _prefill = MutableStateFlow(AddHabitFormState())
    val prefill: StateFlow<AddHabitFormState> = _prefill.asStateFlow()

    init {
        if (editingHabitId != null) {
            viewModelScope.launch {
                habitRepository.getById(editingHabitId)?.let { h ->
                    loadedCreatedAt = h.createdAt
                    loadedArchivedAt = h.archivedAt
                    _prefill.value = AddHabitFormState(
                        loaded = true,
                        name = h.name,
                        emoji = h.icon,
                        colorHex = h.color,
                        frequency = h.frequency,
                        customDays = h.customDays.orEmpty().toSet(),
                        reminderHour = h.reminderHour,
                        reminderMinute = h.reminderMinute,
                        reminderSnoozeMinutes = h.reminderSnoozeMinutes,
                        reminderSnoozeMaxCount = h.reminderSnoozeMaxCount,
                    )
                }
            }
        } else {
            _prefill.value = AddHabitFormState(loaded = true)
        }
    }

    fun save(
        name: String,
        emoji: String,
        colorHex: String,
        frequency: HabitFrequency,
        customDays: List<Int>,
        reminderHour: Int?,
        reminderMinute: Int?,
        reminderSnoozeMinutes: Int,
        reminderSnoozeMaxCount: Int,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val habit = HabitEntity(
                id = editingHabitId ?: UUID.randomUUID().toString(),
                name = name.trim(),
                icon = emoji,
                color = colorHex,
                frequency = frequency,
                customDays = customDays.sorted().takeIf { frequency == HabitFrequency.CUSTOM_DAYS },
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                reminderSnoozeMinutes = reminderSnoozeMinutes,
                reminderSnoozeMaxCount = reminderSnoozeMaxCount,
                createdAt = loadedCreatedAt,
                archivedAt = loadedArchivedAt,
            )
            // Editing must NOT use REPLACE: it deletes+reinserts the row, which cascades
            // to delete all of this habit's check-ins. @Update matches by PK without deleting.
            if (editingHabitId != null) {
                habitRepository.update(habit)
            } else {
                habitRepository.upsert(habit)
            }
            ReminderScheduler(getApplication()).reschedule(habit)
            onSaved()
        }
    }

    companion object {
        fun factory(app: HaveItApplication, editingHabitId: String?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AddHabitViewModel(app, app.container.habitRepository, editingHabitId)
                }
            }
    }
}
