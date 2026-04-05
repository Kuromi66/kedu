package com.pulse.checkin.ui.util

import android.app.TimePickerDialog
import android.content.Context

fun showPulseTimePicker(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onSelected: (Int, Int) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(hour, minute) },
        initialHour,
        initialMinute,
        true,
    ).show()
}
