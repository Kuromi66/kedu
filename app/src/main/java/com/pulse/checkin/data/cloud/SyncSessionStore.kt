package com.pulse.checkin.data.cloud

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

interface SyncSessionStore {
    val sessionFlow: Flow<Session?>
    var clockOffsetMillis: Long
    suspend fun currentToken(): String?
    suspend fun currentUserId(): String?
    suspend fun currentAccountEmail(): String?
    suspend fun currentWatermark(): Long
    suspend fun currentLastSyncAt(): Long
    suspend fun currentLastReconcileAt(): Long
    suspend fun saveSession(token: String, userId: String, email: String)
    suspend fun clearSession()
    suspend fun saveSyncState(watermark: Long, lastSyncAtEpochMillis: Long)
    suspend fun saveLastReconcileAt(reconcileAtEpochMillis: Long)
}

private val Context.sessionDataStore by preferencesDataStore(name = "pulse_session")

class SessionManager(context: Context) : SyncSessionStore {
    private val appContext = context.applicationContext
    private val clockOffset = AtomicLong(0L)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object Keys {
        val authToken = stringPreferencesKey("auth_token")
        val userId = stringPreferencesKey("user_id")
        val accountEmail = stringPreferencesKey("account_email")
        val syncWatermark = longPreferencesKey("sync_watermark")
        val lastSyncAt = longPreferencesKey("last_sync_at")
        val lastReconcileAt = longPreferencesKey("last_reconcile_at")
        val clockOffset = longPreferencesKey("clock_offset")
    }

    init {
        scope.launch {
            val savedOffset = appContext.sessionDataStore.data.first()[Keys.clockOffset] ?: 0L
            clockOffset.set(savedOffset)
        }
    }

    override val sessionFlow: Flow<Session?> = appContext.sessionDataStore.data.map { preferences ->
        val token = preferences[Keys.authToken]
        val userId = preferences[Keys.userId]
        val email = preferences[Keys.accountEmail]
        if (token.isNullOrBlank() || userId.isNullOrBlank() || email.isNullOrBlank()) {
            null
        } else {
            Session(token = token, userId = userId, email = email)
        }
    }

    override var clockOffsetMillis: Long
        get() = clockOffset.get()
        set(value) {
            clockOffset.set(value)
            scope.launch {
                appContext.sessionDataStore.edit { preferences ->
                    preferences[Keys.clockOffset] = value
                }
            }
        }

    override suspend fun currentToken(): String? = appContext.sessionDataStore.data.first()[Keys.authToken]

    override suspend fun currentUserId(): String? = appContext.sessionDataStore.data.first()[Keys.userId]

    override suspend fun currentAccountEmail(): String? = appContext.sessionDataStore.data.first()[Keys.accountEmail]

    override suspend fun currentWatermark(): Long = appContext.sessionDataStore.data.first()[Keys.syncWatermark] ?: 0L

    override suspend fun currentLastSyncAt(): Long = appContext.sessionDataStore.data.first()[Keys.lastSyncAt] ?: 0L

    override suspend fun currentLastReconcileAt(): Long = appContext.sessionDataStore.data.first()[Keys.lastReconcileAt] ?: 0L

    override suspend fun saveSession(token: String, userId: String, email: String) {
        appContext.sessionDataStore.edit { preferences ->
            preferences[Keys.authToken] = token
            preferences[Keys.userId] = userId
            preferences[Keys.accountEmail] = email
        }
    }

    override suspend fun clearSession() {
        appContext.sessionDataStore.edit { preferences ->
            preferences.remove(Keys.authToken)
            preferences.remove(Keys.userId)
            preferences.remove(Keys.accountEmail)
        }
    }

    override suspend fun saveSyncState(watermark: Long, lastSyncAtEpochMillis: Long) {
        appContext.sessionDataStore.edit { preferences ->
            preferences[Keys.syncWatermark] = watermark
            preferences[Keys.lastSyncAt] = lastSyncAtEpochMillis
        }
    }

    override suspend fun saveLastReconcileAt(reconcileAtEpochMillis: Long) {
        appContext.sessionDataStore.edit { preferences ->
            preferences[Keys.lastReconcileAt] = reconcileAtEpochMillis
        }
    }
}
