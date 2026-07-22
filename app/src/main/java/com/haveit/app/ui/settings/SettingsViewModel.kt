package com.haveit.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.haveit.app.HaveItApplication
import com.haveit.app.data.backup.BackupManager
import com.haveit.app.data.settings.AppTheme
import com.haveit.app.data.settings.UserSettings
import com.haveit.app.data.settings.UserSettingsRepository
import com.haveit.app.notification.ReminderScheduler
import com.haveit.app.widget.HabitWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val app: HaveItApplication,
    private val settingsRepository: UserSettingsRepository,
) : AndroidViewModel(application) {

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .map<UserSettings, UserSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    /** [uri] is null when the user picks "기본 알람음" — the device's own default alarm sound. */
    fun setAlarmSoundUri(uri: Uri?) {
        viewModelScope.launch { settingsRepository.setAlarmSoundUri(uri?.toString()) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            val scheduler = ReminderScheduler(getApplication())
            val habits = app.container.habitRepository.getAll().filter { it.archivedAt == null }
            if (enabled) {
                habits.forEach { scheduler.reschedule(it) }
            } else {
                habits.forEach { scheduler.cancel(it.id) }
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _message.value = runCatching {
                BackupManager(app).writeBackup(getApplication(), uri)
                "백업을 내보냈어요"
            }.getOrElse { "내보내기 실패: ${it.message}" }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _message.value = runCatching {
                BackupManager(app).readBackup(getApplication(), uri)
                // Reminders and the widget reflect the imported data.
                val scheduler = ReminderScheduler(getApplication())
                app.container.habitRepository.getAll()
                    .filter { it.archivedAt == null }
                    .forEach { scheduler.reschedule(it) }
                HabitWidget.refresh(getApplication())
                "백업을 가져왔어요"
            }.getOrElse { "가져오기 실패: ${it.message}" }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            _message.value = runCatching {
                val scheduler = ReminderScheduler(getApplication())
                app.container.habitRepository.getAll().forEach { scheduler.cancel(it.id) }
                app.container.checkInRepository.clear()
                app.container.routineRepository.clear()
                app.container.habitRepository.clear()
                HabitWidget.refresh(getApplication())
                "모든 데이터를 초기화했어요"
            }.getOrElse { "초기화 실패: ${it.message}" }
        }
    }

    companion object {
        fun factory(app: HaveItApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(app, app, app.container.userSettingsRepository)
            }
        }
    }
}
