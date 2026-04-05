package com.pulse.checkin.domain.stats

import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.Habit
import java.time.LocalDate
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
        assertFalse(snapshot.habits.single().reachedTarget)
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

        assertEquals(listOf(3000L, 2000L, 1000L), records.map { it.occurredAtEpochMillis })
        assertEquals(listOf(3L, 2L, 1L), records.map { it.id })
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

    private fun habit(targetEnabled: Boolean, targetCount: Int? = null): Habit = Habit(
        id = 1,
        name = "Drink Water",
        colorArgb = 0xFF2446FF,
        glyph = "W",
        targetEnabled = targetEnabled,
        dailyTargetCount = targetCount,
    )

    private fun event(habitId: Long, date: LocalDate, millis: Long): CheckInEvent = CheckInEvent(
        id = millis / 1000,
        habitId = habitId,
        occurredAtEpochMillis = millis,
        localDate = date,
    )
}
