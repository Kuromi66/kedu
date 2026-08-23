package com.pulse.checkin.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.pulse.checkin.data.db.PulseDatabase
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import com.pulse.checkin.data.db.entity.DayEventEntity
import com.pulse.checkin.data.db.entity.HabitEntity
import com.pulse.checkin.data.preferences.AppPreferences
import com.pulse.checkin.domain.model.AppLanguage
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
            val dayEvents = database.dayEventDao().getAll()
            val preferences = appPreferences.currentPreferences()
            val payload = JSONObject()
                .put("version", 2)
                .put("exportedAtEpochMillis", System.currentTimeMillis())
                .put("preferences", preferences.toJson())
                .put("habits", JSONArray().apply { habits.forEach { put(it.toJsonV2()) } })
                .put("events", JSONArray().apply { events.forEach { put(it.toJsonV2()) } })
                .put("dayEvents", JSONArray().apply { dayEvents.forEach { put(it.toJsonV2()) } })

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
            require(version == 1 || version == 2) { "暂不支持这个备份版本" }

            val preferences = root.optJSONObject("preferences")?.toUserPreferences()
                ?: error("备份文件缺少设置信息")
            val now = System.currentTimeMillis()
            val habits = root.optJSONArray("habits")?.toHabitEntities(version, now).orEmpty()
            val events = root.optJSONArray("events")?.toEventEntities(version, now).orEmpty()
            val dayEvents = root.optJSONArray("dayEvents")?.toDayEventEntities(version, now).orEmpty()

            database.withTransaction {
                database.checkInEventDao().clearAll()
                database.habitDao().clearAll()
                database.dayEventDao().clearAll()
                if (habits.isNotEmpty()) database.habitDao().insertAll(habits)
                if (events.isNotEmpty()) database.checkInEventDao().insertAll(events)
                if (dayEvents.isNotEmpty()) database.dayEventDao().insertAll(dayEvents)
            }
            appPreferences.replaceAll(preferences)

            BackupSummary(habitCount = habits.size, eventCount = events.size)
        }
    }
}

private fun UserPreferences.toJson(): JSONObject = JSONObject()
    .put("themeMode", themeMode.name)
    .put("appLanguage", appLanguage.name)
    .put("sortMode", sortMode.name)
    .put("onboardingSeen", onboardingSeen)
    .put("notificationPromptSeen", notificationPromptSeen)

private fun JSONObject.toUserPreferences(): UserPreferences = UserPreferences(
    themeMode = optString("themeMode").takeIf { it.isNotBlank() }?.let(ThemeMode::valueOf) ?: ThemeMode.LIGHT,
    appLanguage = optString("appLanguage").takeIf { it.isNotBlank() }?.let(AppLanguage::valueOf) ?: AppLanguage.ZH,
    sortMode = optString("sortMode").takeIf { it.isNotBlank() }?.let(SortMode::valueOf) ?: SortMode.MANUAL,
    onboardingSeen = optBoolean("onboardingSeen", false),
    notificationPromptSeen = optBoolean("notificationPromptSeen", false),
)

private fun HabitEntity.toJsonV2(): JSONObject = JSONObject()
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
    .put("updatedAtEpochMillis", updatedAtEpochMillis)

private fun CheckInEventEntity.toJsonV2(): JSONObject = JSONObject()
    .put("id", id)
    .put("habitId", habitId)
    .put("occurredAtEpochMillis", occurredAtEpochMillis)
    .put("localDate", localDate)
    .put("isBackfilled", isBackfilled)
    .put("deletedAtEpochMillis", deletedAtEpochMillis ?: JSONObject.NULL)
    .put("updatedAtEpochMillis", updatedAtEpochMillis)
    .put("note", note ?: JSONObject.NULL)

private fun DayEventEntity.toJsonV2(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("eventDate", eventDate)
    .put("repeatsYearly", repeatsYearly)
    .put("note", note ?: JSONObject.NULL)
    .put("sortOrder", sortOrder)
    .put("createdAtEpochMillis", createdAtEpochMillis)
    .put("archived", archived)
    .put("updatedAtEpochMillis", updatedAtEpochMillis)

private fun JSONArray.toHabitEntities(version: Int, now: Long): List<HabitEntity> = buildList(length()) {
    repeat(length()) { index ->
        val item = getJSONObject(index)
        add(
            if (version == 1) {
                val oldId = item.optLong("id", 0L)
                HabitEntity(
                    id = "h-$oldId",
                    name = item.optString("name"),
                    colorArgb = item.optLong("colorArgb"),
                    glyph = item.optString("glyph"),
                    sortOrder = item.optInt("sortOrder", index),
                    reminderEnabled = item.optBoolean("reminderEnabled", false),
                    reminderHour = item.optNullableInt("reminderHour"),
                    reminderMinute = item.optNullableInt("reminderMinute"),
                    targetEnabled = item.optBoolean("targetEnabled", false),
                    dailyTargetCount = item.optNullableInt("dailyTargetCount"),
                    createdAtEpochMillis = item.optLong("createdAtEpochMillis", now),
                    archived = item.optBoolean("archived", false),
                    updatedAtEpochMillis = now,
                )
            } else {
                HabitEntity(
                    id = item.optString("id").ifBlank { "h-$index-$now" },
                    name = item.optString("name"),
                    colorArgb = item.optLong("colorArgb"),
                    glyph = item.optString("glyph"),
                    sortOrder = item.optInt("sortOrder", index),
                    reminderEnabled = item.optBoolean("reminderEnabled", false),
                    reminderHour = item.optNullableInt("reminderHour"),
                    reminderMinute = item.optNullableInt("reminderMinute"),
                    targetEnabled = item.optBoolean("targetEnabled", false),
                    dailyTargetCount = item.optNullableInt("dailyTargetCount"),
                    createdAtEpochMillis = item.optLong("createdAtEpochMillis", now),
                    archived = item.optBoolean("archived", false),
                    updatedAtEpochMillis = item.optLong("updatedAtEpochMillis", now),
                )
            },
        )
    }
}

private fun JSONArray.toEventEntities(version: Int, now: Long): List<CheckInEventEntity> = buildList(length()) {
    repeat(length()) { index ->
        val item = getJSONObject(index)
        add(
            if (version == 1) {
                val oldId = item.optLong("id", 0L)
                val oldHabitId = item.optLong("habitId", 0L)
                CheckInEventEntity(
                    id = "e-$oldId",
                    habitId = "h-$oldHabitId",
                    occurredAtEpochMillis = item.optLong("occurredAtEpochMillis"),
                    localDate = item.optString("localDate"),
                    isBackfilled = item.optBoolean("isBackfilled", false),
                    deletedAtEpochMillis = null,
                    updatedAtEpochMillis = now,
                    note = null,
                )
            } else {
                CheckInEventEntity(
                    id = item.optString("id").ifBlank { "e-$index-$now" },
                    habitId = item.optString("habitId"),
                    occurredAtEpochMillis = item.optLong("occurredAtEpochMillis"),
                    localDate = item.optString("localDate"),
                    isBackfilled = item.optBoolean("isBackfilled", false),
                    deletedAtEpochMillis = item.optNullableLong("deletedAtEpochMillis"),
                    updatedAtEpochMillis = item.optLong("updatedAtEpochMillis", now),
                    note = item.optNullableString("note"),
                )
            },
        )
    }
}

private fun JSONArray.toDayEventEntities(version: Int, now: Long): List<DayEventEntity> = buildList(length()) {
    repeat(length()) { index ->
        val item = getJSONObject(index)
        if (version == 1) return@repeat
        add(
            DayEventEntity(
                id = item.optString("id").ifBlank { "d-$index-$now" },
                name = item.optString("name"),
                eventDate = item.optString("eventDate"),
                repeatsYearly = item.optBoolean("repeatsYearly", false),
                note = item.optNullableString("note"),
                sortOrder = item.optInt("sortOrder", index),
                createdAtEpochMillis = item.optLong("createdAtEpochMillis", now),
                archived = item.optBoolean("archived", false),
                updatedAtEpochMillis = item.optLong("updatedAtEpochMillis", now),
            ),
        )
    }
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (isNull(key) || !has(key)) return null
    return optInt(key)
}

private fun JSONObject.optNullableLong(key: String): Long? {
    if (isNull(key) || !has(key)) return null
    return optLong(key)
}

private fun JSONObject.optNullableString(key: String): String? {
    if (isNull(key) || !has(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}
