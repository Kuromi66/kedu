package com.pulse.checkin.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pulse.checkin.domain.model.SortMode
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pulse_preferences")

class AppPreferences(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val sortMode = stringPreferencesKey("sort_mode")
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
        val notificationPromptSeen = booleanPreferencesKey("notification_prompt_seen")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            themeMode = preferences[Keys.themeMode]
                ?.let { ThemeMode.valueOf(it) }
                ?: ThemeMode.LIGHT,
            sortMode = preferences[Keys.sortMode]
                ?.let { SortMode.valueOf(it) }
                ?: SortMode.MANUAL,
            onboardingSeen = preferences[Keys.onboardingSeen] ?: false,
            notificationPromptSeen = preferences[Keys.notificationPromptSeen] ?: false,
        )
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.themeMode] = themeMode.name
        }
    }
}
