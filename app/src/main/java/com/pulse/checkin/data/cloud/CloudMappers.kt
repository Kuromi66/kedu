package com.pulse.checkin.data.cloud

import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity
import java.time.LocalDate

fun HabitEntity.toDto(): HabitDto = HabitDto(
    id = id,
    name = name,
    colorArgb = colorArgb,
    glyph = glyph,
    sortOrder = sortOrder,
    reminderEnabled = reminderEnabled,
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    targetEnabled = targetEnabled,
    dailyTargetCount = dailyTargetCount,
    createdAtEpochMillis = createdAtEpochMillis,
    archived = archived,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun HabitDto.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    colorArgb = colorArgb,
    glyph = glyph,
    sortOrder = sortOrder,
    reminderEnabled = reminderEnabled,
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    targetEnabled = targetEnabled,
    dailyTargetCount = dailyTargetCount,
    createdAtEpochMillis = createdAtEpochMillis,
    archived = archived,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun CheckInEventEntity.toDto(): EventDto = EventDto(
    id = id,
    habitId = habitId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    localDate = localDate,
    isBackfilled = isBackfilled,
    deletedAtEpochMillis = deletedAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    note = note,
)

fun EventDto.toEntity(): CheckInEventEntity = CheckInEventEntity(
    id = id,
    habitId = habitId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    localDate = localDate,
    isBackfilled = isBackfilled,
    deletedAtEpochMillis = deletedAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    note = note,
)

@Suppress("unused")
fun EventDto.toLocalDate(): LocalDate = LocalDate.parse(localDate)
