package com.haveit.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

enum class AppTheme { LIGHT, DARK, SYSTEM }

data class UserSettings(
    val notificationsEnabled: Boolean,
    val freezeCardsAvailable: Int,
    val freezeCardsPerMonth: Int,
    val theme: AppTheme,
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val FREEZE_CARDS_AVAILABLE = intPreferencesKey("freeze_cards_available")
        val FREEZE_CARDS_PER_MONTH = intPreferencesKey("freeze_cards_per_month")
        val FREEZE_CARDS_RESET_YEAR_MONTH = stringPreferencesKey("freeze_cards_reset_year_month")
        val THEME = stringPreferencesKey("theme")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val perMonth = prefs[Keys.FREEZE_CARDS_PER_MONTH] ?: DEFAULT_FREEZE_CARDS_PER_MONTH
        // If the calendar month rolled over since the last reset, the effective balance
        // is a full allowance again (the write happens lazily in tryUseFreezeCard).
        val available = if (prefs[Keys.FREEZE_CARDS_RESET_YEAR_MONTH] != currentYearMonth()) {
            perMonth
        } else {
            prefs[Keys.FREEZE_CARDS_AVAILABLE] ?: perMonth
        }
        UserSettings(
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            freezeCardsAvailable = available,
            freezeCardsPerMonth = perMonth,
            theme = prefs[Keys.THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.SYSTEM,
        )
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setFreezeCardsPerMonth(count: Int) {
        context.dataStore.edit { it[Keys.FREEZE_CARDS_PER_MONTH] = count }
    }

    /**
     * Consumes one freeze card, first rolling the monthly allowance over if the calendar
     * month has changed since the last reset. Returns false if none were available.
     */
    suspend fun tryUseFreezeCard(): Boolean {
        var didUse = false
        context.dataStore.edit { prefs ->
            val perMonth = prefs[Keys.FREEZE_CARDS_PER_MONTH] ?: DEFAULT_FREEZE_CARDS_PER_MONTH
            val storedYearMonth = prefs[Keys.FREEZE_CARDS_RESET_YEAR_MONTH]
            val nowYearMonth = currentYearMonth()

            var available = prefs[Keys.FREEZE_CARDS_AVAILABLE] ?: perMonth
            if (storedYearMonth != nowYearMonth) {
                available = perMonth
                prefs[Keys.FREEZE_CARDS_RESET_YEAR_MONTH] = nowYearMonth
            }

            if (available > 0) {
                available -= 1
                didUse = true
            }
            prefs[Keys.FREEZE_CARDS_AVAILABLE] = available
        }
        return didUse
    }

    private fun currentYearMonth(): String = YearMonth.now().toString()

    companion object {
        const val DEFAULT_FREEZE_CARDS_PER_MONTH = 1
    }
}
