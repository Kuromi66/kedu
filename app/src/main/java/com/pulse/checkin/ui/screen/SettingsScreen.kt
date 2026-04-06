package com.pulse.checkin.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.PulseActionIcon
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.components.ScreenHeader

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    notificationsGranted: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
) {
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "\u8bbe\u7f6e", compactLayout = compactLayout)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = contentSpacing,
                bottom = if (compactLayout) 84.dp else 92.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            item {
                GlassCard {
                    SettingsSectionHeader(
                        title = "\u5916\u89c2",
                        icon = PulseIconKind.Theme,
                        compactLayout = compactLayout,
                    )
                    Spacer(modifier = Modifier.height(if (compactLayout) 12.dp else 14.dp))
                    ThemeChoiceGroup(
                        selected = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        compactLayout = compactLayout,
                    )
                }
            }
            item {
                GlassCard {
                    SettingsSectionHeader(
                        title = "\u6570\u636e",
                        icon = PulseIconKind.Data,
                        compactLayout = compactLayout,
                    )
                    Spacer(modifier = Modifier.height(if (compactLayout) 12.dp else 14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onExportData,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                        ) {
                            SettingsButtonContent(
                                text = "\u5bfc\u51fa",
                                icon = PulseIconKind.Upload,
                                color = MaterialTheme.colorScheme.primary,
                                compactLayout = compactLayout,
                            )
                        }
                        Button(
                            onClick = onImportData,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            SettingsButtonContent(
                                text = "\u5bfc\u5165",
                                icon = PulseIconKind.Download,
                                color = MaterialTheme.colorScheme.onPrimary,
                                compactLayout = compactLayout,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "\u5bfc\u5165\u4f1a\u8986\u76d6\u5f53\u524d\u672c\u5730\u6570\u636e",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                GlassCard {
                    SettingsSectionHeader(
                        title = "\u901a\u77e5",
                        icon = PulseIconKind.Bell,
                        compactLayout = compactLayout,
                        trailing = {
                            Text(
                                text = if (notificationsGranted) "\u5df2\u5f00\u542f" else "\u672a\u5f00\u542f",
                                color = if (notificationsGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(if (compactLayout) 12.dp else 14.dp))
                    Button(
                        onClick = onRequestNotificationPermission,
                        enabled = !notificationsGranted,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        SettingsButtonContent(
                            text = if (notificationsGranted) "\u5df2\u5f00\u542f" else "\u5f00\u542f\u901a\u77e5",
                            icon = PulseIconKind.Bell,
                            color = MaterialTheme.colorScheme.onPrimary,
                            compactLayout = compactLayout,
                        )
                    }
                }
            }
            item {
                GlassCard {
                    SettingsSectionHeader(
                        title = "\u5173\u4e8e",
                        icon = PulseIconKind.Info,
                        compactLayout = compactLayout,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\u523b\u5ea6\u662f\u4e00\u4e2a\u5b8c\u5168\u672c\u5730\u7684\u4e60\u60ef\u6253\u5361\u5e94\u7528",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: PulseIconKind,
    compactLayout: Boolean,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (compactLayout) 34.dp else 38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                PulseActionIcon(
                    kind = icon,
                    color = MaterialTheme.colorScheme.primary,
                    compactLayout = compactLayout,
                    modifier = Modifier.size(if (compactLayout) 18.dp else 20.dp),
                )
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        trailing?.invoke()
    }
}

@Composable
private fun SettingsButtonContent(
    text: String,
    icon: PulseIconKind,
    color: Color,
    compactLayout: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseActionIcon(
            kind = icon,
            color = color,
            compactLayout = compactLayout,
            modifier = Modifier.size(if (compactLayout) 18.dp else 20.dp),
        )
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ThemeChoiceGroup(
    selected: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    compactLayout: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compactLayout) 22.dp else 24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        ThemeMode.values().forEachIndexed { index, mode ->
            ThemeChoiceItem(
                mode = mode,
                selected = selected == mode,
                compactLayout = compactLayout,
                onClick = { onThemeModeChange(mode) },
            )
            if (index != ThemeMode.values().lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = if (compactLayout) 14.dp else 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun ThemeChoiceItem(
    mode: ThemeMode,
    selected: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit,
) {
    val title = when (mode) {
        ThemeMode.LIGHT -> "\u6d45\u8272"
        ThemeMode.SYSTEM -> "\u8ddf\u968f\u7cfb\u7edf"
        ThemeMode.DARK -> "\u6df1\u8272"
    }
    val icon = when (mode) {
        ThemeMode.LIGHT -> PulseIconKind.LightMode
        ThemeMode.SYSTEM -> PulseIconKind.SystemMode
        ThemeMode.DARK -> PulseIconKind.DarkMode
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = if (compactLayout) 14.dp else 16.dp, vertical = if (compactLayout) 14.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compactLayout) 34.dp else 38.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surface,
                ),
            contentAlignment = Alignment.Center,
        ) {
            PulseActionIcon(
                kind = icon,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                compactLayout = compactLayout,
                modifier = Modifier.size(if (compactLayout) 18.dp else 20.dp),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .size(if (compactLayout) 20.dp else 22.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surface,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    ),
            )
        }
    }
}
