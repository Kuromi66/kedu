package com.pulse.checkin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.DayEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 AND deletedAtEpochMillis IS NULL ORDER BY sortOrder ASC, createdAtEpochMillis ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE archived = 1 AND deletedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis ASC")
    fun observeArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAtEpochMillis ASC")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE archived = 0 AND reminderEnabled = 1 AND deletedAtEpochMillis IS NULL")
    suspend fun getActiveReminderHabits(): List<HabitEntity>

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<HabitEntity>)

    @Query("DELETE FROM habits")
    suspend fun clearAll()

    @Query("UPDATE habits SET archived = :archived, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAtEpochMillis: Long)

    @Query("UPDATE habits SET deletedAtEpochMillis = :deletedAtEpochMillis, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id")
    suspend fun setDeleted(id: String, deletedAtEpochMillis: Long, updatedAtEpochMillis: Long)
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

    @Query("UPDATE check_in_events SET occurredAtEpochMillis = :occurredAtEpochMillis, note = :note, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id")
    suspend fun updateRecord(id: String, occurredAtEpochMillis: Long, note: String?, updatedAtEpochMillis: Long)

    @Query("UPDATE check_in_events SET deletedAtEpochMillis = :deletedAtEpochMillis, updatedAtEpochMillis = :updatedAtEpochMillis WHERE habitId = :habitId AND deletedAtEpochMillis IS NULL")
    suspend fun tombstoneByHabit(habitId: String, deletedAtEpochMillis: Long, updatedAtEpochMillis: Long)
}

@Dao
interface DayEventDao {
    @Query("SELECT * FROM day_events WHERE archived = 0 ORDER BY sortOrder ASC, createdAtEpochMillis ASC")
    fun observeActive(): Flow<List<DayEventEntity>>

    @Query("SELECT * FROM day_events WHERE archived = 0 AND reminderEnabled = 1 ORDER BY eventDate ASC")
    suspend fun getActiveReminderDayEvents(): List<DayEventEntity>

    @Query("SELECT * FROM day_events ORDER BY sortOrder ASC, createdAtEpochMillis ASC")
    suspend fun getAll(): List<DayEventEntity>

    @Query("SELECT * FROM day_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DayEventEntity?

    @Upsert
    suspend fun upsert(event: DayEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<DayEventEntity>)

    @Query("DELETE FROM day_events")
    suspend fun clearAll()

    @Query("UPDATE day_events SET archived = :archived, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAtEpochMillis: Long)

    @Query("UPDATE day_events SET sortOrder = :sortOrder, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Int, updatedAtEpochMillis: Long)
}
