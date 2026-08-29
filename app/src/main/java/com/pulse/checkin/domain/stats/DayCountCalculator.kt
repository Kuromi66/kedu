package com.pulse.checkin.domain.stats

import com.pulse.checkin.domain.model.DayEvent
import com.pulse.checkin.domain.model.CalendarType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

sealed interface DayCountResult {
    data class DaysUntil(val days: Long) : DayCountResult
    data class DaysSince(val days: Long) : DayCountResult
    data object Today : DayCountResult
}

object DayCountCalculator {
    fun compute(event: DayEvent, today: LocalDate, lunar: LunarCalendar): DayCountResult {
        val target = nextOccurrence(event, today, lunar)
        val days = ChronoUnit.DAYS.between(today, target)
        return when {
            days > 0 -> DayCountResult.DaysUntil(days)
            days < 0 -> DayCountResult.DaysSince(-days)
            else -> DayCountResult.Today
        }
    }

    fun nextOccurrence(event: DayEvent, today: LocalDate, lunar: LunarCalendar): LocalDate {
        if (event.repeatsMonthly) return monthlyOccurrence(event.date.dayOfMonth, today)
        if (!event.repeatsYearly) return event.date
        return when (event.calendarType) {
            CalendarType.SOLAR -> solarNextOccurrence(event.date, today)
            CalendarType.LUNAR -> lunarNextOccurrence(event, today, lunar)
        }
    }

    fun sortForDisplay(events: List<DayEvent>, today: LocalDate, lunar: LunarCalendar): List<DayEvent> {
        val upcoming = events
            .filter { compute(it, today, lunar) !is DayCountResult.DaysSince }
            .sortedBy { nextOccurrence(it, today, lunar) }
        val past = events
            .filter { compute(it, today, lunar) is DayCountResult.DaysSince }
            .sortedByDescending { it.date }
        return upcoming + past
    }

    fun progress(event: DayEvent, today: LocalDate, lunar: LunarCalendar): Float? {
        val next = nextOccurrence(event, today, lunar)
        if (next.isBefore(today)) return null
        val last = lastOccurrence(event, next, lunar)
        if (!next.isAfter(last)) return null
        val total = ChronoUnit.DAYS.between(last, next)
        if (total <= 0) return null
        val elapsed = ChronoUnit.DAYS.between(last, today).coerceAtLeast(0L)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    private fun solarNextOccurrence(date: LocalDate, today: LocalDate): LocalDate {
        var candidate = adjustForYear(date, today.year)
        if (candidate.isBefore(today)) {
            candidate = adjustForYear(date, today.year + 1)
        }
        return candidate
    }

    private fun lunarNextOccurrence(event: DayEvent, today: LocalDate, lunar: LunarCalendar): LocalDate {
        val month = event.lunarMonth ?: event.date.monthValue
        val day = event.lunarDay ?: event.date.dayOfMonth
        val lunarDate = LunarDate(month, day, event.lunarLeap)
        var candidate = lunar.toGregorian(today.year, lunarDate)
        if (candidate.isBefore(today)) {
            candidate = lunar.toGregorian(today.year + 1, lunarDate)
        }
        return candidate
    }

    private fun lastOccurrence(event: DayEvent, next: LocalDate, lunar: LunarCalendar): LocalDate {
        if (event.repeatsMonthly) {
            return monthlyOccurrence(event.date.dayOfMonth, next.minusMonths(1))
        }
        if (!event.repeatsYearly) {
            val created = Instant.ofEpochMilli(event.createdAtEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            return if (created.isBefore(next)) created else next.minusDays(1)
        }
        return when (event.calendarType) {
            CalendarType.SOLAR -> adjustForYear(event.date, next.year - 1)
            CalendarType.LUNAR -> {
                val month = event.lunarMonth ?: event.date.monthValue
                val day = event.lunarDay ?: event.date.dayOfMonth
                lunar.toGregorian(next.year - 1, LunarDate(month, day, event.lunarLeap))
            }
        }
    }

    private fun monthlyOccurrence(dayOfMonth: Int, today: LocalDate): LocalDate {
        val day = dayOfMonth.coerceIn(1, 31)
        var candidate = today.withDayOfMonth(day.coerceAtMost(today.lengthOfMonth()))
        if (candidate.isBefore(today)) {
            val nextMonth = today.plusMonths(1)
            candidate = nextMonth.withDayOfMonth(day.coerceAtMost(nextMonth.lengthOfMonth()))
        }
        return candidate
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
