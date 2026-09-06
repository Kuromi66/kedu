package com.pulse.checkin.data.cloud

import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.DayEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity
import com.pulse.checkin.data.update.VersionManifest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncManagerTest {

    private class FakeSession : SyncSessionStore {
        private var token: String? = "token-1"
        private var watermark = 1_000L
        private var lastSyncAt = 0L
        private var lastReconcileAt = 0L

        override val sessionFlow: Flow<Session?> =
            MutableStateFlow(Session(token = "token-1", userId = "u-1", email = "a@example.com"))

        override var clockOffsetMillis: Long = 0L

        override suspend fun currentToken(): String? = token

        override suspend fun currentUserId(): String? = "u-1"

        override suspend fun currentAccountEmail(): String? = "a@example.com"

        override suspend fun currentWatermark(): Long = watermark

        override suspend fun currentLastSyncAt(): Long = lastSyncAt

        override suspend fun currentLastReconcileAt(): Long = lastReconcileAt

        override suspend fun saveSession(token: String, userId: String, email: String) {
            this.token = token
        }

        override suspend fun clearSession() {
            token = null
        }

        override suspend fun saveSyncState(watermark: Long, lastSyncAtEpochMillis: Long) {
            this.watermark = watermark
            this.lastSyncAt = lastSyncAtEpochMillis
        }

        override suspend fun saveLastReconcileAt(reconcileAtEpochMillis: Long) {
            this.lastReconcileAt = reconcileAtEpochMillis
        }
    }

    private class FakeApi(
        var syncResponse: SyncResponse = SyncResponse(serverTime = 3_000L),
        var metaResponse: MetaResponse = MetaResponse(serverTime = 3_000L),
    ) : CloudApi {
        var lastRequest: SyncRequest? = null
        var lastAuth: AuthRequest? = null
        var lastAuthHeader: String? = null

        override suspend fun fetchVersion(): VersionManifest = VersionManifest(1, "1.0.0", "", "")

        override suspend fun register(body: AuthRequest): AuthResponse {
            lastAuth = body
            return AuthResponse(token = "t", userId = "u", serverTime = 3_000L)
        }

        override suspend fun login(body: AuthRequest): AuthResponse {
            lastAuth = body
            return AuthResponse(token = "t", userId = "u", serverTime = 3_000L)
        }

        override suspend fun logout(authorization: String): Response<Unit> {
            lastAuthHeader = authorization
            return Response.success(Unit)
        }

        override suspend fun sync(authorization: String, body: SyncRequest): SyncResponse {
            lastAuthHeader = authorization
            lastRequest = body
            return syncResponse
        }

        override suspend fun syncMeta(authorization: String): MetaResponse {
            lastAuthHeader = authorization
            return metaResponse
        }
    }

    private class FakeDataSource(
        val habits: MutableList<HabitEntity> = mutableListOf(),
        val events: MutableList<CheckInEventEntity> = mutableListOf(),
        val dayEvents: MutableList<DayEventEntity> = mutableListOf(),
    ) : SyncDataSource {
        override suspend fun getAllHabits(): List<HabitEntity> = habits.toList()

        override suspend fun upsertHabits(habits: List<HabitEntity>) {
            habits.forEach { habit ->
                this.habits.removeAll { it.id == habit.id }
                this.habits.add(habit)
            }
        }

        override suspend fun getAllEventsIncludingDeleted(): List<CheckInEventEntity> = events.toList()

        override suspend fun upsertEvents(events: List<CheckInEventEntity>) {
            events.forEach { event ->
                this.events.removeAll { it.id == event.id }
                this.events.add(event)
            }
        }

        override suspend fun getAllDayEvents(): List<DayEventEntity> = dayEvents.toList()

        override suspend fun upsertDayEvents(events: List<DayEventEntity>) {
            events.forEach { event ->
                this.dayEvents.removeAll { it.id == event.id }
                this.dayEvents.add(event)
            }
        }

        override suspend fun clearAll() {
            habits.clear()
            events.clear()
            dayEvents.clear()
        }
    }

    private val fakeClock = object : SyncClock {
        override fun nowMillis(): Long = 10_000L
    }

    @Test
    fun `signed out sync is a no-op`() = runBlocking {
        val session = FakeSession().apply { clearSession() }
        val manager = SyncManager(FakeApi(), session, FakeDataSource(), fakeClock)

        val outcome = manager.syncOnce()

        assertEquals(SyncOutcome.SignedOut, outcome)
    }

    @Test
    fun `sync pushes only changed records and applies pulled records`() = runBlocking {
        val session = FakeSession()
        val dataSource = FakeDataSource(
            habits = mutableListOf(
                habit(id = "h-local", updatedAt = 1_500L),
                habit(id = "h-old", updatedAt = 500L),
            ),
            events = mutableListOf(event(id = "e-local", habitId = "h-local", updatedAt = 1_600L)),
        )
        val api = FakeApi(
            syncResponse = SyncResponse(
                serverTime = 3_000L,
                habits = listOf(
                    HabitDto(
                        id = "pulled-h",
                        name = "云端习惯",
                        colorArgb = 1L,
                        glyph = "P",
                        sortOrder = 0,
                        reminderEnabled = false,
                        targetEnabled = false,
                        createdAtEpochMillis = 1L,
                        archived = false,
                        updatedAtEpochMillis = 2_000L,
                    ),
                ),
                events = listOf(
                    EventDto(
                        id = "pulled-e",
                        habitId = "pulled-h",
                        occurredAtEpochMillis = 2L,
                        localDate = "2026-08-23",
                        updatedAtEpochMillis = 2_100L,
                    ),
                ),
            ),
        )
        val manager = SyncManager(api, session, dataSource, fakeClock)

        val outcome = manager.syncOnce()

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals("Bearer token-1", api.lastAuthHeader)
        assertEquals(listOf("h-local"), api.lastRequest?.habits?.map { it.id })
        assertEquals(listOf("e-local"), api.lastRequest?.events?.map { it.id })
        assertEquals(1_000L, api.lastRequest?.since)
        assertTrue(dataSource.habits.any { it.id == "pulled-h" })
        assertTrue(dataSource.events.any { it.id == "pulled-e" })
        assertEquals(3_000L, session.currentWatermark())
        assertTrue(session.clockOffsetMillis < 0L)
    }

    @Test
    fun `sync uploads tombstones for deleted events`() = runBlocking {
        val session = FakeSession()
        val dataSource = FakeDataSource(
            events = mutableListOf(
                event(id = "e-deleted", habitId = "h-local", updatedAt = 1_700L, deletedAt = 1_700L),
            ),
        )
        val api = FakeApi()
        val manager = SyncManager(api, session, dataSource, fakeClock)

        manager.syncOnce()

        assertEquals(listOf("e-deleted"), api.lastRequest?.events?.map { it.id })
        assertEquals(1_700L, api.lastRequest?.events?.single()?.deletedAtEpochMillis)
    }

    @Test
    fun `reconcile uploads newer local and fetches newer remote by id`() = runBlocking {
        val session = FakeSession()
        // 本地有两条习惯：h-local 比服务端新，h-missing 服务端没有；h-remote 服务端有但本地缺
        val dataSource = FakeDataSource(
            habits = mutableListOf(
                habit(id = "h-local", updatedAt = 1_500L),
                habit(id = "h-missing", updatedAt = 500L),
            ),
        )
        val api = FakeApi(
            metaResponse = MetaResponse(
                serverTime = 4_000L,
                habits = listOf(
                    RecordMeta(id = "h-local", updatedAtEpochMillis = 1_000L),
                    RecordMeta(id = "h-remote", updatedAtEpochMillis = 2_000L),
                ),
            ),
            syncResponse = SyncResponse(
                serverTime = 5_000L,
                habits = listOf(
                    HabitDto(
                        id = "h-remote",
                        name = "云端习惯",
                        colorArgb = 1L,
                        glyph = "P",
                        sortOrder = 0,
                        reminderEnabled = false,
                        targetEnabled = false,
                        createdAtEpochMillis = 1L,
                        archived = false,
                        updatedAtEpochMillis = 2_000L,
                    ),
                ),
            ),
        )
        val manager = SyncManager(api, session, dataSource, fakeClock)

        val outcome = manager.reconcileOnce()

        assertIs<SyncOutcome.Success>(outcome)
        // 只上传「本地更新/本地有而服务端缺」的
        assertEquals(listOf("h-local", "h-missing"), api.lastRequest?.habits?.map { it.id })
        // 只拉取「服务端更新/本地缺」的，避免全量回显
        assertEquals(listOf("h-remote"), api.lastRequest?.fetchIds?.habits)
        // 拉回的远端版本写回本地
        assertTrue(dataSource.habits.any { it.id == "h-remote" })
        assertEquals(5_000L, session.currentWatermark())
    }

    @Test
    fun `sync pushes and merges day events`() = runBlocking {
        val session = FakeSession()
        val dataSource = FakeDataSource(
            dayEvents = mutableListOf(
                DayEventEntity(
                    id = "d-local",
                    name = "纪念日",
                    eventDate = "2026-09-01",
                    repeatsYearly = true,
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 1_500L,
                ),
            ),
        )
        val api = FakeApi(
            syncResponse = SyncResponse(
                serverTime = 3_000L,
                dayEvents = listOf(
                    DayEventDto(
                        id = "d-pulled",
                        name = "考试",
                        eventDate = "2026-12-01",
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 2_000L,
                    ),
                ),
            ),
        )
        val manager = SyncManager(api, session, dataSource, fakeClock)

        val outcome = manager.syncOnce()

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(listOf("d-local"), api.lastRequest?.dayEvents?.map { it.id })
        assertTrue(dataSource.dayEvents.any { it.id == "d-pulled" })
        assertEquals(1, outcome.pulledDayEvents)
    }

    @Test
    fun `login saves session and resets watermark for full merge`() = runBlocking {
        val session = FakeSession()
        val manager = SyncManager(FakeApi(), session, FakeDataSource(), fakeClock)

        val result = manager.login("  a@example.com  ", "secret123")

        assertTrue(result.isSuccess)
        assertEquals("a@example.com", manager.sessionFlow.firstSessionEmail())
        assertEquals(0L, session.currentWatermark())
    }

    @Test
    fun `login clears local data to avoid leaking previous account`() = runBlocking {
        val session = FakeSession()
        val dataSource = FakeDataSource(
            habits = mutableListOf(habit(id = "old-h", updatedAt = 1_000L)),
            events = mutableListOf(event(id = "old-e", habitId = "old-h", updatedAt = 1_000L)),
        )
        val manager = SyncManager(FakeApi(), session, dataSource, fakeClock)

        val result = manager.login("a@example.com", "secret123")

        assertTrue(result.isSuccess)
        assertTrue(dataSource.habits.isEmpty())
        assertTrue(dataSource.events.isEmpty())
        assertEquals(0L, session.currentWatermark())
    }

    @Test
    fun `logout clears local data`() = runBlocking {
        val session = FakeSession()
        val dataSource = FakeDataSource(
            habits = mutableListOf(habit(id = "old-h", updatedAt = 1_000L)),
            events = mutableListOf(event(id = "old-e", habitId = "old-h", updatedAt = 1_000L)),
        )
        val manager = SyncManager(FakeApi(), session, dataSource, fakeClock)

        manager.logout()

        assertTrue(dataSource.habits.isEmpty())
        assertTrue(dataSource.events.isEmpty())
        assertNull(session.currentToken())
    }

    @Test
    fun `register maps http 409 to email taken`() = runBlocking {
        val failingApi = object : CloudApi {
            override suspend fun fetchVersion(): VersionManifest = VersionManifest(1, "1.0.0", "", "")
            override suspend fun register(body: AuthRequest): AuthResponse {
                throw HttpException(
                    Response.error<Any>(
                        409,
                        """{"error":"Email already registered"}""".toResponseBody("application/json".toMediaType()),
                    ),
                )
            }

            override suspend fun login(body: AuthRequest): AuthResponse = error("unused")
            override suspend fun logout(authorization: String): Response<Unit> = Response.success(Unit)
            override suspend fun sync(authorization: String, body: SyncRequest): SyncResponse = error("unused")
            override suspend fun syncMeta(authorization: String): MetaResponse = MetaResponse(serverTime = 0L)
        }
        val manager = SyncManager(failingApi, FakeSession(), FakeDataSource(), fakeClock)

        val result = manager.register("a@example.com", "secret123")

        assertTrue(result.isFailure)
        assertEquals(SyncError.EMAIL_TAKEN, (result.exceptionOrNull() as? SyncException)?.error)
    }

    @Test
    fun `sync failure returns typed error`() = runBlocking {
        val failingApi = object : CloudApi {
            override suspend fun fetchVersion(): VersionManifest = VersionManifest(1, "1.0.0", "", "")
            override suspend fun register(body: AuthRequest): AuthResponse = error("unused")
            override suspend fun login(body: AuthRequest): AuthResponse = error("unused")
            override suspend fun logout(authorization: String): Response<Unit> = Response.success(Unit)
            override suspend fun sync(authorization: String, body: SyncRequest): SyncResponse {
                throw HttpException(
                    Response.error<Any>(401, """{"error":"Unauthorized"}""".toResponseBody("application/json".toMediaType())),
                )
            }
            override suspend fun syncMeta(authorization: String): MetaResponse = MetaResponse(serverTime = 0L)
        }
        val manager = SyncManager(failingApi, FakeSession(), FakeDataSource(), fakeClock)

        val outcome = manager.syncOnce()

        assertIs<SyncOutcome.Failure>(outcome)
        assertEquals(SyncError.INVALID_CREDENTIALS, outcome.error)
    }

    @Test
    fun `logout clears local session even when server unreachable`() = runBlocking {
        val session = FakeSession()
        val failingApi = object : CloudApi {
            override suspend fun fetchVersion(): VersionManifest = VersionManifest(1, "1.0.0", "", "")
            override suspend fun register(body: AuthRequest): AuthResponse = error("unused")
            override suspend fun login(body: AuthRequest): AuthResponse = error("unused")
            override suspend fun logout(authorization: String): Response<Unit> = throw RuntimeException("network down")
            override suspend fun sync(authorization: String, body: SyncRequest): SyncResponse = error("unused")
            override suspend fun syncMeta(authorization: String): MetaResponse = MetaResponse(serverTime = 0L)
        }
        val manager = SyncManager(failingApi, session, FakeDataSource(), fakeClock)

        manager.logout()

        assertNull(session.currentToken())
    }

    private fun habit(id: String, updatedAt: Long): HabitEntity = HabitEntity(
        id = id,
        name = "habit-$id",
        colorArgb = 1L,
        glyph = "H",
        sortOrder = 0,
        reminderEnabled = false,
        reminderHour = null,
        reminderMinute = null,
        targetEnabled = false,
        dailyTargetCount = null,
        createdAtEpochMillis = updatedAt,
        archived = false,
        updatedAtEpochMillis = updatedAt,
    )

    private fun event(
        id: String,
        habitId: String,
        updatedAt: Long,
        deletedAt: Long? = null,
    ): CheckInEventEntity = CheckInEventEntity(
        id = id,
        habitId = habitId,
        occurredAtEpochMillis = updatedAt,
        localDate = "2026-08-23",
        isBackfilled = false,
        deletedAtEpochMillis = deletedAt,
        updatedAtEpochMillis = updatedAt,
    )
}

private suspend fun Flow<Session?>.firstSessionEmail(): String? = first()?.email
