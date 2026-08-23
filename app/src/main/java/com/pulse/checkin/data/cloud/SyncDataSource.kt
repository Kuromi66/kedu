package com.pulse.checkin.data.cloud

import com.pulse.checkin.data.db.dao.CheckInEventDao
import com.pulse.checkin.data.db.dao.HabitDao
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity

interface SyncDataSource {
    suspend fun getAllHabits(): List<HabitEntity>
    suspend fun upsertHabits(habits: List<HabitEntity>)
    suspend fun getAllEventsIncludingDeleted(): List<CheckInEventEntity>
    suspend fun upsertEvents(events: List<CheckInEventEntity>)
}

class RoomSyncDataSource(
    private val habitDao: HabitDao,
    private val eventDao: CheckInEventDao,
) : SyncDataSource {
    override suspend fun getAllHabits(): List<HabitEntity> = habitDao.getAll()

    override suspend fun upsertHabits(habits: List<HabitEntity>) {
        habits.forEach { habitDao.upsert(it) }
    }

    override suspend fun getAllEventsIncludingDeleted(): List<CheckInEventEntity> = eventDao.getAllIncludingDeleted()

    override suspend fun upsertEvents(events: List<CheckInEventEntity>) {
        events.forEach { eventDao.upsert(it) }
    }
}
