package com.pulse.checkin.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
)

@Entity(tableName = "check_in_events")
data class CheckInEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val occurredAtEpochMillis: Long,
    val localDate: String,
)
