package com.notifmanager.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

class AppSettings(private val context: Context) {
    val dynamicColorEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_ENABLED] ?: true
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

    val setupDismissedOnce: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SETUP_DISMISSED_ONCE] ?: false
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[DYNAMIC_COLOR_ENABLED] = enabled
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

    suspend fun setSetupDismissedOnce(dismissed: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SETUP_DISMISSED_ONCE] = dismissed
        }
    }

    companion object {
        const val DEFAULT_HISTORY_RETENTION_DAYS = 30
        const val RETENTION_NEVER = 0

        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PAUSE_BATCHING = booleanPreferencesKey("pause_batching")
        val SETUP_DISMISSED_ONCE = booleanPreferencesKey("setup_dismissed_once")
    }
}
