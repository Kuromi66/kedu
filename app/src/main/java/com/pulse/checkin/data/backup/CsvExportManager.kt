package com.pulse.checkin.data.backup

import android.content.Context
import android.net.Uri
import com.pulse.checkin.R
import com.pulse.checkin.data.db.PulseDatabase
import com.pulse.checkin.data.db.entity.CheckInEventEntity
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CsvExportManager(
    private val context: Context,
    private val database: PulseDatabase,
) {
    suspend fun exportToUri(uri: Uri): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            val habits = database.habitDao().getAll().filter { it.deletedAtEpochMillis == null }
            val events = database.checkInEventDao().getAll()
            val nameById = habits.associate { it.id to it.name }
            val header = context.getString(R.string.csv_header)
            val csv = CsvFormat.buildCsv(header, nameById, events)
            val bytes = "\uFEFF".toByteArray(StandardCharsets.UTF_8) + csv.toByteArray(StandardCharsets.UTF_8)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
                output.flush()
            } ?: error("无法创建导出文件")
            events.size
        }
    }
}

object CsvFormat {
    fun buildCsv(
        header: String,
        nameById: Map<String, String>,
        events: List<CheckInEventEntity>,
    ): String {
        val sb = StringBuilder()
        sb.append(header).append('\n')
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val zone = ZoneId.systemDefault()
        events.forEach { event ->
            val localTime = Instant.ofEpochMilli(event.occurredAtEpochMillis)
                .atZone(zone)
                .toLocalTime()
            sb.append(csvCell(nameById[event.habitId] ?: event.habitId)).append(',')
                .append(csvCell(event.localDate)).append(',')
                .append(csvCell(localTime.format(timeFormatter))).append(',')
                .append(if (event.isBackfilled) "1" else "0").append(',')
                .append(csvCell(event.note ?: "")).append('\n')
        }
        return sb.toString()
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
