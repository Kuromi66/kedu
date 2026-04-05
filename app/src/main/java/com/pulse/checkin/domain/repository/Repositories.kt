package com.pulse.checkin.domain.repository

import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.Habit
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    suspend fun getHabit(id: Long): Habit?
    suspend fun getActiveReminderHabits(): List<Habit>
    suspend fun upsert(habit: Habit): Long
    suspend fun setArchived(id: Long, archived: Boolean)
}

interface CheckInRepository {
    fun observeAllEvents(): Flow<List<CheckInEvent>>
    suspend fun addCheckIn(habitId: Long, occurredAtEpochMillis: Long = System.currentTimeMillis())
    suspend fun removeLatestForDay(habitId: Long, date: LocalDate)
}
