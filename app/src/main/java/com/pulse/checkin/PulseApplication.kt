package com.pulse.checkin

import android.app.Application
import android.content.Context
import com.pulse.checkin.data.backup.BackupManager
import com.pulse.checkin.data.backup.CsvExportManager
import com.pulse.checkin.data.cloud.CloudApi
import com.pulse.checkin.data.cloud.CloudApiFactory
import com.pulse.checkin.data.cloud.DeviceSyncClock
import com.pulse.checkin.data.cloud.RoomSyncDataSource
import com.pulse.checkin.data.cloud.SessionManager
import com.pulse.checkin.data.cloud.SyncClock
import com.pulse.checkin.data.cloud.SyncDataSource
import com.pulse.checkin.data.cloud.SyncManager
import com.pulse.checkin.data.cloud.SyncWorker
import com.pulse.checkin.data.db.PulseDatabase
import com.pulse.checkin.data.preferences.AppPreferences
import com.pulse.checkin.data.repository.CheckInRepositoryImpl
import com.pulse.checkin.data.repository.DayEventRepositoryImpl
import com.pulse.checkin.data.repository.HabitRepositoryImpl
import com.pulse.checkin.domain.repository.CheckInRepository
import com.pulse.checkin.domain.repository.DayEventRepository
import com.pulse.checkin.domain.repository.HabitRepository
import com.pulse.checkin.domain.stats.LocalStatsCalculator
import com.pulse.checkin.domain.stats.StatsCalculator
import com.pulse.checkin.reminder.ReminderScheduler
import com.pulse.checkin.reminder.ReminderSchedulerImpl
import com.pulse.checkin.reminder.DayEventReminderScheduler
import com.pulse.checkin.widget.PulseWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PulseApplication : Application() {
    lateinit var container: AppContainer
        private set
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncWorker.schedule(this)
        appScope.launch {
            container.dayEventReminderScheduler.syncAll(
                container.dayEventRepository.getActiveReminderDayEvents(),
            )
        }
        appScope.launch {
            combine(
                container.habitRepository.observeHabits(),
                container.checkInRepository.observeAllEvents(),
                container.dayEventRepository.observeDayEvents(),
            ) { habits, events, dayEvents -> Triple(habits, events, dayEvents) }
                .collect {
                    container.pulseWidgetUpdater.updateAll()
                }
        }
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = PulseDatabase.getInstance(appContext)

    val sessionManager = SessionManager(appContext)
    val syncClock: SyncClock = DeviceSyncClock(sessionManager)
    val habitRepository: HabitRepository = HabitRepositoryImpl(
        database.habitDao(),
        database.checkInEventDao(),
        syncClock,
    )
    val checkInRepository: CheckInRepository = CheckInRepositoryImpl(database.checkInEventDao(), syncClock)
    val dayEventRepository: DayEventRepository = DayEventRepositoryImpl(database.dayEventDao(), syncClock)
    val preferences = AppPreferences(appContext)
    val backupManager = BackupManager(appContext, database, preferences)
    val csvExportManager = CsvExportManager(appContext, database)
    val statsCalculator: StatsCalculator = LocalStatsCalculator()
    val reminderScheduler: ReminderScheduler = ReminderSchedulerImpl(appContext)
    val dayEventReminderScheduler = DayEventReminderScheduler(appContext)
    val pulseWidgetUpdater = PulseWidgetUpdater(appContext)
    val cloudApi: CloudApi = CloudApiFactory.create()
    val syncDataSource: SyncDataSource = RoomSyncDataSource(
        database.habitDao(),
        database.checkInEventDao(),
        database.dayEventDao(),
    )
    val syncManager = SyncManager(cloudApi, sessionManager, syncDataSource, syncClock)
}
