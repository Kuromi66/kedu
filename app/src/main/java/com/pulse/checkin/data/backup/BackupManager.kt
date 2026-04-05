package com.pulse.checkin.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.pulse.checkin.data.db.PulseDatabase
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity
import com.pulse.checkin.data.preferences.AppPreferences
import com.pulse.checkin.domain.model.SortMode
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.domain.model.UserPreferences
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val habitCount: Int,
    val eventCount: Int,
)

class BackupManager(
    private val context: Context,
    private val database: PulseDatabase,
    private val appPreferences: AppPreferences,
) {
    suspend fun exportToUri(uri: Uri): Result<BackupSummary> = runCatching {
        withContext(Dispatchers.IO) {
            val habits = database.habitDao().getAll()
            val events = database.checkInEventDao().getAll()
            val preferences = appPreferences.currentPreferences()
            val payload = JSONObject()
                .put("version", 1)
                .put("exportedAtEpochMillis", System.currentTimeMillis())
                .put("preferences", preferences.toJson())
                .put("habits", JSONArray().apply { habits.forEach { put(it.toJson()) } })
                .put("events", JSONArray().apply { events.forEach { put(it.toJson()) } })

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(payload.toString(2).toByteArray(StandardCharsets.UTF_8))
                output.flush()
            } ?: error("无法创建导出文件")

            BackupSummary(habitCount = habits.size, eventCount = events.size)
        }
    }

    suspend fun importFromUri(uri: Uri): Result<BackupSummary> = runCatching {
        withContext(Dispatchers.IO) {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取备份文件")
            val root = JSONObject(raw)
            val version = root.optInt("version", -1)
            require(version == 1) { "暂不支持这个备份版本" }

            val preferences = root.optJSONObject("preferences")?.toUserPreferences()
                ?: error("备份文件缺少设置信息")
            val habits = root.optJSONArray("habits")?.toHabitEntities().orEmpty()
            val events = root.optJSONArray("events")?.toEventEntities().orEmpty()

            database.withTransaction {
                database.checkInEventDao().clearAll()
                database.habitDao().clearAll()
                if (habits.isNotEmpty()) database.habitDao().insertAll(habits)
                if (events.isNotEmpty()) database.checkInEventDao().insertAll(events)
            }
            appPreferences.replaceAll(preferences)

            BackupSummary(habitCount = habits.size, eventCount = events.size)
        }
    }
}

private fun UserPreferences.toJson(): JSONObject = JSONObject()
    .put("themeMode", themeMode.name)
    .put("sortMode", sortMode.name)
    .put("onboardingSeen", onboardingSeen)
    .put("notificationPromptSeen", notificationPromptSeen)

private fun JSONObject.toUserPreferences(): UserPreferences = UserPreferences(
    themeMode = optString("themeMode").takeIf { it.isNotBlank() }?.let(ThemeMode::valueOf) ?: ThemeMode.LIGHT,
    sortMode = optString("sortMode").takeIf { it.isNotBlank() }?.let(SortMode::valueOf) ?: SortMode.MANUAL,
    onboardingSeen = optBoolean("onboardingSeen", false),
    notificationPromptSeen = optBoolean("notificationPromptSeen", false),
)

private fun HabitEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("colorArgb", colorArgb)
    .put("glyph", glyph)
    .put("sortOrder", sortOrder)
    .put("reminderEnabled", reminderEnabled)
    .put("reminderHour", reminderHour)
    .put("reminderMinute", reminderMinute)
    .put("targetEnabled", targetEnabled)
    .put("dailyTargetCount", dailyTargetCount)
    .put("createdAtEpochMillis", createdAtEpochMillis)
    .put("archived", archived)

private fun CheckInEventEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("habitId", habitId)
    .put("occurredAtEpochMillis", occurredAtEpochMillis)
    .put("localDate", localDate)

private fun JSONArray.toHabitEntities(): List<HabitEntity> = buildList(length()) {
    repeat(length()) { index ->
        val item = getJSONObject(index)
        add(
            HabitEntity(
                id = item.optLong("id", 0L),
                name = item.optString("name"),
                colorArgb = item.optLong("colorArgb"),
                glyph = item.optString("glyph"),
                sortOrder = item.optInt("sortOrder", index),
                reminderEnabled = item.optBoolean("reminderEnabled", false),
                reminderHour = item.optNullableInt("reminderHour"),
                reminderMinute = item.optNullableInt("reminderMinute"),
                targetEnabled = item.optBoolean("targetEnabled", false),
                dailyTargetCount = item.optNullableInt("dailyTargetCount"),
                createdAtEpochMillis = item.optLong("createdAtEpochMillis", System.currentTimeMillis()),
                archived = item.optBoolean("archived", false),
            ),
        )
    }
}

private fun JSONArray.toEventEntities(): List<CheckInEventEntity> = buildList(length()) {
    repeat(length()) { index ->
        val item = getJSONObject(index)
        add(
            CheckInEventEntity(
                id = item.optLong("id", 0L),
                habitId = item.optLong("habitId"),
                occurredAtEpochMillis = item.optLong("occurredAtEpochMillis"),
                localDate = item.optString("localDate"),
            ),
        )
    }
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (isNull(key) || !has(key)) return null
    return optInt(key)
}
