package com.pulse.checkin.domain.stats

import java.time.LocalDate

data class LunarDate(
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean = false,
)

interface LunarCalendar {
    fun toLunar(gregorian: LocalDate): LunarDate

    fun toGregorian(year: Int, lunar: LunarDate): LocalDate
}
