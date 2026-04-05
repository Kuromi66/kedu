package com.pulse.checkin.domain.stats

import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.Habit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

interface StatsCalculator {
    fun buildTodaySnapshot(habits: List<Habit>, events: List<CheckInEvent>, today: LocalDate): TodaySnapshot
    fun buildMonthSnapshot(
        habits: List<Habit>,
        events: List<CheckInEvent>,
        month: YearMonth,
        selectedDate: LocalDate,
        today: LocalDate,
    ): MonthSnapshot
}

data class TodayHabitSummary(
    val habit: Habit,
    val todayCount: Int,
    val progress: Float,
    val reachedTarget: Boolean,
    val latestEventAt: Long?,
)

data class TodaySnapshot(
    val habits: List<TodayHabitSummary>,
    val totalCount: Int,
    val completedHabits: Int,
    val targetHabitCount: Int,
    val bestCurrentStreak: Int,
) {
    companion object {
        val Empty = TodaySnapshot(
            habits = emptyList(),
            totalCount = 0,
            completedHabits = 0,
            targetHabitCount = 0,
            bestCurrentStreak = 0,
        )
    }
}

data class HistoryHabitDetail(
    val habit: Habit,
    val count: Int,
    val reachedTarget: Boolean,
    val eventTimes: List<LocalTime>,
)

data class CalendarDaySummary(
    val date: LocalDate,
    val totalCount: Int,
    val completedHabitCount: Int,
)

data class MonthSnapshot(
    val month: YearMonth,
    val calendarDays: List<CalendarDaySummary>,
    val monthTotalCount: Int,
    val completedHabitDays: Int,
    val targetHabitDays: Int,
    val bestCurrentStreak: Int,
    val selectedDateDetails: List<HistoryHabitDetail>,
) {
    companion object {
        val Empty = MonthSnapshot(
            month = YearMonth.now(),
            calendarDays = emptyList(),
            monthTotalCount = 0,
            completedHabitDays = 0,
            targetHabitDays = 0,
            bestCurrentStreak = 0,
            selectedDateDetails = emptyList(),
        )
    }
}

class LocalStatsCalculator : StatsCalculator {
    override fun buildTodaySnapshot(
        habits: List<Habit>,
        events: List<CheckInEvent>,
        today: LocalDate,
    ): TodaySnapshot {
        val eventsToday = events.filter { it.localDate == today }
        val countsToday = eventsToday.groupingBy { it.habitId }.eachCount()
        val latestByHabit = eventsToday.groupBy { it.habitId }
            .mapValues { (_, value) -> value.maxOfOrNull { it.occurredAtEpochMillis } }
        val dailyCounts = buildDailyCounts(events)
        val targetHabits = habits.filter { it.targetEnabled }
        val summaries = habits.sortedBy { it.sortOrder }.map { habit ->
            val count = countsToday[habit.id] ?: 0
            val target = habit.targetCountOrDefault()
            val reachedTarget = target != null && count >= target
            val progress = when {
                target != null -> (count.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                count > 0 -> 1f
                else -> 0f
            }
            TodayHabitSummary(
                habit = habit,
                todayCount = count,
                progress = progress,
                reachedTarget = reachedTarget,
                latestEventAt = latestByHabit[habit.id],
            )
        }
        val bestCurrentStreak = targetHabits.maxOfOrNull { habit ->
            computeCurrentStreak(habit, dailyCounts[habit.id].orEmpty(), today)
        } ?: 0
        return TodaySnapshot(
            habits = summaries,
            totalCount = eventsToday.size,
            completedHabits = summaries.count { it.reachedTarget },
            targetHabitCount = targetHabits.size,
            bestCurrentStreak = bestCurrentStreak,
        )
    }

    override fun buildMonthSnapshot(
        habits: List<Habit>,
        events: List<CheckInEvent>,
        month: YearMonth,
        selectedDate: LocalDate,
        today: LocalDate,
    ): MonthSnapshot {
        val monthEvents = events.filter { YearMonth.from(it.localDate) == month }
        val groupedByDate = monthEvents.groupBy { it.localDate }
        val dailyCounts = buildDailyCounts(events)
        val calendarDays = (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            val eventsForDay = groupedByDate[date].orEmpty()
            val counts = eventsForDay.groupingBy { it.habitId }.eachCount()
            CalendarDaySummary(
                date = date,
                totalCount = eventsForDay.size,
                completedHabitCount = habits.count { habit ->
                    val target = habit.targetCountOrDefault()
                    target != null && (counts[habit.id] ?: 0) >= target
                },
            )
        }
        val selectedEvents = groupedByDate[selectedDate].orEmpty()
        val selectedDetails = habits.sortedBy { it.sortOrder }.map { habit ->
            val eventsForHabit = selectedEvents.filter { it.habitId == habit.id }
            val target = habit.targetCountOrDefault()
            HistoryHabitDetail(
                habit = habit,
                count = eventsForHabit.size,
                reachedTarget = target != null && eventsForHabit.size >= target,
                eventTimes = eventsForHabit
                    .sortedByDescending { it.occurredAtEpochMillis }
                    .map { event ->
                        Instant.ofEpochMilli(event.occurredAtEpochMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                    },
            )
        }
        val targetHabits = habits.filter { it.targetEnabled }
        val completionDays = calendarDays.sumOf { it.completedHabitCount }
        val dayUpperBound = when {
            month.isAfter(YearMonth.from(today)) -> month.lengthOfMonth()
            month == YearMonth.from(today) -> today.dayOfMonth
            else -> month.lengthOfMonth()
        }
        val trackedHabitDays = targetHabits.size * dayUpperBound
        val anchor = if (selectedDate.isAfter(today)) today else selectedDate
        val bestCurrentStreak = targetHabits.maxOfOrNull { habit ->
            computeCurrentStreak(habit, dailyCounts[habit.id].orEmpty(), anchor)
        } ?: 0
        return MonthSnapshot(
            month = month,
            calendarDays = calendarDays,
            monthTotalCount = monthEvents.size,
            completedHabitDays = completionDays,
            targetHabitDays = trackedHabitDays,
            bestCurrentStreak = bestCurrentStreak,
            selectedDateDetails = selectedDetails,
        )
    }

    private fun buildDailyCounts(events: List<CheckInEvent>): Map<Long, Map<LocalDate, Int>> {
        return events.groupBy { it.habitId }
            .mapValues { (_, value) -> value.groupingBy { it.localDate }.eachCount() }
    }

    private fun computeCurrentStreak(
        habit: Habit,
        countsByDate: Map<LocalDate, Int>,
        anchor: LocalDate,
    ): Int {
        val target = habit.targetCountOrDefault() ?: return 0
        var streak = 0
        var currentDate = anchor
        while ((countsByDate[currentDate] ?: 0) >= target) {
            streak += 1
            currentDate = currentDate.minusDays(1)
        }
        return streak
    }
}
