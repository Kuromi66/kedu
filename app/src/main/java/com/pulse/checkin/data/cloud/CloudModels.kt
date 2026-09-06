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
    val deletedAtEpochMillis: Long? = null,
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
    val repeatsMonthly: Boolean = false,
    val repeatsYearly: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val note: String? = null,
    val sortOrder: Int = 0,
    val calendarType: String = "SOLAR",
    val lunarMonth: Int? = null,
    val lunarDay: Int? = null,
    val lunarLeap: Boolean = false,
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
    val fetchIds: FetchIds? = null,
)

@Serializable
data class SyncResponse(
    val serverTime: Long,
    val habits: List<HabitDto> = emptyList(),
    val events: List<EventDto> = emptyList(),
    val dayEvents: List<DayEventDto> = emptyList(),
)

// 对帐补拉：指定需要从服务端拉取最新正文的 id 集合
@Serializable
data class FetchIds(
    val habits: List<String> = emptyList(),
    val events: List<String> = emptyList(),
    val dayEvents: List<String> = emptyList(),
)

// 对帐摘要：只含 id 与该记录的更新时间，用来与本地做双向版本比对
@Serializable
data class RecordMeta(
    val id: String,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class MetaResponse(
    val serverTime: Long,
    val habits: List<RecordMeta> = emptyList(),
    val events: List<RecordMeta> = emptyList(),
    val dayEvents: List<RecordMeta> = emptyList(),
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
