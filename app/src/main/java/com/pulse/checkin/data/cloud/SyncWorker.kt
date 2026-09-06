package com.pulse.checkin.data.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pulse.checkin.PulseApplication
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PulseApplication).container
        return when (container.syncManager.reconcileOnce()) {
            is SyncOutcome.Success -> {
                container.reminderScheduler.syncAll(container.habitRepository.getActiveReminderHabits())
                container.dayEventReminderScheduler.syncAll(
                    container.dayEventRepository.getActiveReminderDayEvents(),
                )
                Result.success()
            }
            is SyncOutcome.SignedOut -> Result.success()
            is SyncOutcome.Failure -> Result.success()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "pulse_periodic_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
