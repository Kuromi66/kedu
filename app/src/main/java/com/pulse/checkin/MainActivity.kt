package com.pulse.checkin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pulse.checkin.ui.AppViewModel
import com.pulse.checkin.ui.PulseApp

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<AppViewModel> {
        AppViewModel.factory((application as PulseApplication).container)
    }

    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseApp(viewModel = viewModel)
        }
    }
}
