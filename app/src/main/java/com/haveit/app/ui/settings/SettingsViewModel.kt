package com.haveit.app.ui.settings

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import com.haveit.app.notification.AlarmSounds
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

    private var previewPlayer: MediaPlayer? = null

    fun consumeMessage() { _message.value = null }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setAlarmSound(key: String) {
        viewModelScope.launch { settingsRepository.setAlarmSoundKey(key) }
        previewAlarmSound(key)
    }

    /** Plays the chosen tone once at alarm volume so the user can hear it before committing. */
    fun previewAlarmSound(key: String) {
        stopPreview()
        val context = getApplication<Application>()
        previewPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setOnCompletionListener { stopPreview() }
            runCatching {
                setDataSource(context, AlarmSounds.uri(context, key))
                setOnPreparedListener { start() }
                prepareAsync()
            }.onFailure { stopPreview() }
        }
    }

    fun stopPreview() {
        previewPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            player.release()
        }
        previewPlayer = null
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
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
