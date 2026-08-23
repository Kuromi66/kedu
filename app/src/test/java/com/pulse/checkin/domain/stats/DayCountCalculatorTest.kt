package com.pulse.checkin.domain.stats

import com.pulse.checkin.domain.model.DayEvent
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DayCountCalculatorTest {
    private val today = LocalDate.of(2026, 8, 23)

    private fun event(date: LocalDate, repeatsYearly: Boolean = false): DayEvent = DayEvent(
        id = "e-1",
        name = "测试",
        date = date,
        repeatsYearly = repeatsYearly,
    )

    @Test
    fun `one time future date shows days until`() {
        val result = DayCountCalculator.compute(event(today.plusDays(5)), today)
        assertEquals(DayCountResult.DaysUntil(5), result)
    }

    @Test
    fun `one time past date shows days since`() {
        val result = DayCountCalculator.compute(event(today.minusDays(7)), today)
        assertEquals(DayCountResult.DaysSince(7), result)
    }

    @Test
    fun `one time today shows today`() {
        assertEquals(DayCountResult.Today, DayCountCalculator.compute(event(today), today))
    }

    @Test
    fun `yearly event later this year counts to this year`() {
        val result = DayCountCalculator.compute(event(LocalDate.of(2020, 12, 25), repeatsYearly = true), today)
        assertEquals(DayCountResult.DaysUntil(124), result)
    }

    @Test
    fun `yearly event already passed counts to next year`() {
        val result = DayCountCalculator.compute(event(LocalDate.of(2020, 1, 1), repeatsYearly = true), today)
        assertEquals(DayCountResult.DaysUntil(131), result)
    }

    @Test
    fun `yearly event today is today`() {
        val result = DayCountCalculator.compute(event(today.minusYears(3), repeatsYearly = true), today)
        assertEquals(DayCountResult.Today, result)
    }

    @Test
    fun `feb 29 yearly event rolls to feb 28 in non leap year`() {
        val next = DayCountCalculator.nextOccurrence(
            LocalDate.of(2020, 2, 29),
            repeatsYearly = true,
            today = LocalDate.of(2026, 2, 1),
        )
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun `feb 29 yearly event stays feb 29 in leap year`() {
        val next = DayCountCalculator.nextOccurrence(
            LocalDate.of(2020, 2, 29),
            repeatsYearly = true,
            today = LocalDate.of(2028, 2, 1),
        )
        assertEquals(LocalDate.of(2028, 2, 29), next)
    }

    @Test
    fun `sort places upcoming before past`() {
        val past = event(today.minusDays(3))
        val soon = event(today.plusDays(2))
        val later = event(today.plusDays(10))
        val sorted = DayCountCalculator.sortForDisplay(listOf(past, later, soon), today)
        assertEquals(listOf(soon.id, later.id, past.id), sorted.map { it.id })
    }
}
