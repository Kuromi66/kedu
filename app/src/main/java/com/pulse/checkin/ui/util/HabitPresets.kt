package com.pulse.checkin.ui.util

import androidx.compose.ui.graphics.Color

val habitGlyphOptions = listOf("P", "W", "R", "M", "G", "S")

val habitIconOptions = listOf(
    "\uD83D\uDCA7",
    "\uD83C\uDFC3",
    "\uD83C\uDFCB",
    "\uD83E\uDDD8",
    "\uD83D\uDCD6",
    "\u270D",
    "\uD83E\uDDF9",
    "\uD83E\uDDB7",
    "\uD83E\uDEE5",
    "\uD83D\uDEB6",
    "\uD83C\uDF7D",
    "\uD83D\uDC8A",
    "\uD83C\uDFB8",
    "\uD83D\uDCBB",
)

fun isPresetHabitIcon(glyph: String): Boolean = glyph in habitIconOptions

val habitColorOptions = listOf(
    0xFF101828,
    0xFF2446FF,
    0xFF0D9488,
    0xFFE76F51,
    0xFF7C3AED,
    0xFFF59E0B,
)

fun Long.toPulseColor(): Color = Color(this)
