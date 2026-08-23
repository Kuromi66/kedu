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

    suspend fun login(email: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val response = api.login(AuthRequest(email = email.trim(), password = password))
            session.saveSession(response.token, response.userId, email.trim())
            session.clockOffsetMillis = response.serverTime - System.currentTimeMillis()
            session.saveSyncState(watermark = 0L, lastSyncAtEpochMillis = 0L)
        }.mapFailure()
    }

    suspend fun register(email: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val response = api.register(AuthRequest(email = email.trim(), password = password))
            session.saveSession(response.token, response.userId, email.trim())
            session.clockOffsetMillis = response.serverTime - System.currentTimeMillis()
            session.saveSyncState(watermark = 0L, lastSyncAtEpochMillis = 0L)
        }.mapFailure()
    }

    suspend fun logout() = withContext(ioDispatcher) {
        try {
            val token = session.currentToken()
            if (!token.isNullOrBlank()) {
                runCatching { api.logout() }
            }
        } finally {
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
                val pushHabits = localHabits
                    .filter { it.updatedAtEpochMillis > watermark }
                    .map { it.toDto() }
                val pushEvents = localEvents
                    .filter { it.updatedAtEpochMillis > watermark }
                    .map { it.toDto() }
                val response = api.sync(
                    SyncRequest(
                        since = watermark,
                        habits = pushHabits,
                        events = pushEvents,
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
                session.saveSyncState(
                    watermark = response.serverTime,
                    lastSyncAtEpochMillis = now,
                )
                SyncOutcome.Success(
                    pulledHabits = response.habits.size,
                    pulledEvents = response.events.size,
                )
            } catch (exception: Exception) {
                SyncOutcome.Failure(exception.toSyncError())
            }
        }
    }

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
