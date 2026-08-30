package com.pulse.checkin.data.update

import com.pulse.checkin.BuildConfig
import com.pulse.checkin.data.cloud.CloudApi
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val manifest: VersionManifest) : UpdateCheckResult
    data object Failed : UpdateCheckResult
    data object SkippedToday : UpdateCheckResult
}

class UpdateManager(
    private val api: CloudApi,
    private val stateStore: UpdateStateStore,
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    suspend fun check(force: Boolean): UpdateCheckResult = withContext(Dispatchers.IO) {
        val todayString = today().toString()
        if (!force && stateStore.lastUpdateCheckDate() == todayString) {
            return@withContext UpdateCheckResult.SkippedToday
        }
        try {
            val manifest = api.fetchVersion()
            stateStore.markUpdateChecked(todayString)
            if (manifest.versionCode > currentVersionCode) {
                UpdateCheckResult.Available(manifest)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            UpdateCheckResult.Failed
        }
    }

    suspend fun shouldNotify(manifest: VersionManifest): Boolean {
        return manifest.versionCode > currentVersionCode &&
            stateStore.lastNotifiedVersionCode() != manifest.versionCode
    }

    suspend fun markNotified(manifest: VersionManifest) {
        stateStore.markNotified(manifest.versionCode)
    }
}
