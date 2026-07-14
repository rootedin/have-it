package com.haveit.app.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.repository.HabitRepository
import com.haveit.app.notification.ReminderScheduler
import com.haveit.app.widget.HabitWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchiveViewModel(
    application: Application,
    private val habitRepository: HabitRepository,
) : AndroidViewModel(application) {

    val archived: StateFlow<List<HabitEntity>> = habitRepository.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(habit: HabitEntity) {
        viewModelScope.launch {
            val restored = habit.copy(archivedAt = null)
            habitRepository.update(restored)
            ReminderScheduler(getApplication()).reschedule(restored)
            HabitWidget.refresh(getApplication())
        }
    }

    fun delete(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.delete(habit)
        }
    }

    companion object {
        fun factory(app: HaveItApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer { ArchiveViewModel(app, app.container.habitRepository) }
        }
    }
}
