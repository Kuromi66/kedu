package com.pulse.checkin.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pulse.checkin.domain.model.AppLanguage
import com.pulse.checkin.domain.model.SortMode
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.domain.model.UserPreferences
import com.pulse.checkin.data.update.UpdateStateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pulse_preferences")

class AppPreferences(private val context: Context) : UpdateStateStore {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val appLanguage = stringPreferencesKey("app_language")
        val sortMode = stringPreferencesKey("sort_mode")
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
        val notificationPromptSeen = booleanPreferencesKey("notification_prompt_seen")
        val updateLastCheckDate = stringPreferencesKey("update_last_check_date")
        val updateNotifiedVersionCode = intPreferencesKey("update_notified_version_code")
    }

    override suspend fun lastUpdateCheckDate(): String? =
        context.dataStore.data.first()[Keys.updateLastCheckDate]

    override suspend fun lastNotifiedVersionCode(): Int =
        context.dataStore.data.first()[Keys.updateNotifiedVersionCode] ?: 0

    override suspend fun markUpdateChecked(date: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.updateLastCheckDate] = date
        }
    }

    override suspend fun markNotified(versionCode: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.updateNotifiedVersionCode] = versionCode
        }
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            themeMode = preferences[Keys.themeMode]
                ?.let { ThemeMode.valueOf(it) }
                ?: ThemeMode.LIGHT,
            appLanguage = preferences[Keys.appLanguage]
                ?.let { AppLanguage.valueOf(it) }
                ?: AppLanguage.ZH,
            sortMode = preferences[Keys.sortMode]
                ?.let { SortMode.valueOf(it) }
                ?: SortMode.MANUAL,
            onboardingSeen = preferences[Keys.onboardingSeen] ?: false,
            notificationPromptSeen = preferences[Keys.notificationPromptSeen] ?: false,
        )
    }

    suspend fun currentPreferences(): UserPreferences = userPreferences.first()

    suspend fun replaceAll(userPreferences: UserPreferences) {
        context.dataStore.edit { preferences ->
            preferences[Keys.themeMode] = userPreferences.themeMode.name
            preferences[Keys.appLanguage] = userPreferences.appLanguage.name
            preferences[Keys.sortMode] = userPreferences.sortMode.name
            preferences[Keys.onboardingSeen] = userPreferences.onboardingSeen
            preferences[Keys.notificationPromptSeen] = userPreferences.notificationPromptSeen
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.themeMode] = themeMode.name
        }
    }

    suspend fun setAppLanguage(appLanguage: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[Keys.appLanguage] = appLanguage.name
        }
    }
}
