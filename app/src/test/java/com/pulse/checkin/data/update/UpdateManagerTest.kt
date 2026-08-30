package com.pulse.checkin.data.update

import com.pulse.checkin.data.cloud.CloudApi
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import retrofit2.Response

class UpdateManagerTest {
    private val today = LocalDate.of(2026, 8, 30)

    private class FakeApi(
        var manifest: VersionManifest = VersionManifest(
            versionCode = 200,
            versionName = "2.0.0",
            notes = "更新说明",
            url = "https://example.com/update",
        ),
    ) : CloudApi {
        var fetchCount = 0

        override suspend fun fetchVersion(): VersionManifest {
            fetchCount += 1
            return manifest
        }

        override suspend fun register(body: com.pulse.checkin.data.cloud.AuthRequest): com.pulse.checkin.data.cloud.AuthResponse =
            error("unused")

        override suspend fun login(body: com.pulse.checkin.data.cloud.AuthRequest): com.pulse.checkin.data.cloud.AuthResponse =
            error("unused")

        override suspend fun logout(authorization: String): Response<Unit> = error("unused")

        override suspend fun sync(
            authorization: String,
            body: com.pulse.checkin.data.cloud.SyncRequest,
        ): com.pulse.checkin.data.cloud.SyncResponse = error("unused")
    }

    private class FakeStore : UpdateStateStore {
        var checkDate: String? = null
        var notifiedVersion: Int = 0

        override suspend fun lastUpdateCheckDate(): String? = checkDate

        override suspend fun lastNotifiedVersionCode(): Int = notifiedVersion

        override suspend fun markUpdateChecked(date: String) {
            checkDate = date
        }

        override suspend fun markNotified(versionCode: Int) {
            notifiedVersion = versionCode
        }
    }

    private fun manager(
        api: FakeApi = FakeApi(),
        store: FakeStore = FakeStore(),
    ) = UpdateManager(
        api = api,
        stateStore = store,
        currentVersionCode = 100,
        today = { today },
    )

    @Test
    fun `newer manifest is available`() = runBlocking {
        val result = manager().check(force = true)
        assertIs<UpdateCheckResult.Available>(result)
        assertEquals("2.0.0", result.manifest.versionName)
    }

    @Test
    fun `same or older manifest is up to date`() = runBlocking {
        val same = manager(api = FakeApi(manifest = VersionManifest(100, "1.0.0", "", "")))
        assertEquals(UpdateCheckResult.UpToDate, same.check(force = true))
        val older = manager(api = FakeApi(manifest = VersionManifest(99, "0.9.0", "", "")))
        assertEquals(UpdateCheckResult.UpToDate, older.check(force = true))
    }

    @Test
    fun `non force check skips network when already checked today`() = runBlocking {
        val api = FakeApi()
        val store = FakeStore().apply { checkDate = today.toString() }
        val result = manager(api = api, store = store).check(force = false)
        assertIs<UpdateCheckResult.SkippedToday>(result)
        assertEquals(0, api.fetchCount)
    }

    @Test
    fun `force check ignores daily throttle`() = runBlocking {
        val api = FakeApi()
        val store = FakeStore().apply { checkDate = today.toString() }
        val result = manager(api = api, store = store).check(force = true)
        assertIs<UpdateCheckResult.Available>(result)
        assertEquals(1, api.fetchCount)
    }

    @Test
    fun `successful check records today as checked`() = runBlocking {
        val store = FakeStore()
        manager(store = store).check(force = true)
        assertEquals(today.toString(), store.checkDate)
    }

    @Test
    fun `shouldNotify only when newer and not yet notified`() = runBlocking {
        val m = manager()
        val manifest = VersionManifest(200, "2.0.0", "", "")
        assertTrue(m.shouldNotify(manifest))
        m.markNotified(manifest)
        assertFalse(m.shouldNotify(manifest))
        assertFalse(m.shouldNotify(VersionManifest(100, "1.0.0", "", "")))
    }
}
