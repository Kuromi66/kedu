package com.pulse.checkin.widget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pulse.checkin.PulseApplication
import com.pulse.checkin.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PulseCheckInWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext as PulseApplication
        app.appScope.launch {
            app.container.pulseWidgetUpdater.updateCheckInWidget()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val pendingResult = goAsync()
        val container = (context.applicationContext as PulseApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_CHECK_IN -> requestConfirm(context, container, habitId)
                    ACTION_CONFIRM_CHECK_IN -> {
                        container.checkInRepository.addCheckIn(habitId)
                        container.syncManager.syncOnce()
                        dismissConfirm(context, habitId)
                    }
                    ACTION_CANCEL_CHECK_IN -> dismissConfirm(context, habitId)
                }
            } finally {
                container.pulseWidgetUpdater.updateCheckInWidget()
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHECK_IN = "com.pulse.checkin.action.WIDGET_CHECK_IN_TAP"
        const val ACTION_CONFIRM_CHECK_IN = "com.pulse.checkin.action.WIDGET_CONFIRM_CHECK_IN"
        const val ACTION_CANCEL_CHECK_IN = "com.pulse.checkin.action.WIDGET_CANCEL_CHECK_IN"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}

private const val CONFIRM_CHANNEL_ID = "pulse_widget_confirm"
private const val CONFIRM_CHANNEL_NAME = "\u6253\u5361\u786e\u8ba4"

private suspend fun requestConfirm(
    context: Context,
    container: com.pulse.checkin.AppContainer,
    habitId: String,
) {
    val habit = container.habitRepository.getHabit(habitId) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        // 没有通知权限时退化为直接打卡，保证小组件可用
        container.checkInRepository.addCheckIn(habitId)
        container.syncManager.syncOnce()
        return
    }
    createConfirmChannelIfNeeded(context)

    val notificationId = (habitId.hashCode() and 0x7fffffff).coerceAtLeast(1)
    val confirmIntent = Intent(context, PulseCheckInWidgetProvider::class.java).apply {
        action = PulseCheckInWidgetProvider.ACTION_CONFIRM_CHECK_IN
        putExtra(PulseCheckInWidgetProvider.EXTRA_HABIT_ID, habitId)
    }
    val cancelIntent = Intent(context, PulseCheckInWidgetProvider::class.java).apply {
        action = PulseCheckInWidgetProvider.ACTION_CANCEL_CHECK_IN
        putExtra(PulseCheckInWidgetProvider.EXTRA_HABIT_ID, habitId)
    }
    val confirmPending = PendingIntent.getBroadcast(
        context,
        notificationId,
        confirmIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val cancelPending = PendingIntent.getBroadcast(
        context,
        notificationId + 1,
        cancelIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val dismissIntent = Intent(context, PulseCheckInWidgetProvider::class.java).apply {
        action = PulseCheckInWidgetProvider.ACTION_CANCEL_CHECK_IN
        putExtra(PulseCheckInWidgetProvider.EXTRA_HABIT_ID, habitId)
    }
    val dismissPending = PendingIntent.getBroadcast(
        context,
        notificationId + 2,
        dismissIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CONFIRM_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(habit.name)
        .setContentText(context.getString(R.string.confirm_check_in_text, habit.name))
        .setContentIntent(dismissPending)
        .addAction(0, context.getString(R.string.check_in), confirmPending)
        .addAction(0, context.getString(R.string.cancel), cancelPending)
        .setAutoCancel(false)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    NotificationManagerCompat.from(context).notify(notificationId, notification)
}

private fun dismissConfirm(context: Context, habitId: String) {
    NotificationManagerCompat.from(context).cancel(
        (habitId.hashCode() and 0x7fffffff).coerceAtLeast(1),
    )
}

private fun createConfirmChannelIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        CONFIRM_CHANNEL_ID,
        CONFIRM_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT,
    )
    manager.createNotificationChannel(channel)
}
