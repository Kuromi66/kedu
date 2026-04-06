package com.pulse.checkin.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pulse.checkin.domain.model.ThemeMode

private val LightScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = Mint,
    tertiary = Rose,
    background = Shell,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = SoftInk,
    outline = Mist,
)

private val DarkScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    secondary = Mint,
    tertiary = Rose,
    background = DarkSurface,
    surface = DarkSurfaceAlt,
    surfaceVariant = Color(0xFF22304A),
    onPrimary = Ink,
    onSecondary = Ink,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
)

private val PulseShapes = Shapes()

@Composable
fun PulseTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
    }
    val targetScheme = if (darkTheme) DarkScheme else LightScheme
    val colorScheme = animatePulseColorScheme(targetScheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        shapes = PulseShapes,
        content = content,
    )
}


@Composable
private fun animatePulseColorScheme(target: ColorScheme): ColorScheme {
    val animationSpec = spring<Color>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )
    return target.copy(
        primary = animateColorAsState(target.primary, animationSpec = animationSpec, label = "theme-primary").value,
        secondary = animateColorAsState(target.secondary, animationSpec = animationSpec, label = "theme-secondary").value,
        tertiary = animateColorAsState(target.tertiary, animationSpec = animationSpec, label = "theme-tertiary").value,
        background = animateColorAsState(target.background, animationSpec = animationSpec, label = "theme-background").value,
        surface = animateColorAsState(target.surface, animationSpec = animationSpec, label = "theme-surface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, animationSpec = animationSpec, label = "theme-surface-variant").value,
        onPrimary = animateColorAsState(target.onPrimary, animationSpec = animationSpec, label = "theme-on-primary").value,
        onSecondary = animateColorAsState(target.onSecondary, animationSpec = animationSpec, label = "theme-on-secondary").value,
        onBackground = animateColorAsState(target.onBackground, animationSpec = animationSpec, label = "theme-on-background").value,
        onSurface = animateColorAsState(target.onSurface, animationSpec = animationSpec, label = "theme-on-surface").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, animationSpec = animationSpec, label = "theme-on-surface-variant").value,
        outline = animateColorAsState(target.outline, animationSpec = animationSpec, label = "theme-outline").value,
    )
}
