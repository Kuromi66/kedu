package com.pulse.checkin

import android.app.Application
import android.content.Context
import com.pulse.checkin.data.backup.BackupManager
import com.pulse.checkin.data.db.PulseDatabase
import com.pulse.checkin.data.preferences.AppPreferences
import com.pulse.checkin.data.repository.CheckInRepositoryImpl
import com.pulse.checkin.data.repository.HabitRepositoryImpl
import com.pulse.checkin.domain.repository.CheckInRepository
import com.pulse.checkin.domain.repository.HabitRepository
import com.pulse.checkin.domain.stats.LocalStatsCalculator
import com.pulse.checkin.domain.stats.StatsCalculator
import com.pulse.checkin.reminder.ReminderScheduler
import com.pulse.checkin.reminder.ReminderSchedulerImpl

class PulseApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = PulseDatabase.getInstance(appContext)

    val habitRepository: HabitRepository = HabitRepositoryImpl(database.habitDao())
    val checkInRepository: CheckInRepository = CheckInRepositoryImpl(database.checkInEventDao())
    val preferences = AppPreferences(appContext)
    val backupManager = BackupManager(appContext, database, preferences)
    val statsCalculator: StatsCalculator = LocalStatsCalculator()
    val reminderScheduler: ReminderScheduler = ReminderSchedulerImpl(appContext)
}
