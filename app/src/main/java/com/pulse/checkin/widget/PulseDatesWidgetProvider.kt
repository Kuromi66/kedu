package com.pulse.checkin.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.pulse.checkin.PulseApplication
import kotlinx.coroutines.launch

class PulseDatesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext as PulseApplication
        app.appScope.launch {
            app.container.pulseWidgetUpdater.updateDatesWidget()
        }
    }
}
