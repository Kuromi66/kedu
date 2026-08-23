package com.pulse.checkin.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ShareImageDialog(
    title: String,
    description: String,
    bitmap: Bitmap?,
    saveLabel: String,
    shareLabel: String,
    cancelLabel: String,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                bitmap?.let { imageBitmap ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                ShareImageOptionRow(
                    icon = PulseIconKind.Download,
                    text = saveLabel,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onSave,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                ShareImageOptionRow(
                    icon = PulseIconKind.Share,
                    text = shareLabel,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onShare,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                ShareImageOptionRow(
                    icon = null,
                    text = cancelLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun ShareImageOptionRow(
    icon: PulseIconKind?,
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            PulseActionIcon(
                kind = icon,
                color = color,
                compactLayout = true,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
