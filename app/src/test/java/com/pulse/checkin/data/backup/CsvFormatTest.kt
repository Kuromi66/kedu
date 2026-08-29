package com.pulse.checkin.data.backup

import com.pulse.checkin.data.db.entity.CheckInEventEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvFormatTest {
    private val zone = ZoneId.systemDefault()

    private fun event(
        habitId: String,
        date: LocalDate,
        hour: Int,
        minute: Int,
        backfilled: Boolean = false,
        note: String? = null,
    ): CheckInEventEntity {
        val millis = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        return CheckInEventEntity(
            id = "e-$habitId-$date-$hour",
            habitId = habitId,
            occurredAtEpochMillis = millis,
            localDate = date.toString(),
            isBackfilled = backfilled,
            note = note,
        )
    }

    @Test
    fun `csv contains header and escaped rows`() {
        val events = listOf(
            event("h-1", LocalDate.of(2026, 8, 23), 9, 5, note = "含,逗号\"和引号"),
            event("h-2", LocalDate.of(2026, 8, 24), 21, 30, backfilled = true),
        )
        val csv = CsvFormat.buildCsv(
            header = "Habit,Date,Time,Backfilled,Note",
            nameById = mapOf("h-1" to "喝水,习惯", "h-2" to "阅读"),
            events = events,
        )
        val lines = csv.trimEnd().lines()
        assertEquals("Habit,Date,Time,Backfilled,Note", lines[0])
        assertTrue(lines[1].contains("\"喝水,习惯\""))
        assertTrue(lines[1].contains("\"含,逗号\"\"和引号\""))
        assertTrue(lines[1].contains(",\"09:05\",0,"))
        assertTrue(lines[2].contains(",\"21:30\",1,"))
    }
}
