package com.haveit.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

enum class AppTheme { LIGHT, DARK, SYSTEM }

data class UserSettings(
    val notificationsEnabled: Boolean,
    val theme: AppTheme,
    val alarmSoundUri: String?,
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val THEME = stringPreferencesKey("theme")
        val ALARM_SOUND_URI = stringPreferencesKey("alarm_sound_uri")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            theme = prefs[Keys.THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.SYSTEM,
            // Null means "use the device's default alarm sound".
            alarmSoundUri = prefs[Keys.ALARM_SOUND_URI],
        )
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setAlarmSoundUri(uri: String?) {
        context.dataStore.edit {
            if (uri != null) it[Keys.ALARM_SOUND_URI] = uri else it.remove(Keys.ALARM_SOUND_URI)
        }
    }
}
