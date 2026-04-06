package com.pulse.checkin.ui.util

import androidx.compose.ui.graphics.Color

val habitGlyphOptions = listOf("P", "W", "R", "M", "G", "S")

val habitColorOptions = listOf(
    0xFF101828,
    0xFF2446FF,
    0xFF0D9488,
    0xFFE76F51,
    0xFF7C3AED,
    0xFFF59E0B,
)

fun Long.toPulseColor(): Color = Color(this)
