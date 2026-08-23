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

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAtEpochMillis ASC")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE archived = 0 AND reminderEnabled = 1")
    suspend fun getActiveReminderHabits(): List<HabitEntity>

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<HabitEntity>)

    @Query("DELETE FROM habits")
    suspend fun clearAll()

    @Query("UPDATE habits SET archived = :archived, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAtEpochMillis: Long)
}

@Dao
interface CheckInEventDao {
    @Query("SELECT * FROM check_in_events WHERE deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis DESC")
    fun observeAllEvents(): Flow<List<CheckInEventEntity>>

    @Query("SELECT * FROM check_in_events WHERE deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis DESC")
    suspend fun getAll(): List<CheckInEventEntity>

    @Query("SELECT * FROM check_in_events ORDER BY occurredAtEpochMillis DESC")
    suspend fun getAllIncludingDeleted(): List<CheckInEventEntity>

    @Upsert
    suspend fun upsert(event: CheckInEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CheckInEventEntity>)

    @Query("DELETE FROM check_in_events")
    suspend fun clearAll()

    @Query("SELECT * FROM check_in_events WHERE habitId = :habitId AND localDate = :localDate AND deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestForDay(habitId: String, localDate: String): CheckInEventEntity?

    @Query("UPDATE check_in_events SET deletedAtEpochMillis = :deletedAtEpochMillis, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id")
    suspend fun softDeleteById(id: String, deletedAtEpochMillis: Long, updatedAtEpochMillis: Long)
}
