package com.pulse.checkin.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val cornerRadius = if (compactLayout) 24.dp else 28.dp
    val contentPadding = if (compactLayout) 16.dp else 20.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(cornerRadius),
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(cornerRadius),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
