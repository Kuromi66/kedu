package com.pulse.checkin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pulse.checkin.ui.AppViewModel
import com.pulse.checkin.ui.PulseApp
import com.pulse.checkin.ui.AppTab
import com.pulse.checkin.reminder.EXTRA_NAVIGATE_TO
import com.pulse.checkin.reminder.NAVIGATE_TO_CHECK_IN
import com.pulse.checkin.reminder.NAVIGATE_TO_DAY_EVENTS

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<AppViewModel> {
        AppViewModel.factory((application as PulseApplication).container)
    }

    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val navigateTo = intent?.getStringExtra(EXTRA_NAVIGATE_TO)
        val pendingCheckInHabitId = if (navigateTo == NAVIGATE_TO_CHECK_IN) {
            intent?.getStringExtra(EXTRA_CHECK_IN_HABIT_ID)
        } else {
            null
        }
        setContent {
            PulseApp(viewModel = viewModel, pendingCheckInHabitId = pendingCheckInHabitId)
        }
        if (navigateTo == NAVIGATE_TO_DAY_EVENTS) {
            viewModel.selectTab(AppTab.DAY_EVENTS)
        }
    }

    companion object {
        const val EXTRA_CHECK_IN_HABIT_ID = "check_in_habit_id"
    }
}
