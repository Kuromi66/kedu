package com.pulse.checkin.domain.repository

import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.DayEvent
import com.pulse.checkin.domain.model.Habit
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    suspend fun getHabit(id: String): Habit?
    suspend fun getActiveReminderHabits(): List<Habit>
    suspend fun upsert(habit: Habit)
    suspend fun setArchived(id: String, archived: Boolean)
}

interface CheckInRepository {
    fun observeAllEvents(): Flow<List<CheckInEvent>>
    suspend fun addCheckIn(
        habitId: String,
        occurredAtEpochMillis: Long = System.currentTimeMillis(),
        localDate: LocalDate? = null,
        isBackfilled: Boolean = false,
        note: String? = null,
    )
    suspend fun removeLatestForDay(habitId: String, date: LocalDate)
    suspend fun deleteCheckIn(eventId: String)
}

interface DayEventRepository {
    fun observeDayEvents(): Flow<List<DayEvent>>
    suspend fun getDayEvent(id: String): DayEvent?
    suspend fun upsert(event: DayEvent)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun reorderDayEvents(orderedIds: List<String>)
}
