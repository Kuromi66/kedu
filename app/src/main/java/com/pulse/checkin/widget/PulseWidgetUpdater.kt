package com.pulse.checkin.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.pulse.checkin.MainActivity
import com.pulse.checkin.PulseApplication
import com.pulse.checkin.R
import com.pulse.checkin.domain.model.CheckInEvent
import com.pulse.checkin.domain.model.DayEvent
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.stats.DayCountCalculator
import com.pulse.checkin.domain.stats.DayCountResult
import com.pulse.checkin.reminder.EXTRA_NAVIGATE_TO
import com.pulse.checkin.reminder.NAVIGATE_TO_CHECK_IN
import com.pulse.checkin.reminder.NAVIGATE_TO_DAY_EVENTS
import com.pulse.checkin.ui.util.AndroidLunarCalendar
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class PulseWidgetUpdater(
    private val context: Context,
) {
    private val app get() = context.applicationContext as PulseApplication

    private val checkInRowIds = intArrayOf(
        R.id.habit_row_1,
        R.id.habit_row_2,
        R.id.habit_row_3,
        R.id.habit_row_4,
    )
    private val checkInNameIds = intArrayOf(
        R.id.habit_name_1,
        R.id.habit_name_2,
        R.id.habit_name_3,
        R.id.habit_name_4,
    )
    private val checkInProgressIds = intArrayOf(
        R.id.habit_progress_1,
        R.id.habit_progress_2,
        R.id.habit_progress_3,
        R.id.habit_progress_4,
    )
    private val checkInDotIds = intArrayOf(
        R.id.habit_dot_1,
        R.id.habit_dot_2,
        R.id.habit_dot_3,
        R.id.habit_dot_4,
    )
    private val datesRowIds = intArrayOf(
        R.id.date_row_1,
        R.id.date_row_2,
        R.id.date_row_3,
        R.id.date_row_4,
    )
    private val datesNameIds = intArrayOf(
        R.id.date_name_1,
        R.id.date_name_2,
        R.id.date_name_3,
        R.id.date_name_4,
    )
    private val datesCountIds = intArrayOf(
        R.id.date_count_1,
        R.id.date_count_2,
        R.id.date_count_3,
        R.id.date_count_4,
    )
    private val datesDotIds = intArrayOf(
        R.id.date_dot_1,
        R.id.date_dot_2,
        R.id.date_dot_3,
        R.id.date_dot_4,
    )
    private val datesAccent = context.getColor(R.color.widget_accent)

    suspend fun updateAll() {
        updateCheckInWidget()
        updateDatesWidget()
    }

    suspend fun updateCheckInWidget() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, PulseCheckInWidgetProvider::class.java),
        )
        if (ids.isEmpty()) return
        val habits = app.container.habitRepository.observeHabits().first()
        val events = app.container.checkInRepository.observeAllEvents().first()
        val views = buildCheckInRemoteViews(habits, events)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    suspend fun updateDatesWidget() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, PulseDatesWidgetProvider::class.java),
        )
        if (ids.isEmpty()) return
        val dayEvents = app.container.dayEventRepository.observeDayEvents().first()
        val views = buildDatesRemoteViews(dayEvents, LocalDate.now())
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun buildCheckInRemoteViews(
        habits: List<Habit>,
        events: List<CheckInEvent>,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_checkin)
        val today = LocalDate.now()
        val counts = events
            .filter { it.localDate == today }
            .groupingBy { it.habitId }
            .eachCount()
        val total = counts.values.sum()
        val completed = habits.count { habit ->
            (counts[habit.id] ?: 0) >= (habit.targetCountOrDefault() ?: 1)
        }
        views.setTextViewText(
            R.id.widget_summary,
            context.getString(R.string.widget_summary, total, completed, habits.size),
        )

        val visible = habits.sortedBy { it.sortOrder }.take(4)
        repeat(4) { index ->
            val habit = visible.getOrNull(index)
            if (habit == null) {
                views.setViewVisibility(checkInRowIds[index], View.GONE)
                return@repeat
            }
            views.setViewVisibility(checkInRowIds[index], View.VISIBLE)
            views.setTextViewText(checkInNameIds[index], habit.name)
            views.setTextColor(checkInDotIds[index], habit.colorArgb.toInt())
            val count = counts[habit.id] ?: 0
            val target = habit.targetCountOrDefault()
            val progressText = if (target != null) {
                "$count/$target"
            } else {
                context.getString(R.string.count_times, count)
            }
            val reached = count >= (target ?: 1)
            views.setTextViewText(
                checkInProgressIds[index],
                if (reached) "\u2713 $progressText" else progressText,
            )
            views.setOnClickPendingIntent(
                checkInRowIds[index],
                openCheckInIntent(habit.id),
            )
        }

        views.setOnClickPendingIntent(R.id.widget_root, openTodayIntent())
        return views
    }

    private fun buildDatesRemoteViews(
        dayEvents: List<DayEvent>,
        today: LocalDate,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_dates)
        val upcoming = dayEvents
            .map { event -> event to DayCountCalculator.compute(event, today, AndroidLunarCalendar) }
            .filter { it.second !is DayCountResult.DaysSince }
            .sortedBy { DayCountCalculator.nextOccurrence(it.first, today, AndroidLunarCalendar) }
            .take(4)

        repeat(4) { index ->
            val item = upcoming.getOrNull(index)
            if (item == null) {
                views.setViewVisibility(datesRowIds[index], View.GONE)
                return@repeat
            }
            val (event, result) = item
            val daysText = when (result) {
                is DayCountResult.DaysUntil -> context.getString(R.string.days_until, result.days)
                DayCountResult.Today -> context.getString(R.string.day_event_today)
                is DayCountResult.DaysSince -> context.getString(R.string.days_since, result.days)
            }
            views.setViewVisibility(datesRowIds[index], View.VISIBLE)
            views.setTextViewText(datesNameIds[index], event.name)
            views.setTextViewText(datesCountIds[index], daysText)
            views.setTextColor(datesDotIds[index], datesAccent)
            views.setOnClickPendingIntent(datesRowIds[index], openDayEventsIntent())
        }

        if (upcoming.isEmpty()) {
            views.setViewVisibility(datesRowIds[0], View.VISIBLE)
            views.setTextViewText(datesNameIds[0], context.getString(R.string.widget_no_dates))
            views.setTextViewText(datesCountIds[0], "")
            views.setTextColor(datesDotIds[0], datesAccent)
            views.setOnClickPendingIntent(datesRowIds[0], openDayEventsIntent())
        }

        views.setOnClickPendingIntent(R.id.dates_root, openDayEventsIntent())
        return views
    }

    private fun openCheckInIntent(habitId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, NAVIGATE_TO_CHECK_IN)
            putExtra(MainActivity.EXTRA_CHECK_IN_HABIT_ID, habitId)
        }
        return PendingIntent.getActivity(
            context,
            (habitId.hashCode() and 0x7fffffff).coerceAtLeast(1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openTodayIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openDayEventsIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, NAVIGATE_TO_DAY_EVENTS)
        }
        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
