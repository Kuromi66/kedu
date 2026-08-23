package com.pulse.checkin.domain.stats

import com.pulse.checkin.domain.model.DayEvent
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface DayCountResult {
    data class DaysUntil(val days: Long) : DayCountResult
    data class DaysSince(val days: Long) : DayCountResult
    data object Today : DayCountResult
}

object DayCountCalculator {
    fun compute(event: DayEvent, today: LocalDate): DayCountResult {
        val target = nextOccurrence(event.date, event.repeatsYearly, today)
        val days = ChronoUnit.DAYS.between(today, target)
        return when {
            days > 0 -> DayCountResult.DaysUntil(days)
            days < 0 -> DayCountResult.DaysSince(-days)
            else -> DayCountResult.Today
        }
    }

    fun nextOccurrence(date: LocalDate, repeatsYearly: Boolean, today: LocalDate): LocalDate {
        if (!repeatsYearly) return date
        var candidate = adjustForYear(date, today.year)
        if (candidate.isBefore(today)) {
            candidate = adjustForYear(date, today.year + 1)
        }
        return candidate
    }

    fun sortForDisplay(events: List<DayEvent>, today: LocalDate): List<DayEvent> {
        val upcoming = events
            .filter { compute(it, today) !is DayCountResult.DaysSince }
            .sortedBy { nextOccurrence(it.date, it.repeatsYearly, today) }
        val past = events
            .filter { compute(it, today) is DayCountResult.DaysSince }
            .sortedByDescending { it.date }
        return upcoming + past
    }

    private fun adjustForYear(date: LocalDate, year: Int): LocalDate {
        val isFeb29 = date.monthValue == 2 && date.dayOfMonth == 29
        val isLeap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        return if (isFeb29 && !isLeap) {
            LocalDate.of(year, 2, 28)
        } else {
            date.withYear(year)
        }
    }
}
