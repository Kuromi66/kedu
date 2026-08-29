package com.pulse.checkin.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Long,
    val glyph: String,
    val sortOrder: Int,
    val reminderEnabled: Boolean,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val targetEnabled: Boolean,
    val dailyTargetCount: Int?,
    val createdAtEpochMillis: Long,
    val archived: Boolean,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(tableName = "check_in_events")
data class CheckInEventEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val occurredAtEpochMillis: Long,
    val localDate: String,
    val isBackfilled: Boolean = false,
    val deletedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long = occurredAtEpochMillis,
    val note: String? = null,
)

@Entity(tableName = "day_events")
data class DayEventEntity(
    @PrimaryKey val id: String,
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
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)
