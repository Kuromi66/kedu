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
import com.pulse.checkin.BuildConfig
import com.pulse.checkin.domain.model.AppLanguage
import com.pulse.checkin.domain.model.ThemeMode
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.PulseActionIcon
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.components.ScreenHeader
import com.pulse.checkin.ui.i18n.LocalPulseStrings

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    appLanguage: AppLanguage,
    notificationsGranted: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
) {
    val strings = LocalPulseStrings.current
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = strings.settingsTitle, compactLayout = compactLayout)
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
                        title = strings.appearance,
                        icon = PulseIconKind.Theme,
                        compactLayout = compactLayout,
                    )
                    Spacer(modifier = Modifier.height(if (compactLayout) 12.dp else 14.dp))
                    ThemeChoiceGroup(themeMode, onThemeModeChange, compactLayout)
                }
            }
            item {
                GlassCard {
                    SettingsSectionHeader(
                        title = strings.languageTitle,
                        icon = PulseIconKind.Language,
                        compactLayout = compactLayout,
                    )
                    Spacer(modifier = Modifier.height(if (compactLayout) 12.dp else 14.dp))
                    LanguageChoiceGroup(appLanguage, onAppLanguageChange, compactLayout)
                }
            }
            item {
                GlassCard {
                    SettingsSectionHeader(
                        title = strings.data,
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
                            SettingsButtonContent(strings.export, PulseIconKind.Upload, MaterialTheme.colorScheme.primary, compactLayout)
                        }
                        Button(
                            onClick = onImportData,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            SettingsButtonContent(strings.import, PulseIconKind.Download, MaterialTheme.colorScheme.onPrimary, compactLayout)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.importOverwriteHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                GlassCard {
                    SettingsSectionHeader(
                        title = strings.notifications,
                        icon = PulseIconKind.Bell,
                        compactLayout = compactLayout,
                        trailing = {
                            Text(
                                text = if (notificationsGranted) strings.enabled else strings.disabled,
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
                            text = if (notificationsGranted) strings.enabled else strings.enableNotifications,
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
                        title = strings.about,
                        icon = PulseIconKind.Info,
                        compactLayout = compactLayout,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.aboutText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.versionName(BuildConfig.VERSION_NAME),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Text(text = text, color = color, fontWeight = FontWeight.SemiBold)
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
            ThemeChoiceItem(mode, selected == mode, compactLayout) { onThemeModeChange(mode) }
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
    val strings = LocalPulseStrings.current
    val title = when (mode) {
        ThemeMode.LIGHT -> strings.lightMode
        ThemeMode.SYSTEM -> strings.systemMode
        ThemeMode.DARK -> strings.darkMode
    }
    val icon = when (mode) {
        ThemeMode.LIGHT -> PulseIconKind.LightMode
        ThemeMode.SYSTEM -> PulseIconKind.SystemMode
        ThemeMode.DARK -> PulseIconKind.DarkMode
    }

    ChoiceRow(title, icon, selected, compactLayout, onClick)
}

@Composable
private fun LanguageChoiceGroup(
    selected: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    compactLayout: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compactLayout) 22.dp else 24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        AppLanguage.values().forEachIndexed { index, language ->
            LanguageChoiceItem(language, selected == language, compactLayout) { onAppLanguageChange(language) }
            if (index != AppLanguage.values().lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = if (compactLayout) 14.dp else 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun LanguageChoiceItem(
    language: AppLanguage,
    selected: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalPulseStrings.current
    val title = when (language) {
        AppLanguage.ZH -> strings.chinese
        AppLanguage.EN -> strings.english
    }

    ChoiceRow(title, PulseIconKind.Info, selected, compactLayout, onClick)
}

@Composable
private fun ChoiceRow(
    title: String,
    icon: PulseIconKind,
    selected: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit,
) {
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
