package com.pulse.checkin.data.update

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
import com.pulse.checkin.MainActivity
import com.pulse.checkin.R
import com.pulse.checkin.reminder.EXTRA_NAVIGATE_TO
import com.pulse.checkin.reminder.NAVIGATE_TO_UPDATE

private const val UPDATE_CHANNEL_ID = "pulse_update"
private const val UPDATE_CHANNEL_NAME = "\u65b0\u7248\u672c\u63d0\u9192"
private const val UPDATE_NOTIFICATION_ID = 1001

object UpdateNotifier {
    fun notify(context: Context, manifest: VersionManifest) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannelIfNeeded(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, NAVIGATE_TO_UPDATE)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(context.getString(R.string.update_notification_text, manifest.versionName))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            UPDATE_CHANNEL_ID,
            UPDATE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }
}
