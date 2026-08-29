package com.pulse.checkin.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.pulse.checkin.PulseApplication
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
        if (intent.action != ACTION_CHECK_IN) return
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val pendingResult = goAsync()
        val container = (context.applicationContext as PulseApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.checkInRepository.addCheckIn(habitId)
                container.syncManager.syncOnce()
            } finally {
                container.pulseWidgetUpdater.updateCheckInWidget()
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHECK_IN = "com.pulse.checkin.action.WIDGET_CHECK_IN"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}
