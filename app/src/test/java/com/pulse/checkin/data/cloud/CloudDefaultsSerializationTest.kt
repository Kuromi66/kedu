package com.pulse.checkin.data.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudDefaultsSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val dayEvent = DayEventDto(
        id = "id-1",
        name = "147",
        eventDate = "2026-09-14",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    @Test
    fun `day event with default field values always serializes sortOrder and flag fields`() {
        val serialized = json.encodeToString(
            SyncRequest.serializer(),
            SyncRequest(since = 0L, habits = emptyList(), events = emptyList(), dayEvents = listOf(dayEvent)),
        )

        val firstDayEvent = json.parseToJsonElement(serialized)
            .jsonObject
            .getValue("dayEvents")
            .jsonArray
            .single()
            .jsonObject

        assertEquals(dayEvent.sortOrder, firstDayEvent.getValue("sortOrder").jsonPrimitive.content.toInt())
        assertTrue(firstDayEvent.containsKey("calendarType"))
        assertTrue(firstDayEvent.containsKey("reminderEnabled"))
        assertTrue(firstDayEvent.containsKey("archived"))
        assertTrue(firstDayEvent.containsKey("note"))
    }
}
