package com.pulse.checkin.data.cloud

import com.pulse.checkin.data.db.dao.CheckInEventDao
import com.pulse.checkin.data.db.dao.DayEventDao
import com.pulse.checkin.data.db.dao.HabitDao
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.DayEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity

interface SyncDataSource {
    suspend fun getAllHabits(): List<HabitEntity>
    suspend fun upsertHabits(habits: List<HabitEntity>)
    suspend fun getAllEventsIncludingDeleted(): List<CheckInEventEntity>
    suspend fun upsertEvents(events: List<CheckInEventEntity>)
    suspend fun getAllDayEvents(): List<DayEventEntity>
    suspend fun upsertDayEvents(events: List<DayEventEntity>)
    suspend fun clearAll()
}

class RoomSyncDataSource(
    private val habitDao: HabitDao,
    private val eventDao: CheckInEventDao,
    private val dayEventDao: DayEventDao,
) : SyncDataSource {
    override suspend fun getAllHabits(): List<HabitEntity> = habitDao.getAll()

    override suspend fun upsertHabits(habits: List<HabitEntity>) {
        if (habits.isNotEmpty()) {
            habitDao.insertAll(habits)
        }
    }

    override suspend fun getAllEventsIncludingDeleted(): List<CheckInEventEntity> = eventDao.getAllIncludingDeleted()

    override suspend fun upsertEvents(events: List<CheckInEventEntity>) {
        if (events.isNotEmpty()) {
            eventDao.insertAll(events)
        }
    }

    override suspend fun getAllDayEvents(): List<DayEventEntity> = dayEventDao.getAll()

    override suspend fun upsertDayEvents(events: List<DayEventEntity>) {
        if (events.isNotEmpty()) {
            dayEventDao.insertAll(events)
        }
    }

    override suspend fun clearAll() {
        habitDao.clearAll()
        eventDao.clearAll()
        dayEventDao.clearAll()
    }
}
