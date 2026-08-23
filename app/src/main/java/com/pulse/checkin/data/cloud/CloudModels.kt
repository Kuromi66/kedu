package com.pulse.checkin.data.cloud

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val serverTime: Long,
)

@Serializable
data class HabitDto(
    val id: String,
    val name: String,
    val colorArgb: Long,
    val glyph: String,
    val sortOrder: Int,
    val reminderEnabled: Boolean,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val targetEnabled: Boolean,
    val dailyTargetCount: Int? = null,
    val createdAtEpochMillis: Long,
    val archived: Boolean,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class EventDto(
    val id: String,
    val habitId: String,
    val occurredAtEpochMillis: Long,
    val localDate: String,
    val isBackfilled: Boolean = false,
    val deletedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long,
    val note: String? = null,
)

@Serializable
data class DayEventDto(
    val id: String,
    val name: String,
    val eventDate: String,
    val repeatsYearly: Boolean = false,
    val note: String? = null,
    val createdAtEpochMillis: Long,
    val archived: Boolean = false,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class SyncRequest(
    val since: Long,
    val habits: List<HabitDto>,
    val events: List<EventDto>,
    val dayEvents: List<DayEventDto> = emptyList(),
)

@Serializable
data class SyncResponse(
    val serverTime: Long,
    val habits: List<HabitDto> = emptyList(),
    val events: List<EventDto> = emptyList(),
    val dayEvents: List<DayEventDto> = emptyList(),
)

data class Session(
    val token: String,
    val userId: String,
    val email: String,
)

enum class SyncError {
    INVALID_CREDENTIALS,
    EMAIL_TAKEN,
    INVALID_INPUT,
    NETWORK,
    UNKNOWN,
}

class SyncException(val error: SyncError) : Exception(error.name)

sealed interface SyncOutcome {
    data object SignedOut : SyncOutcome
    data class Success(
        val pulledHabits: Int,
        val pulledEvents: Int,
        val pulledDayEvents: Int = 0,
    ) : SyncOutcome
    data class Failure(val error: SyncError) : SyncOutcome
}
