package com.pulse.checkin.data.repository

import com.pulse.checkin.data.db.dao.CheckInEventDao
import com.pulse.checkin.data.db.dao.DayEventDao
import com.pulse.checkin.data.db.dao.HabitDao
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.DayEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity
import com.pulse.checkin.data.cloud.SyncClock
import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.DayEvent
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.repository.CheckInRepository
import com.pulse.checkin.domain.repository.DayEventRepository
import com.pulse.checkin.domain.repository.HabitRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val clock: SyncClock,
) : HabitRepository {
    override fun observeHabits(): Flow<List<Habit>> {
        return habitDao.observeActiveHabits().map { habits -> habits.map(HabitEntity::toDomain) }
    }

    override suspend fun getHabit(id: String): Habit? = habitDao.getById(id)?.toDomain()

    override suspend fun getActiveReminderHabits(): List<Habit> {
        return habitDao.getActiveReminderHabits().map(HabitEntity::toDomain)
    }

    override suspend fun upsert(habit: Habit) {
        habitDao.upsert(habit.toEntity())
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        habitDao.setArchived(id, archived, updatedAtEpochMillis = clock.nowMillis())
    }
}

class CheckInRepositoryImpl(
    private val eventDao: CheckInEventDao,
    private val clock: SyncClock,
) : CheckInRepository {
    override fun observeAllEvents(): Flow<List<CheckInEvent>> {
        return eventDao.observeAllEvents().map { events -> events.map(CheckInEventEntity::toDomain) }
    }

    override suspend fun addCheckIn(
        habitId: String,
        occurredAtEpochMillis: Long,
        localDate: LocalDate?,
        isBackfilled: Boolean,
        note: String?,
    ) {
        val date = localDate ?: Instant.ofEpochMilli(occurredAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        eventDao.upsert(
            CheckInEventEntity(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                occurredAtEpochMillis = occurredAtEpochMillis,
                localDate = date.toString(),
                isBackfilled = isBackfilled,
                updatedAtEpochMillis = clock.nowMillis(),
                note = note,
            ),
        )
    }

    override suspend fun removeLatestForDay(habitId: String, date: LocalDate) {
        val latest = eventDao.getLatestForDay(habitId = habitId, localDate = date.toString()) ?: return
        val now = clock.nowMillis()
        eventDao.softDeleteById(
            id = latest.id,
            deletedAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
    }

    override suspend fun deleteCheckIn(eventId: String) {
        val now = clock.nowMillis()
        eventDao.softDeleteById(
            id = eventId,
            deletedAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
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
    updatedAtEpochMillis = updatedAtEpochMillis,
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
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun CheckInEventEntity.toDomain(): CheckInEvent = CheckInEvent(
    id = id,
    habitId = habitId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    localDate = LocalDate.parse(localDate),
    isBackfilled = isBackfilled,
    deletedAtEpochMillis = deletedAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    note = note,
)

class DayEventRepositoryImpl(
    private val dayEventDao: DayEventDao,
    private val clock: SyncClock,
) : DayEventRepository {
    override fun observeDayEvents(): Flow<List<DayEvent>> {
        return dayEventDao.observeActive().map { events -> events.map(DayEventEntity::toDomain) }
    }

    override suspend fun getDayEvent(id: String): DayEvent? = dayEventDao.getById(id)?.toDomain()

    override suspend fun upsert(event: DayEvent) {
        dayEventDao.upsert(event.toEntity())
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        dayEventDao.setArchived(id, archived, updatedAtEpochMillis = clock.nowMillis())
    }

    override suspend fun reorderDayEvents(orderedIds: List<String>) {
        val now = clock.nowMillis()
        orderedIds.forEachIndexed { index, id ->
            dayEventDao.setSortOrder(id, index, now)
        }
    }
}

private fun DayEventEntity.toDomain(): DayEvent = DayEvent(
    id = id,
    name = name,
    date = LocalDate.parse(eventDate),
    repeatsYearly = repeatsYearly,
    note = note,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAtEpochMillis,
    archived = archived,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun DayEvent.toEntity(): DayEventEntity = DayEventEntity(
    id = id,
    name = name,
    eventDate = date.toString(),
    repeatsYearly = repeatsYearly,
    note = note,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAtEpochMillis,
    archived = archived,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
