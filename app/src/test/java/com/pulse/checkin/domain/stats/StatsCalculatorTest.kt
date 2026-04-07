package com.pulse.checkin.domain.stats

import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.Habit
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsCalculatorTest {
    private val calculator = LocalStatsCalculator()
    private val today = LocalDate.of(2026, 4, 5)

    @Test
    fun `today snapshot aggregates repeated check-ins`() {
        val habit = habit(targetEnabled = false)
        val events = listOf(
            event(habitId = 1, date = today, millis = 1),
            event(habitId = 1, date = today, millis = 2),
            event(habitId = 1, date = today, millis = 3),
        )

        val snapshot = calculator.buildTodaySnapshot(listOf(habit), events, today)

        assertEquals(3, snapshot.totalCount)
        assertEquals(3, snapshot.habits.single().todayCount)
        assertTrue(snapshot.habits.single().reachedTarget)
        assertEquals(1, snapshot.completedHabits)
    }

    @Test
    fun `today snapshot exposes same-day records in descending time order`() {
        val habit = habit(targetEnabled = false)
        val events = listOf(
            event(habitId = 1, date = today, millis = 1000),
            event(habitId = 1, date = today, millis = 3000),
            event(habitId = 1, date = today, millis = 2000),
        )

        val records = calculator.buildTodaySnapshot(listOf(habit), events, today).habits.single().records

        assertEquals(records.map { it.occurredAtEpochMillis }.sortedDescending(), records.map { it.occurredAtEpochMillis })
        assertEquals(listOf(3000L, 2000L, 1000L), records.map { it.id })
    }

    @Test
    fun `target habit is completed only when count reaches threshold`() {
        val habit = habit(targetEnabled = true, targetCount = 4)
        val events = listOf(
            event(habitId = 1, date = today, millis = 1),
            event(habitId = 1, date = today, millis = 2),
            event(habitId = 1, date = today, millis = 3),
            event(habitId = 1, date = today, millis = 4),
        )

        val snapshot = calculator.buildTodaySnapshot(listOf(habit), events, today)

        assertTrue(snapshot.habits.single().reachedTarget)
        assertEquals(1, snapshot.completedHabits)
        assertEquals(1, snapshot.targetHabitCount)
    }

    @Test
    fun `current streak counts consecutive completed days`() {
        val habit = habit(targetEnabled = true, targetCount = 2)
        val events = listOf(
            event(1, today, 1),
            event(1, today, 2),
            event(1, today.minusDays(1), 3),
            event(1, today.minusDays(1), 4),
            event(1, today.minusDays(2), 5),
            event(1, today.minusDays(2), 6),
            event(1, today.minusDays(4), 7),
            event(1, today.minusDays(4), 8),
        )

        val snapshot = calculator.buildTodaySnapshot(listOf(habit), events, today)

        assertEquals(3, snapshot.bestCurrentStreak)
    }

    @Test
    fun `month snapshot computes details and completion rate inputs`() {
        val habit = habit(targetEnabled = true, targetCount = 2)
        val month = YearMonth.from(today)
        val events = listOf(
            event(1, today, 1),
            event(1, today, 2),
            event(1, today.minusDays(1), 3),
            event(1, today.minusDays(1), 4),
        )

        val snapshot = calculator.buildMonthSnapshot(
            habits = listOf(habit),
            events = events,
            month = month,
            selectedDate = today,
            today = today,
        )

        assertEquals(4, snapshot.monthTotalCount)
        assertEquals(2, snapshot.completedHabitDays)
        assertEquals(today.dayOfMonth, snapshot.targetHabitDays)
        assertEquals(2, snapshot.selectedDateDetails.single().count)
    }


    @Test
    fun `month snapshot keeps backfilled record flag`() {
        val habit = habit(targetEnabled = false)
        val month = YearMonth.from(today)
        val events = listOf(
            event(1, today, 1, isBackfilled = false),
            event(1, today, 2, isBackfilled = true),
        )

        val records = calculator.buildMonthSnapshot(
            habits = listOf(habit),
            events = events,
            month = month,
            selectedDate = today,
            today = today,
        ).selectedDateDetails.single().records

        assertEquals(listOf(true, false), records.map { it.isBackfilled })
    }


    @Test
    fun `month snapshot marks calendar day with backfilled record`() {
        val habit = habit(targetEnabled = false)
        val month = YearMonth.from(today)
        val events = listOf(
            event(1, today, 1, isBackfilled = true),
            event(1, today.minusDays(1), 2, isBackfilled = false),
        )

        val calendarDays = calculator.buildMonthSnapshot(
            habits = listOf(habit),
            events = events,
            month = month,
            selectedDate = today,
            today = today,
        ).calendarDays

        assertTrue(calendarDays.first { it.date == today }.hasBackfilledRecord)
        assertFalse(calendarDays.first { it.date == today.minusDays(1) }.hasBackfilledRecord)
    }

    @Test
    fun `year snapshot aggregates monthly totals and active days`() {
        val habit = habit(targetEnabled = false)
        val events = listOf(
            event(1, LocalDate.of(2026, 1, 2), 1),
            event(1, LocalDate.of(2026, 1, 2), 2),
            event(1, LocalDate.of(2026, 2, 3), 3),
            event(1, LocalDate.of(2026, 2, 4), 4),
        )

        val snapshot = calculator.buildYearSnapshot(listOf(habit), events, 2026, 1, today)

        assertEquals(4, snapshot.summaryMetrics.totalCount)
        assertEquals(3, snapshot.summaryMetrics.activeDayCount)
        assertEquals(365, snapshot.summaryMetrics.trackedDayCount)
        assertEquals(3, snapshot.summaryMetrics.completionDayCount)
        assertEquals(2, snapshot.monthlyDetails[0].totalCount)
        assertEquals(2, snapshot.monthlyDetails[1].totalCount)
        assertEquals(2, snapshot.monthlyDetails[1].activeDayCount)
    }


    @Test
    fun `year completion uses active days for non target habit`() {
        val habit = habit(targetEnabled = false)
        val events = listOf(
            event(1, LocalDate.of(2026, 1, 2), 1),
            event(1, LocalDate.of(2026, 1, 2), 2),
            event(1, LocalDate.of(2026, 1, 5), 3),
        )

        val snapshot = calculator.buildYearSnapshot(listOf(habit), events, 2026, 1, today)

        assertEquals(2, snapshot.summaryMetrics.completionDayCount)
        assertEquals(365, snapshot.summaryMetrics.trackedDayCount)
    }

    @Test
    fun `year completion uses reached days for target habit`() {
        val habit = habit(targetEnabled = true, targetCount = 2)
        val events = listOf(
            event(1, LocalDate.of(2026, 1, 2), 1),
            event(1, LocalDate.of(2026, 1, 2), 2),
            event(1, LocalDate.of(2026, 1, 5), 3),
        )

        val snapshot = calculator.buildYearSnapshot(listOf(habit), events, 2026, 1, today)

        assertEquals(1, snapshot.summaryMetrics.completionDayCount)
        assertEquals(365, snapshot.summaryMetrics.trackedDayCount)
    }


    @Test
    fun `year completion only counts days in selected year`() {
        val habit = habit(targetEnabled = false)
        val events = listOf(
            event(1, LocalDate.of(2025, 12, 31), 1),
            event(1, LocalDate.of(2026, 1, 2), 2),
            event(1, LocalDate.of(2026, 1, 5), 3),
        )

        val snapshot = calculator.buildYearSnapshot(listOf(habit), events, 2026, 1, today)

        assertEquals(2, snapshot.summaryMetrics.completionDayCount)
        assertEquals(2, snapshot.summaryMetrics.activeDayCount)
    }

    @Test
    fun `year snapshot longest streak for non target habit uses active days`() {
        val habit = habit(targetEnabled = false)
        val events = listOf(
            event(1, LocalDate.of(2026, 3, 1), 1),
            event(1, LocalDate.of(2026, 3, 2), 2),
            event(1, LocalDate.of(2026, 3, 3), 3),
            event(1, LocalDate.of(2026, 3, 5), 4),
        )

        val snapshot = calculator.buildYearSnapshot(listOf(habit), events, 2026, 1, today)

        assertEquals(3, snapshot.summaryMetrics.longestStreak)
        assertEquals(3, snapshot.monthlyDetails[2].longestStreak)
    }

    @Test
    fun `year snapshot groups hourly distribution by local hour`() {
        val habit = habit(targetEnabled = false)
        val events = listOf(
            event(1, LocalDate.of(2026, 4, 1), 1, hour = 0, minute = 10),
            event(1, LocalDate.of(2026, 4, 1), 2, hour = 0, minute = 55),
            event(1, LocalDate.of(2026, 4, 1), 3, hour = 23, minute = 5),
        )

        val snapshot = calculator.buildYearSnapshot(listOf(habit), events, 2026, 1, today)

        assertEquals(2, snapshot.hourlyDistribution.first { it.hour == 0 }.count)
        assertEquals(1, snapshot.hourlyDistribution.first { it.hour == 23 }.count)
    }

    @Test
    fun `year snapshot exposes all months even without records`() {
        val habit = habit(targetEnabled = false)

        val snapshot = calculator.buildYearSnapshot(listOf(habit), emptyList(), 2026, 1, today)

        assertEquals(12, snapshot.trendPoints.size)
        assertEquals(12, snapshot.monthlyDetails.size)
        assertTrue(snapshot.monthlyDetails.all { it.totalCount == 0 })
        assertFalse(snapshot.hasRecords)
    }

    private fun habit(targetEnabled: Boolean, targetCount: Int? = null): Habit = Habit(
        id = 1,
        name = "Drink Water",
        colorArgb = 0xFF2446FF,
        glyph = "W",
        targetEnabled = targetEnabled,
        dailyTargetCount = targetCount,
    )

    private fun event(
        habitId: Long,
        date: LocalDate,
        millis: Long,
        hour: Int = 0,
        minute: Int = 0,
        isBackfilled: Boolean = false,
    ): CheckInEvent {
        val epochMillis = date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + millis
        return CheckInEvent(
            id = millis,
            habitId = habitId,
            occurredAtEpochMillis = epochMillis,
            localDate = date,
            isBackfilled = isBackfilled,
        )
    }
}
