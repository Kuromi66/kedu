package com.pulse.checkin.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class ThemeMode {
    LIGHT,
    SYSTEM,
    DARK,
}

enum class SortMode {
    MANUAL,
}

enum class AppLanguage {
    ZH,
    EN,
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val appLanguage: AppLanguage = AppLanguage.ZH,
    val sortMode: SortMode = SortMode.MANUAL,
    val onboardingSeen: Boolean = false,
    val notificationPromptSeen: Boolean = false,
)

data class Habit(
    val id: String = "",
    val name: String,
    val colorArgb: Long,
    val glyph: String,
    val sortOrder: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val targetEnabled: Boolean = false,
    val dailyTargetCount: Int? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
) {
    fun reminderTimeOrNull(): LocalTime? {
        if (!reminderEnabled || reminderHour == null || reminderMinute == null) return null
        return LocalTime.of(reminderHour, reminderMinute)
    }

    fun targetCountOrDefault(): Int? = if (targetEnabled) (dailyTargetCount ?: 1).coerceAtLeast(1) else null
}

data class CheckInEvent(
    val id: String = "",
    val habitId: String,
    val occurredAtEpochMillis: Long,
    val localDate: LocalDate,
    val isBackfilled: Boolean = false,
    val deletedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long = occurredAtEpochMillis,
)
