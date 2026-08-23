package com.pulse.checkin.ui.util

import android.icu.util.Calendar
import android.icu.util.ChineseCalendar
import com.pulse.checkin.domain.stats.LunarCalendar
import com.pulse.checkin.domain.stats.LunarDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object AndroidLunarCalendar : LunarCalendar {
    private const val EPOCH_OFFSET = 2637

    override fun toLunar(gregorian: LocalDate): LunarDate {
        val millis = gregorian.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val cc = ChineseCalendar().apply {
            clear()
            setTimeInMillis(millis)
        }
        return LunarDate(
            month = cc.get(ChineseCalendar.MONTH) + 1,
            day = cc.get(ChineseCalendar.DAY_OF_MONTH),
            isLeapMonth = cc.get(ChineseCalendar.IS_LEAP_MONTH) != 0,
        )
    }

    override fun toGregorian(year: Int, lunar: LunarDate): LocalDate {
        val leapAttempts = listOf(lunar.isLeapMonth, false).distinct()
        for (leap in leapAttempts) {
            for (day in lunar.day downTo 1) {
                val date = convert(year, lunar.month, day, leap)
                val back = toLunar(date)
                if (back.month == lunar.month && back.day == day && back.isLeapMonth == leap) {
                    return date
                }
            }
        }
        return convert(year, 12, 29, false)
    }

    private fun convert(year: Int, month: Int, day: Int, leap: Boolean): LocalDate {
        val cc = ChineseCalendar().apply {
            clear()
            set(ChineseCalendar.EXTENDED_YEAR, year + EPOCH_OFFSET)
            set(ChineseCalendar.MONTH, month - 1)
            set(ChineseCalendar.DAY_OF_MONTH, day)
            set(ChineseCalendar.IS_LEAP_MONTH, if (leap) 1 else 0)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        return Instant.ofEpochMilli(cc.timeInMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
}
