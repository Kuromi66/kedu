package com.pulse.checkin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScreenHeader(
    title: String,
    compactLayout: Boolean,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(if (compactLayout) 52.dp else 58.dp)
            .padding(horizontal = if (compactLayout) 16.dp else 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = if (compactLayout) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (leading != null) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                leading()
            }
        }
        if (action != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                action()
            }
        }
    }
}

@Composable
fun HeaderFilterButton(
    active: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }

    Box(
        modifier = modifier
            .size(if (compactLayout) 38.dp else 42.dp)
            .background(color = containerColor, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PulseActionIcon(
            kind = PulseIconKind.Filter,
            color = accentColor,
            compactLayout = compactLayout,
            modifier = Modifier.size(if (compactLayout) 18.dp else 20.dp),
        )
    }
}
