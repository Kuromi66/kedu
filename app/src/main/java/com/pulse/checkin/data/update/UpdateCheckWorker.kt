package com.pulse.checkin.data.update

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

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PulseApplication).container
        when (val result = container.updateManager.check(force = false)) {
            is UpdateCheckResult.Available -> {
                if (container.updateManager.shouldNotify(result.manifest)) {
                    container.updateManager.markNotified(result.manifest)
                    UpdateNotifier.notify(applicationContext, result.manifest)
                }
            }
            else -> Unit
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "pulse_update_check"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
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
