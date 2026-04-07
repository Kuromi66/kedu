package com.pulse.checkin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pulse.checkin.data.db.dao.CheckInEventDao
import com.pulse.checkin.data.db.dao.HabitDao
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity

@Database(
    entities = [HabitEntity::class, CheckInEventEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun checkInEventDao(): CheckInEventDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE check_in_events ADD COLUMN isBackfilled INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        @Volatile
        private var instance: PulseDatabase? = null

        fun getInstance(context: Context): PulseDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    "pulse-database",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
