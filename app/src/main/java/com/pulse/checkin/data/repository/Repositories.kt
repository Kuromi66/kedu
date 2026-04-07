package com.pulse.checkin.data.repository

import com.pulse.checkin.data.db.dao.CheckInEventDao
import com.pulse.checkin.data.db.dao.HabitDao
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity
import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.repository.CheckInRepository
import com.pulse.checkin.domain.repository.HabitRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
) : HabitRepository {
    override fun observeHabits(): Flow<List<Habit>> {
        return habitDao.observeActiveHabits().map { habits -> habits.map(HabitEntity::toDomain) }
    }

    override suspend fun getHabit(id: Long): Habit? = habitDao.getById(id)?.toDomain()

    override suspend fun getActiveReminderHabits(): List<Habit> {
        return habitDao.getActiveReminderHabits().map(HabitEntity::toDomain)
    }

    override suspend fun upsert(habit: Habit): Long = habitDao.upsert(habit.toEntity())

    override suspend fun setArchived(id: Long, archived: Boolean) {
        habitDao.setArchived(id, archived)
    }
}

class CheckInRepositoryImpl(
    private val eventDao: CheckInEventDao,
) : CheckInRepository {
    override fun observeAllEvents(): Flow<List<CheckInEvent>> {
        return eventDao.observeAllEvents().map { events -> events.map(CheckInEventEntity::toDomain) }
    }

    override suspend fun addCheckIn(
        habitId: Long,
        occurredAtEpochMillis: Long,
        localDate: LocalDate?,
        isBackfilled: Boolean,
    ) {
        val date = localDate ?: Instant.ofEpochMilli(occurredAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        eventDao.insert(
            CheckInEventEntity(
                habitId = habitId,
                occurredAtEpochMillis = occurredAtEpochMillis,
                localDate = date.toString(),
                isBackfilled = isBackfilled,
            ),
        )
    }

    override suspend fun removeLatestForDay(habitId: Long, date: LocalDate) {
        val latest = eventDao.getLatestForDay(habitId = habitId, localDate = date.toString()) ?: return
        eventDao.deleteById(latest.id)
    }

    override suspend fun deleteCheckIn(eventId: Long) {
        eventDao.deleteById(eventId)
    }
}

private fun HabitEntity.toDomain(): Habit = Habit(
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
)

private fun Habit.toEntity(): HabitEntity = HabitEntity(
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
)

private fun CheckInEventEntity.toDomain(): CheckInEvent = CheckInEvent(
    id = id,
    habitId = habitId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    localDate = LocalDate.parse(localDate),
    isBackfilled = isBackfilled,
)
