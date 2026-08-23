package com.pulse.checkin.domain.stats

import com.pulse.checkin.domain.model.CalendarType
import com.pulse.checkin.domain.model.DayEvent
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DayCountCalculatorTest {
    private val today = LocalDate.of(2026, 8, 23)
    private val fakeLunar = FakeLunarCalendar()

    private fun event(
        date: LocalDate,
        repeatsYearly: Boolean = false,
        calendarType: CalendarType = CalendarType.SOLAR,
        lunarMonth: Int? = null,
        lunarDay: Int? = null,
        lunarLeap: Boolean = false,
    ): DayEvent = DayEvent(
        id = "e-1",
        name = "测试",
        date = date,
        repeatsYearly = repeatsYearly,
        calendarType = calendarType,
        lunarMonth = lunarMonth,
        lunarDay = lunarDay,
        lunarLeap = lunarLeap,
    )

    @Test
    fun `one time future date shows days until`() {
        val result = DayCountCalculator.compute(event(today.plusDays(5)), today, fakeLunar)
        assertEquals(DayCountResult.DaysUntil(5), result)
    }

    @Test
    fun `one time past date shows days since`() {
        val result = DayCountCalculator.compute(event(today.minusDays(7)), today, fakeLunar)
        assertEquals(DayCountResult.DaysSince(7), result)
    }

    @Test
    fun `one time today shows today`() {
        assertEquals(DayCountResult.Today, DayCountCalculator.compute(event(today), today, fakeLunar))
    }

    @Test
    fun `yearly event later this year counts to this year`() {
        val result = DayCountCalculator.compute(event(LocalDate.of(2020, 12, 25), repeatsYearly = true), today, fakeLunar)
        assertEquals(DayCountResult.DaysUntil(124), result)
    }

    @Test
    fun `yearly event already passed counts to next year`() {
        val result = DayCountCalculator.compute(event(LocalDate.of(2020, 1, 1), repeatsYearly = true), today, fakeLunar)
        assertEquals(DayCountResult.DaysUntil(131), result)
    }

    @Test
    fun `yearly event today is today`() {
        val result = DayCountCalculator.compute(event(today.minusYears(3), repeatsYearly = true), today, fakeLunar)
        assertEquals(DayCountResult.Today, result)
    }

    @Test
    fun `feb 29 yearly event rolls to feb 28 in non leap year`() {
        val next = DayCountCalculator.nextOccurrence(
            event(LocalDate.of(2020, 2, 29), repeatsYearly = true),
            LocalDate.of(2026, 2, 1),
            fakeLunar,
        )
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun `feb 29 yearly event stays feb 29 in leap year`() {
        val next = DayCountCalculator.nextOccurrence(
            event(LocalDate.of(2020, 2, 29), repeatsYearly = true),
            LocalDate.of(2028, 2, 1),
            fakeLunar,
        )
        assertEquals(LocalDate.of(2028, 2, 29), next)
    }

    @Test
    fun `lunar yearly event counts to lunar date this year`() {
        val result = DayCountCalculator.compute(
            event(
                date = LocalDate.of(2000, 9, 15),
                repeatsYearly = true,
                calendarType = CalendarType.LUNAR,
                lunarMonth = 8,
                lunarDay = 15,
            ),
            today,
            fakeLunar,
        )
        assertEquals(DayCountResult.DaysUntil(23), result)
    }

    @Test
    fun `lunar yearly event already passed counts to next year`() {
        val laterToday = LocalDate.of(2026, 10, 1)
        val result = DayCountCalculator.compute(
            event(
                date = LocalDate.of(2000, 9, 15),
                repeatsYearly = true,
                calendarType = CalendarType.LUNAR,
                lunarMonth = 8,
                lunarDay = 15,
            ),
            laterToday,
            fakeLunar,
        )
        assertEquals(DayCountResult.DaysUntil(349), result)
    }

    @Test
    fun `sort places upcoming before past`() {
        val past = event(today.minusDays(3))
        val soon = event(today.plusDays(2))
        val later = event(today.plusDays(10))
        val sorted = DayCountCalculator.sortForDisplay(listOf(past, later, soon), today, fakeLunar)
        assertEquals(listOf(soon.id, later.id, past.id), sorted.map { it.id })
    }

    @Test
    fun `one time future event progress counts from creation`() {
        val created = today.minusDays(10)
        val event = DayEvent(
            id = "e-1",
            name = "测试",
            date = today.plusDays(10),
            createdAtEpochMillis = created.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        val progress = DayCountCalculator.progress(event, today, fakeLunar)
        assertEquals(0.5f, progress!!, 0.01f)
    }

    @Test
    fun `yearly future event progress approaches full`() {
        val event = event(LocalDate.of(2000, 9, 15), repeatsYearly = true)
        val progress = DayCountCalculator.progress(event, today, fakeLunar)
        assertEquals(342f / 365f, progress!!, 0.01f)
    }

    @Test
    fun `lunar yearly future event progress approaches full`() {
        val event = event(
            date = LocalDate.of(2000, 9, 15),
            repeatsYearly = true,
            calendarType = CalendarType.LUNAR,
            lunarMonth = 8,
            lunarDay = 15,
        )
        val progress = DayCountCalculator.progress(event, today, fakeLunar)
        assertEquals(342f / 365f, progress!!, 0.01f)
    }

    @Test
    fun `past one time event has no progress`() {
        assertNull(DayCountCalculator.progress(event(today.minusDays(3)), today, fakeLunar))
    }

    private class FakeLunarCalendar : LunarCalendar {
        override fun toLunar(gregorian: LocalDate): LunarDate {
            return if (gregorian.monthValue == 9 && gregorian.dayOfMonth == 15) {
                LunarDate(month = 8, day = 15)
            } else {
                LunarDate(month = gregorian.monthValue, day = gregorian.dayOfMonth)
            }
        }

        override fun toGregorian(year: Int, lunar: LunarDate): LocalDate {
            return if (lunar.month == 8 && lunar.day == 15) {
                LocalDate.of(year, 9, 15)
            } else {
                LocalDate.of(year, lunar.month.coerceIn(1, 12), lunar.day.coerceIn(1, 28))
            }
        }
    }
}
