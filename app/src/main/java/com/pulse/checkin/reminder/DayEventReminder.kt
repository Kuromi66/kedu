package com.pulse.checkin.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pulse.checkin.MainActivity
import com.pulse.checkin.PulseApplication
import com.pulse.checkin.R
import com.pulse.checkin.domain.model.DayEvent
import com.pulse.checkin.domain.stats.DayCountCalculator
import com.pulse.checkin.ui.util.AndroidLunarCalendar
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

const val EXTRA_NAVIGATE_TO = "navigate_to"
const val NAVIGATE_TO_DAY_EVENTS = "day_events"

private const val DAY_EVENT_CHANNEL_ID = "pulse_day_events"
private const val DAY_EVENT_CHANNEL_NAME = "\u91cd\u8981\u65e5\u671f\u63d0\u9192"
private const val KEY_DAY_EVENT_ID = "day_event_id"
private const val KEY_DAY_EVENT_NAME = "day_event_name"
private const val KEY_DAY_EVENT_DAYS_BEFORE = "day_event_days_before"
private const val DAY_EVENT_WORK_PREFIX = "pulse_day_event_reminder_"
private val REMINDER_TIME = LocalTime.of(9, 0)

class DayEventReminderScheduler(
    private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleForDayEvent(event: DayEvent) {
        cancelForDayEvent(event.id)
        if (event.archived || !event.reminderEnabled) return
        val today = LocalDate.now()
        val isOneTime = !event.repeatsMonthly && !event.repeatsYearly
        if (isOneTime && event.date.isBefore(today)) return

        val reminderDate = DayCountCalculator.nextReminderDate(event, today, AndroidLunarCalendar)
        if (reminderDate.isBefore(today)) return
        val reminderDateTime = LocalDateTime.of(reminderDate, REMINDER_TIME)
        val initialDelayMillis = Duration.between(LocalDateTime.now(), reminderDateTime).toMillis().coerceAtLeast(0L)

        val data = Data.Builder()
            .putString(KEY_DAY_EVENT_ID, event.id)
            .putString(KEY_DAY_EVENT_NAME, event.name)
            .putInt(KEY_DAY_EVENT_DAYS_BEFORE, event.reminderDaysBefore.coerceAtLeast(1))
            .build()
        val request = OneTimeWorkRequestBuilder<DayEventReminderWorker>()
            .setInputData(data)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag(uniqueWorkName(event.id))
            .build()
        workManager.enqueueUniqueWork(
            uniqueWorkName(event.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelForDayEvent(eventId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(eventId))
    }

    fun syncAll(events: List<DayEvent>) {
        events.forEach(::scheduleForDayEvent)
    }

    private fun uniqueWorkName(eventId: String): String = "$DAY_EVENT_WORK_PREFIX$eventId"
}

class DayEventReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PulseApplication).container
        val eventId = inputData.getString(KEY_DAY_EVENT_ID) ?: return Result.success()
        val event = container.dayEventRepository.getDayEvent(eventId) ?: return Result.success()
        if (event.archived || !event.reminderEnabled) return Result.success()

        val today = LocalDate.now()
        val next = DayCountCalculator.nextOccurrence(event, today, AndroidLunarCalendar)
        val reminderDate = next.minusDays(event.reminderDaysBefore.coerceAtLeast(1).toLong())
        if (reminderDate == today && next.isAfter(today)) {
            notifyDayEvent(applicationContext, event, next)
        }

        // 自续排下一个周期
        container.dayEventReminderScheduler.scheduleForDayEvent(event)
        return Result.success()
    }
}

private fun notifyDayEvent(context: Context, event: DayEvent, next: LocalDate) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    createDayEventChannelIfNeeded(context)

    val days = ChronoUnit.DAYS.between(LocalDate.now(), next).coerceAtLeast(0L)
    val text = if (days > 0) {
        context.getString(R.string.days_until, days)
    } else {
        context.getString(R.string.day_event_today)
    }
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_NAVIGATE_TO, NAVIGATE_TO_DAY_EVENTS)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        (event.id.hashCode() and 0x7fffffff).coerceAtLeast(1),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, DAY_EVENT_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(event.name)
        .setContentText(text)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(
        (event.id.hashCode() and 0x7fffffff).coerceAtLeast(1),
        notification,
    )
}

private fun createDayEventChannelIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        DAY_EVENT_CHANNEL_ID,
        DAY_EVENT_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT,
    )
    manager.createNotificationChannel(channel)
}
