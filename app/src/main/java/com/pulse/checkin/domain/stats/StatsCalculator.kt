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

    fun buildYearSnapshot(
        habits: List<Habit>,
        events: List<CheckInEvent>,
        year: Int,
        selectedHabitId: String?,
        today: LocalDate,
    ): YearSnapshot
}

data class CheckInRecordItem(
    val id: String,
    val occurredAtEpochMillis: Long,
    val displayTime: LocalTime,
    val isBackfilled: Boolean = false,
)

typealias TodayCheckInRecord = CheckInRecordItem

data class TodayHabitSummary(
    val habit: Habit,
    val todayCount: Int,
    val progress: Float,
    val reachedTarget: Boolean,
    val latestEventAt: Long?,
    val records: List<CheckInRecordItem>,
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
    val records: List<CheckInRecordItem>,
)

data class CalendarDaySummary(
    val date: LocalDate,
    val totalCount: Int,
    val completedHabitCount: Int,
    val hasBackfilledRecord: Boolean,
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

data class YearHabitOption(
    val habit: Habit,
    val yearCount: Int,
    val activeDayCount: Int,
)

data class YearSummaryMetrics(
    val completionDayCount: Int,
    val trackedDayCount: Int,
    val activeDayCount: Int,
    val totalCount: Int,
    val longestStreak: Int,
) {
    companion object {
        val Empty = YearSummaryMetrics(
            completionDayCount = 0,
            trackedDayCount = 0,
            activeDayCount = 0,
            totalCount = 0,
            longestStreak = 0,
        )
    }
}

data class MonthlyTrendPoint(
    val month: YearMonth,
    val totalCount: Int,
    val activeDayCount: Int,
    val longestStreak: Int,
)

data class HourlyDistributionBucket(
    val hour: Int,
    val count: Int,
)

data class MonthlyDetailRow(
    val month: YearMonth,
    val completionRate: Float,
    val activeDayCount: Int,
    val totalCount: Int,
    val longestStreak: Int,
)

data class YearSnapshot(
    val year: Int,
    val habitOptions: List<YearHabitOption>,
    val selectedHabitId: String?,
    val selectedHabit: Habit?,
    val summaryMetrics: YearSummaryMetrics,
    val trendPoints: List<MonthlyTrendPoint>,
    val hourlyDistribution: List<HourlyDistributionBucket>,
    val monthlyDetails: List<MonthlyDetailRow>,
    val hasRecords: Boolean,
) {
    companion object {
        val Empty = YearSnapshot(
            year = LocalDate.now().year,
            habitOptions = emptyList(),
            selectedHabitId = null,
            selectedHabit = null,
            summaryMetrics = YearSummaryMetrics.Empty,
            trendPoints = emptyList(),
            hourlyDistribution = emptyList(),
            monthlyDetails = emptyList(),
            hasRecords = false,
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
        val recordsByHabit = eventsToday.groupBy { it.habitId }
            .mapValues { (_, value) -> value.toRecordItems() }
        val latestEventAtByHabit = events
            .groupBy { it.habitId }
            .mapValues { (_, value) -> value.maxOfOrNull { event -> event.occurredAtEpochMillis } }
        val dailyCounts = buildDailyCounts(events)
        val summaries = habits.sortedBy { it.sortOrder }.map { habit ->
            val count = countsToday[habit.id] ?: 0
            val target = habit.targetCountOrDefault()
            val reachedTarget = reachedGoal(habit, count)
            val progress = when {
                target != null -> (count.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                count > 0 -> 1f
                else -> 0f
            }
            val records = recordsByHabit[habit.id].orEmpty()
            TodayHabitSummary(
                habit = habit,
                todayCount = count,
                progress = progress,
                reachedTarget = reachedTarget,
                latestEventAt = latestEventAtByHabit[habit.id],
                records = records,
            )
        }
        val bestCurrentStreak = habits.maxOfOrNull { habit ->
            computeCurrentStreak(habit, dailyCounts[habit.id].orEmpty(), today)
        } ?: 0
        return TodaySnapshot(
            habits = summaries,
            totalCount = eventsToday.size,
            completedHabits = summaries.count { it.reachedTarget },
            targetHabitCount = habits.size,
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
                    reachedGoal(habit, counts[habit.id] ?: 0)
                },
                hasBackfilledRecord = eventsForDay.any { it.isBackfilled },
            )
        }
        val selectedEvents = groupedByDate[selectedDate].orEmpty()
        val selectedDetails = habits.sortedBy { it.sortOrder }.map { habit ->
            val eventsForHabit = selectedEvents.filter { it.habitId == habit.id }
            HistoryHabitDetail(
                habit = habit,
                count = eventsForHabit.size,
                reachedTarget = reachedGoal(habit, eventsForHabit.size),
                records = eventsForHabit.toRecordItems(),
            )
        }
        val completionDays = calendarDays.sumOf { it.completedHabitCount }
        val dayUpperBound = when {
            month.isAfter(YearMonth.from(today)) -> month.lengthOfMonth()
            month == YearMonth.from(today) -> today.dayOfMonth
            else -> month.lengthOfMonth()
        }
        val trackedHabitDays = habits.size * dayUpperBound
        val anchor = if (selectedDate.isAfter(today)) today else selectedDate
        val bestCurrentStreak = habits.maxOfOrNull { habit ->
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

    override fun buildYearSnapshot(
        habits: List<Habit>,
        events: List<CheckInEvent>,
        year: Int,
        selectedHabitId: String?,
        today: LocalDate,
    ): YearSnapshot {
        val sortedHabits = habits.sortedBy { it.sortOrder }
        val yearEvents = events.filter { it.localDate.year == year }
        val dailyCounts = buildDailyCounts(events)
        val habitOptions = sortedHabits.map { habit ->
            val eventsForHabit = yearEvents.filter { it.habitId == habit.id }
            YearHabitOption(
                habit = habit,
                yearCount = eventsForHabit.size,
                activeDayCount = eventsForHabit.map { it.localDate }.distinct().size,
            )
        }
        val selectedHabit = sortedHabits.firstOrNull { it.id == selectedHabitId } ?: sortedHabits.firstOrNull()
        if (selectedHabit == null) {
            return YearSnapshot.Empty.copy(year = year)
        }
        val selectedEvents = yearEvents.filter { it.habitId == selectedHabit.id }
        val trendPoints = (1..12).map { monthValue ->
            val month = YearMonth.of(year, monthValue)
            val monthEvents = selectedEvents.filter { YearMonth.from(it.localDate) == month }
            val activeDates = monthEvents.map { it.localDate }.distinct()
            MonthlyTrendPoint(
                month = month,
                totalCount = monthEvents.size,
                activeDayCount = activeDates.size,
                longestStreak = computeLongestStreak(activeDates),
            )
        }
        val hourlyDistribution = (0..23).map { hour ->
            HourlyDistributionBucket(
                hour = hour,
                count = selectedEvents.count { event ->
                    Instant.ofEpochMilli(event.occurredAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .hour == hour
                },
            )
        }
        val selectedDailyCounts = dailyCounts[selectedHabit.id].orEmpty()
            .filterKeys { date -> date.year == year }
        val dayUpperBound = LocalDate.of(year, 12, 31).dayOfYear
        val completionDayCount = if (selectedHabit.targetCountOrDefault() != null) {
            selectedDailyCounts.values.count { count ->
                count >= (selectedHabit.targetCountOrDefault() ?: 1)
            }
        } else {
            selectedDailyCounts.size
        }
        val metrics = YearSummaryMetrics(
            completionDayCount = completionDayCount,
            trackedDayCount = dayUpperBound,
            activeDayCount = selectedEvents.map { it.localDate }.distinct().size,
            totalCount = selectedEvents.size,
            longestStreak = computeLongestStreak(selectedEvents.map { it.localDate }.distinct()),
        )
        val monthlyDetails = trendPoints.map { point ->
            val monthDailyCounts = selectedDailyCounts.filterKeys { date ->
                YearMonth.from(date) == point.month
            }
            val monthCompletionDays = if (selectedHabit.targetCountOrDefault() != null) {
                monthDailyCounts.values.count { count ->
                    count >= (selectedHabit.targetCountOrDefault() ?: 1)
                }
            } else {
                monthDailyCounts.size
            }
            MonthlyDetailRow(
                month = point.month,
                completionRate = monthCompletionDays.toFloat() / point.month.lengthOfMonth().toFloat(),
                activeDayCount = point.activeDayCount,
                totalCount = point.totalCount,
                longestStreak = point.longestStreak,
            )
        }
        return YearSnapshot(
            year = year,
            habitOptions = habitOptions,
            selectedHabitId = selectedHabit.id,
            selectedHabit = selectedHabit,
            summaryMetrics = metrics,
            trendPoints = trendPoints,
            hourlyDistribution = hourlyDistribution,
            monthlyDetails = monthlyDetails,
            hasRecords = selectedEvents.isNotEmpty(),
        )
    }

    private fun buildDailyCounts(events: List<CheckInEvent>): Map<String, Map<LocalDate, Int>> {
        return events.groupBy { it.habitId }
            .mapValues { (_, value) -> value.groupingBy { it.localDate }.eachCount() }
    }

    private fun computeCurrentStreak(
        habit: Habit,
        countsByDate: Map<LocalDate, Int>,
        anchor: LocalDate,
    ): Int {
        var streak = 0
        var currentDate = anchor
        while (reachedGoal(habit, countsByDate[currentDate] ?: 0)) {
            streak += 1
            currentDate = currentDate.minusDays(1)
        }
        return streak
    }

    private fun reachedGoal(habit: Habit, count: Int): Boolean {
        val target = habit.targetCountOrDefault()
        return if (target != null) count >= target else count > 0
    }

    private fun computeLongestStreak(dates: Collection<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sortedDates = dates.distinct().sorted()
        var best = 1
        var current = 1
        for (index in 1 until sortedDates.size) {
            current = if (sortedDates[index - 1].plusDays(1) == sortedDates[index]) {
                current + 1
            } else {
                1
            }
            if (current > best) best = current
        }
        return best
    }

    private fun List<CheckInEvent>.toRecordItems(): List<CheckInRecordItem> {
        return sortedByDescending { it.occurredAtEpochMillis }
            .map { event ->
                CheckInRecordItem(
                    id = event.id,
                    occurredAtEpochMillis = event.occurredAtEpochMillis,
                    displayTime = Instant.ofEpochMilli(event.occurredAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime(),
                    isBackfilled = event.isBackfilled,
                )
            }
    }
}
