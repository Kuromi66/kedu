package com.pulse.checkin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pulse.checkin.data.db.dao.CheckInEventDao
import com.pulse.checkin.data.db.dao.HabitDao
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity

@Database(
    entities = [HabitEntity::class, CheckInEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun checkInEventDao(): CheckInEventDao

    companion object {
        @Volatile
        private var instance: PulseDatabase? = null

        fun getInstance(context: Context): PulseDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    "pulse-database",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
