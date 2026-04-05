package com.pulse.checkin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder ASC, createdAtEpochMillis ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE archived = 0 AND reminderEnabled = 1")
    suspend fun getActiveReminderHabits(): List<HabitEntity>

    @Upsert
    suspend fun upsert(habit: HabitEntity): Long

    @Query("UPDATE habits SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}

@Dao
interface CheckInEventDao {
    @Query("SELECT * FROM check_in_events ORDER BY occurredAtEpochMillis DESC")
    fun observeAllEvents(): Flow<List<CheckInEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CheckInEventEntity): Long

    @Query("SELECT * FROM check_in_events WHERE habitId = :habitId AND localDate = :localDate ORDER BY occurredAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestForDay(habitId: Long, localDate: String): CheckInEventEntity?

    @Query("DELETE FROM check_in_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
