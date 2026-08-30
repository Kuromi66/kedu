package com.pulse.checkin.data.update

interface UpdateStateStore {
    suspend fun lastUpdateCheckDate(): String?
    suspend fun lastNotifiedVersionCode(): Int
    suspend fun markUpdateChecked(date: String)
    suspend fun markNotified(versionCode: Int)
}
