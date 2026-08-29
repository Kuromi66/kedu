package com.pulse.checkin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pulse.checkin.data.db.dao.CheckInEventDao
import com.pulse.checkin.data.db.dao.DayEventDao
import com.pulse.checkin.data.db.dao.HabitDao
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.DayEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity

@Database(
    entities = [HabitEntity::class, CheckInEventEntity::class, DayEventEntity::class],
    version = 10,
    exportSchema = false,
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun checkInEventDao(): CheckInEventDao
    abstract fun dayEventDao(): DayEventDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE check_in_events ADD COLUMN isBackfilled INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS habits_new (" +
                        "id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, colorArgb INTEGER NOT NULL, " +
                        "glyph TEXT NOT NULL, sortOrder INTEGER NOT NULL, reminderEnabled INTEGER NOT NULL, " +
                        "reminderHour INTEGER, reminderMinute INTEGER, targetEnabled INTEGER NOT NULL, " +
                        "dailyTargetCount INTEGER, createdAtEpochMillis INTEGER NOT NULL, " +
                        "archived INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO habits_new (id, name, colorArgb, glyph, sortOrder, reminderEnabled, " +
                        "reminderHour, reminderMinute, targetEnabled, dailyTargetCount, createdAtEpochMillis, " +
                        "archived, updatedAtEpochMillis) " +
                        "SELECT 'h-' || id, name, colorArgb, glyph, sortOrder, reminderEnabled, reminderHour, " +
                        "reminderMinute, targetEnabled, dailyTargetCount, createdAtEpochMillis, archived, " +
                        "createdAtEpochMillis FROM habits",
                )
                db.execSQL("DROP TABLE habits")
                db.execSQL("ALTER TABLE habits_new RENAME TO habits")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS check_in_events_new (" +
                        "id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, " +
                        "occurredAtEpochMillis INTEGER NOT NULL, localDate TEXT NOT NULL, " +
                        "isBackfilled INTEGER NOT NULL, deletedAtEpochMillis INTEGER, " +
                        "updatedAtEpochMillis INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO check_in_events_new (id, habitId, occurredAtEpochMillis, localDate, " +
                        "isBackfilled, deletedAtEpochMillis, updatedAtEpochMillis) " +
                        "SELECT 'e-' || id, 'h-' || habitId, occurredAtEpochMillis, localDate, isBackfilled, " +
                        "NULL, occurredAtEpochMillis FROM check_in_events",
                )
                db.execSQL("DROP TABLE check_in_events")
                db.execSQL("ALTER TABLE check_in_events_new RENAME TO check_in_events")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE check_in_events ADD COLUMN note TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS day_events (" +
                        "id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, eventDate TEXT NOT NULL, " +
                        "repeatsYearly INTEGER NOT NULL DEFAULT 0, note TEXT, " +
                        "createdAtEpochMillis INTEGER NOT NULL, archived INTEGER NOT NULL DEFAULT 0, " +
                        "updatedAtEpochMillis INTEGER NOT NULL)",
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_events ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_events ADD COLUMN calendarType TEXT NOT NULL DEFAULT 'SOLAR'")
                db.execSQL("ALTER TABLE day_events ADD COLUMN lunarMonth INTEGER")
                db.execSQL("ALTER TABLE day_events ADD COLUMN lunarDay INTEGER")
                db.execSQL("ALTER TABLE day_events ADD COLUMN lunarLeap INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_events ADD COLUMN repeatsMonthly INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_events ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE day_events ADD COLUMN reminderDaysBefore INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN deletedAtEpochMillis INTEGER")
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
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .addMigrations(MIGRATION_7_8)
                    .addMigrations(MIGRATION_8_9)
                    .addMigrations(MIGRATION_9_10)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
