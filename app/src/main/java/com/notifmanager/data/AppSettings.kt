package com.notifmanager.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class AppSettings(private val context: Context) {
    val dynamicColorEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_ENABLED] ?: true
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        ThemeMode.entries.getOrElse(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.ordinal) { ThemeMode.SYSTEM }
    }

    val showSystemApps: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SHOW_SYSTEM_APPS] ?: false
    }

    val historyRetentionDays: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[HISTORY_RETENTION_DAYS] ?: DEFAULT_HISTORY_RETENTION_DAYS
    }

    val onboardingCompleted: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    val pauseBatching: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[PAUSE_BATCHING] ?: false
    }

    val temporaryOpenUntilMillis: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[TEMPORARY_OPEN_UNTIL_MILLIS] ?: 0L
    }

    val setupDismissedOnce: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SETUP_DISMISSED_ONCE] ?: false
    }

    val nonBatchableDefaultsNormalized: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[NON_BATCHABLE_DEFAULTS_NORMALIZED] ?: false
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.ordinal
        }
    }

    suspend fun setShowSystemApps(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SHOW_SYSTEM_APPS] = enabled
        }
    }

    suspend fun setHistoryRetentionDays(days: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[HISTORY_RETENTION_DAYS] = days
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setPauseBatching(paused: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[PAUSE_BATCHING] = paused
        }
    }

    suspend fun setTemporaryOpenUntilMillis(untilMillis: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[TEMPORARY_OPEN_UNTIL_MILLIS] = untilMillis.coerceAtLeast(0L)
            prefs[PAUSE_BATCHING] = false
        }
    }

    suspend fun setSetupDismissedOnce(dismissed: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SETUP_DISMISSED_ONCE] = dismissed
        }
    }

    suspend fun setNonBatchableDefaultsNormalized(normalized: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[NON_BATCHABLE_DEFAULTS_NORMALIZED] = normalized
        }
    }

    companion object {
        const val DEFAULT_HISTORY_RETENTION_DAYS = 30
        const val RETENTION_NEVER = 0

        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PAUSE_BATCHING = booleanPreferencesKey("pause_batching")
        val TEMPORARY_OPEN_UNTIL_MILLIS = longPreferencesKey("temporary_open_until_millis")
        val SETUP_DISMISSED_ONCE = booleanPreferencesKey("setup_dismissed_once")
        val NON_BATCHABLE_DEFAULTS_NORMALIZED = booleanPreferencesKey("non_batchable_defaults_normalized")
    }
}
