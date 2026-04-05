package com.pulse.checkin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.ui.components.GlassCard

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    notificationsGranted: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Settings", style = MaterialTheme.typography.displaySmall)
                Text("\u4fdd\u6301\u754c\u9762\u514b\u5236\uff0c\u628a\u91cd\u8981\u8bbe\u7f6e\u7559\u5728\u6700\u524d\u9762\u3002", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            GlassCard {
                Text("\u5916\u89c2", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "\u9009\u4e00\u4e2a\u66f4\u9002\u5408\u73af\u5883\u7684\u663e\u793a\u65b9\u5f0f",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(18.dp))
                ThemeChoiceRow(selected = themeMode, onThemeModeChange = onThemeModeChange)
            }
        }
        item {
            GlassCard {
                Text("\u901a\u77e5", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (notificationsGranted) "\u7cfb\u7edf\u901a\u77e5\u6743\u9650\u5df2\u5f00\u542f" else "\u7cfb\u7edf\u901a\u77e5\u6743\u9650\u5c1a\u672a\u5f00\u542f",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRequestNotificationPermission, enabled = !notificationsGranted) {
                    Text(if (notificationsGranted) "\u5df2\u5f00\u542f\u901a\u77e5" else "\u8bf7\u6c42\u901a\u77e5\u6743\u9650")
                }
            }
        }
        item {
            GlassCard {
                Text("\u5e94\u7528\u8bf4\u660e", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pulse \u662f\u4e00\u4e2a\u5b8c\u5168\u672c\u5730\u7684\u6b21\u6570\u578b\u4e60\u60ef\u6253\u5361\u5e94\u7528\u3002\u4f60\u53ef\u4ee5\u4e3a\u6bcf\u4e2a\u4e60\u60ef\u914d\u7f6e\u53ef\u9009\u76ee\u6807\u548c\u6bcf\u65e5\u63d0\u9192\uff0c\u5e76\u5728\u5386\u53f2\u9875\u67e5\u770b\u6708\u5386\u4e0e\u5f53\u5929\u660e\u7ec6\u3002",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThemeChoiceRow(selected: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeMode.values().forEach { mode ->
            val selectedMode = selected == mode
            val title = when (mode) {
                ThemeMode.LIGHT -> "\u6d45\u8272"
                ThemeMode.SYSTEM -> "\u8ddf\u968f\u7cfb\u7edf"
                ThemeMode.DARK -> "\u6df1\u8272"
            }
            val subtitle = when (mode) {
                ThemeMode.LIGHT -> "\u6c38\u8fdc\u660e\u4eae\u6e05\u723d"
                ThemeMode.SYSTEM -> "\u968f\u65f6\u95f4\u81ea\u52a8\u5207\u6362"
                ThemeMode.DARK -> "\u591c\u95f4\u66f4\u6c89\u9759"
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (selectedMode) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                    )
                    .clickable { onThemeModeChange(mode) }
                    .padding(horizontal = 14.dp, vertical = 16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selectedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (selectedMode) "\u5df2\u9009\u62e9" else "\u70b9\u51fb\u5207\u6362",
                            color = if (selectedMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height((18 + index * 6).dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selectedMode) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f + index * 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f + index * 0.08f)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
