package com.pulse.checkin.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.pulse.checkin.MainActivity
import com.pulse.checkin.PulseApplication
import com.pulse.checkin.domain.model.Habit
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "pulse_reminders"
private const val CHANNEL_NAME = "\u6253\u5361\u63d0\u9192"
private const val KEY_HABIT_ID = "habit_id"
private const val KEY_HABIT_NAME = "habit_name"
private const val WORK_PREFIX = "habit_reminder_"

class ReminderSchedulerImpl(
    private val context: Context,
) : ReminderScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun scheduleForHabit(habit: Habit) {
        val reminderTime = habit.reminderTimeOrNull()
        if (habit.archived || reminderTime == null) {
            cancelForHabit(habit.id)
            return
        }
        val initialDelay = computeInitialDelayMillis(habit)
        val data = Data.Builder()
            .putString(KEY_HABIT_ID, habit.id)
            .putString(KEY_HABIT_NAME, habit.name)
            .build()
        val request = PeriodicWorkRequestBuilder<HabitReminderWorker>(1, TimeUnit.DAYS)
            .setInputData(data)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(uniqueWorkName(habit.id))
            .build()
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName(habit.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun cancelForHabit(habitId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(habitId))
    }

    override fun syncAll(habits: List<Habit>) {
        habits.forEach(::scheduleForHabit)
    }

    private fun uniqueWorkName(habitId: String): String = "$WORK_PREFIX$habitId"

    private fun computeInitialDelayMillis(habit: Habit): Long {
        val reminderTime = habit.reminderTimeOrNull() ?: return TimeUnit.HOURS.toMillis(24)
        val now = LocalDateTime.now()
        var next = now.withHour(reminderTime.hour)
            .withMinute(reminderTime.minute)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMillis().coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
    }
}

class HabitReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        createChannelIfNeeded(applicationContext)
        val habitName = inputData.getString(KEY_HABIT_NAME).orEmpty().ifBlank { "\u4eca\u65e5\u4e60\u60ef" }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val habitId = inputData.getString(KEY_HABIT_ID)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (habitId.hashCode() and 0x7fffffff).coerceAtLeast(1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(habitName)
            .setContentText("\u8be5\u4e3a\u4eca\u5929\u7684\u6b21\u6570\u518d\u52a0\u4e00\u6b21\u4e86")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(
            (habitId.hashCode() and 0x7fffffff).coerceAtLeast(1),
            notification,
        )
        return Result.success()
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as PulseApplication).container
                val habits = container.habitRepository.getActiveReminderHabits()
                container.reminderScheduler.syncAll(habits)
                val dayEvents = container.dayEventRepository.getActiveReminderDayEvents()
                container.dayEventReminderScheduler.syncAll(dayEvents)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
