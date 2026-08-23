package com.pulse.checkin.data.cloud

interface SyncClock {
    fun nowMillis(): Long
}

class DeviceSyncClock(
    private val session: SyncSessionStore,
) : SyncClock {
    override fun nowMillis(): Long = System.currentTimeMillis() + session.clockOffsetMillis
}
