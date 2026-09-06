package com.pulse.checkin.data.cloud

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SyncManager(
    private val api: CloudApi,
    private val session: SyncSessionStore,
    private val dataSource: SyncDataSource,
    private val clock: SyncClock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val syncMutex = Mutex()

    val sessionFlow: Flow<Session?> = session.sessionFlow

    suspend fun currentLastSyncAt(): Long = session.currentLastSyncAt()

    // 最近一次成功对帐的时间（0 表示从未对帐），用于进前台触发的节流判断
    suspend fun currentLastReconcileAt(): Long = session.currentLastReconcileAt()

    suspend fun login(email: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val response = api.login(AuthRequest(email = email.trim(), password = password))
            dataSource.clearAll()
            session.saveSession(response.token, response.userId, email.trim())
            session.clockOffsetMillis = response.serverTime - System.currentTimeMillis()
            session.saveSyncState(watermark = 0L, lastSyncAtEpochMillis = 0L)
        }.mapFailure()
    }

    suspend fun register(email: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val response = api.register(AuthRequest(email = email.trim(), password = password))
            dataSource.clearAll()
            session.saveSession(response.token, response.userId, email.trim())
            session.clockOffsetMillis = response.serverTime - System.currentTimeMillis()
            session.saveSyncState(watermark = 0L, lastSyncAtEpochMillis = 0L)
        }.mapFailure()
    }

    suspend fun logout() = withContext(ioDispatcher) {
        try {
            val token = session.currentToken()
            if (!token.isNullOrBlank()) {
                runCatching { api.logout("Bearer $token") }
            }
        } finally {
            dataSource.clearAll()
            session.clearSession()
        }
    }

    suspend fun syncOnce(): SyncOutcome = withContext(ioDispatcher) {
        syncMutex.withLock {
            val token = session.currentToken()
            if (token.isNullOrBlank()) {
                return@withLock SyncOutcome.SignedOut
            }
            try {
                val watermark = session.currentWatermark()
                val localHabits = dataSource.getAllHabits()
                val localEvents = dataSource.getAllEventsIncludingDeleted()
                val localDayEvents = dataSource.getAllDayEvents()
                // 增量上传：只传自上次水位以来改动过的记录（含墓碑），避免每次全量上传
                val pushHabits = localHabits
                    .filter { it.updatedAtEpochMillis > watermark }
                    .map { it.toDto() }
                val pushEvents = localEvents
                    .filter { it.updatedAtEpochMillis > watermark }
                    .map { it.toDto() }
                val pushDayEvents = localDayEvents
                    .filter { it.updatedAtEpochMillis > watermark }
                    .map { it.toDto() }
                val response = api.sync(
                    authorization = "Bearer $token",
                    body = SyncRequest(
                        since = watermark,
                        habits = pushHabits,
                        events = pushEvents,
                        dayEvents = pushDayEvents,
                    ),
                )
                val now = System.currentTimeMillis()
                session.clockOffsetMillis = response.serverTime - now
                if (response.habits.isNotEmpty()) {
                    dataSource.upsertHabits(response.habits.map { it.toEntity() })
                }
                if (response.events.isNotEmpty()) {
                    dataSource.upsertEvents(response.events.map { it.toEntity() })
                }
                if (response.dayEvents.isNotEmpty()) {
                    dataSource.upsertDayEvents(response.dayEvents.map { it.toEntity() })
                }
                session.saveSyncState(
                    watermark = response.serverTime,
                    lastSyncAtEpochMillis = now,
                )
                SyncOutcome.Success(
                    pulledHabits = response.habits.size,
                    pulledEvents = response.events.size,
                    pulledDayEvents = response.dayEvents.size,
                )
            } catch (exception: Exception) {
                SyncOutcome.Failure(exception.toSyncError())
            }
        }
    }

    // 对帐：先取服务端轻量摘要（id + updatedAt），与本地双向比对，
    // 只补传「本地更新/本地有而服务端缺」的记录，只拉取「服务端更新/本地缺」的记录。
    // 不依赖增量判据（时钟漂移也能稳），低频调用。
    suspend fun reconcileOnce(): SyncOutcome = withContext(ioDispatcher) {
        syncMutex.withLock {
            val token = session.currentToken()
            if (token.isNullOrBlank()) {
                return@withLock SyncOutcome.SignedOut
            }
            try {
                val meta = api.syncMeta("Bearer $token")
                val serverHabit = meta.habits.associate { it.id to it.updatedAtEpochMillis }
                val serverEvent = meta.events.associate { it.id to it.updatedAtEpochMillis }
                val serverDayEvent = meta.dayEvents.associate { it.id to it.updatedAtEpochMillis }

                val localHabits = dataSource.getAllHabits()
                val localEvents = dataSource.getAllEventsIncludingDeleted()
                val localDayEvents = dataSource.getAllDayEvents()

                val pushHabits = localHabits.filter { it.shouldUpload(serverHabit) }.map { it.toDto() }
                val pushEvents = localEvents.filter { it.shouldUpload(serverEvent) }.map { it.toDto() }
                val pushDayEvents = localDayEvents.filter { it.shouldUpload(serverDayEvent) }.map { it.toDto() }

                val fetchIds = FetchIds(
                    habits = meta.habits.filter { it.needsFetch(localHabits.map { e -> e.id to e.updatedAtEpochMillis }.toMap()) }.map { it.id },
                    events = meta.events.filter { it.needsFetch(localEvents.map { e -> e.id to e.updatedAtEpochMillis }.toMap()) }.map { it.id },
                    dayEvents = meta.dayEvents.filter { it.needsFetch(localDayEvents.map { e -> e.id to e.updatedAtEpochMillis }.toMap()) }.map { it.id },
                )

                val response = api.sync(
                    authorization = "Bearer $token",
                    body = SyncRequest(
                        since = session.currentWatermark(),
                        habits = pushHabits,
                        events = pushEvents,
                        dayEvents = pushDayEvents,
                        fetchIds = fetchIds,
                    ),
                )
                val now = System.currentTimeMillis()
                session.clockOffsetMillis = response.serverTime - now
                if (response.habits.isNotEmpty()) {
                    dataSource.upsertHabits(response.habits.map { it.toEntity() })
                }
                if (response.events.isNotEmpty()) {
                    dataSource.upsertEvents(response.events.map { it.toEntity() })
                }
                if (response.dayEvents.isNotEmpty()) {
                    dataSource.upsertDayEvents(response.dayEvents.map { it.toEntity() })
                }
                session.saveSyncState(
                    watermark = response.serverTime,
                    lastSyncAtEpochMillis = now,
                )
                session.saveLastReconcileAt(response.serverTime)
                SyncOutcome.Success(
                    pulledHabits = response.habits.size,
                    pulledEvents = response.events.size,
                    pulledDayEvents = response.dayEvents.size,
                )
            } catch (exception: Exception) {
                SyncOutcome.Failure(exception.toSyncError())
            }
        }
    }

    private fun com.pulse.checkin.data.db.entity.HabitEntity.shouldUpload(server: Map<String, Long>): Boolean =
        server[id] == null || updatedAtEpochMillis > server[id]!!

    private fun com.pulse.checkin.data.db.entity.CheckInEventEntity.shouldUpload(server: Map<String, Long>): Boolean =
        server[id] == null || updatedAtEpochMillis > server[id]!!

    private fun com.pulse.checkin.data.db.entity.DayEventEntity.shouldUpload(server: Map<String, Long>): Boolean =
        server[id] == null || updatedAtEpochMillis > server[id]!!

    // 判定服务端记录是否需要拉取：本地没有该 id，或服务端更新时间更新
    private fun RecordMeta.needsFetch(local: Map<String, Long>): Boolean =
        local[id] == null || updatedAtEpochMillis > local[id]!!

    private fun <T> Result<T>.mapFailure(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { exception ->
            if (exception is CancellationException) throw exception
            Result.failure(SyncException(exception.toSyncError()))
        },
    )

    private fun Throwable.toSyncError(): SyncError = when (this) {
        is HttpException -> when (code()) {
            401 -> SyncError.INVALID_CREDENTIALS
            409 -> SyncError.EMAIL_TAKEN
            400 -> SyncError.INVALID_INPUT
            else -> SyncError.UNKNOWN
        }
        is IOException -> SyncError.NETWORK
        is SyncException -> error
        else -> SyncError.UNKNOWN
    }
}
