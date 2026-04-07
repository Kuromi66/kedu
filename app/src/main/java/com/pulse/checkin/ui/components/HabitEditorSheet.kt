package com.pulse.checkin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.ui.HabitDraft
import com.pulse.checkin.ui.components.HabitGlyph
import com.pulse.checkin.ui.util.habitColorOptions
import com.pulse.checkin.ui.util.habitGlyphOptions
import com.pulse.checkin.ui.components.habitIconCategories
import com.pulse.checkin.ui.components.habitIconOptions
import com.pulse.checkin.ui.components.isPresetHabitIcon
import com.pulse.checkin.ui.components.resolveHabitIconToken
import com.pulse.checkin.ui.i18n.LocalPulseStrings
import com.pulse.checkin.ui.util.showPulseTimePicker
import com.pulse.checkin.ui.util.toPulseColor

@OptIn(ExperimentalLayoutApi::class)
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun HabitEditorSheet(
    initialDraft: HabitDraft,
    notificationsGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (HabitDraft) -> Unit,
) {
    val context = LocalContext.current
    val strings = LocalPulseStrings.current
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = configuration.screenHeightDp.dp * 0.84f
    val compactLayout = configuration.screenWidthDp <= 360
    val actionAreaHeight = 88.dp
    var name by remember(initialDraft.id) { mutableStateOf(initialDraft.name) }
    var glyphInputMode by remember(initialDraft.id) {
        mutableStateOf(if (isPresetHabitIcon(initialDraft.glyph)) HabitGlyphInputMode.ICON else HabitGlyphInputMode.LETTER)
    }
    var selectedIconGlyph by remember(initialDraft.id) {
        mutableStateOf(resolveInitialIconToken(initialDraft.glyph))
    }
    var selectedIconCategoryKey by remember(initialDraft.id) {
        mutableStateOf(
            habitIconCategories.firstOrNull { category ->
                category.options.any { it.token == resolveInitialIconToken(initialDraft.glyph) }
            }?.key ?: habitIconCategories.first().key
        )
    }
    var letterGlyph by remember(initialDraft.id) {
        mutableStateOf(normalizeLetterGlyph(initialDraft.glyph).ifBlank { habitGlyphOptions.first() })
    }
    var colorArgb by remember(initialDraft.id) { mutableLongStateOf(initialDraft.colorArgb) }
    var reminderEnabled by remember(initialDraft.id) { mutableStateOf(initialDraft.reminderEnabled) }
    var reminderHour by remember(initialDraft.id) { mutableIntStateOf(initialDraft.reminderHour) }
    var reminderMinute by remember(initialDraft.id) { mutableIntStateOf(initialDraft.reminderMinute) }
    var targetEnabled by remember(initialDraft.id) { mutableStateOf(initialDraft.targetEnabled) }
    var targetCount by remember(initialDraft.id) { mutableIntStateOf(initialDraft.targetCount.coerceAtLeast(1)) }

    val sheetSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedBorderColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight - actionAreaHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = if (initialDraft.id == 0L) strings.habitEditorNew else strings.habitEditorEdit,
                    style = MaterialTheme.typography.headlineMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(18) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    label = { Text(strings.habitName) },
                    placeholder = { Text(strings.habitNamePlaceholder) },
                    singleLine = true,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.habitIdentifier, style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(habitIconCategories) { category ->
                            FilterChip(
                                selected = glyphInputMode == HabitGlyphInputMode.ICON && selectedIconCategoryKey == category.key,
                                onClick = {
                                    glyphInputMode = HabitGlyphInputMode.ICON
                                    selectedIconCategoryKey = category.key
                                },
                                label = { Text(strings.habitIconCategoryTitle(category.key)) },
                            )
                        }
                        item {
                            FilterChip(
                                selected = glyphInputMode == HabitGlyphInputMode.LETTER,
                                onClick = { glyphInputMode = HabitGlyphInputMode.LETTER },
                                label = { Text(strings.letters) },
                            )
                        }
                    }
                    if (glyphInputMode == HabitGlyphInputMode.ICON) {
                        val selectedCategory = habitIconCategories.firstOrNull { it.key == selectedIconCategoryKey }
                            ?: habitIconCategories.first()
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = if (compactLayout) 3 else 4,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            selectedCategory.options.forEach { option ->
                                FilterChip(
                                    selected = selectedIconGlyph == option.token,
                                    onClick = { selectedIconGlyph = option.token },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                                                HabitGlyph(
                                                    glyph = option.token,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(16.dp),
                                                    compactLayout = true,
                                                )
                                            }
                                            Text(strings.habitIconLabel(option.token, option.label), fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = letterGlyph,
                            onValueChange = { letterGlyph = normalizeLetterGlyph(it) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            label = { Text(strings.iconLetter) },
                            placeholder = { Text("P / W / R") },
                            supportingText = { Text(strings.iconLetterSupport) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(habitGlyphOptions) { option ->
                                FilterChip(
                                    selected = letterGlyph == option,
                                    onClick = { letterGlyph = option },
                                    label = { Text(option, fontWeight = FontWeight.SemiBold) },
                                )
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.primaryColor, style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(habitColorOptions) { option ->
                            val selected = colorArgb == option
                            Row(
                                modifier = Modifier
                                    .size(if (selected) 50.dp else 42.dp)
                                    .clip(CircleShape)
                                    .padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                FilterChip(
                                    selected = selected,
                                    onClick = { colorArgb = option },
                                    label = { Text(" ", color = Color.Transparent) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = option.toPulseColor().copy(alpha = 0.25f),
                                        selectedContainerColor = option.toPulseColor(),
                                    ),
                                )
                            }
                        }
                    }
                }
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.dailyTarget, style = MaterialTheme.typography.titleLarge)
                            Text(strings.dailyTargetDesc, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = targetEnabled, onCheckedChange = { targetEnabled = it }, colors = sheetSwitchColors)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (targetEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                TextButton(onClick = { targetCount = (targetCount - 1).coerceAtLeast(1) }) { Text("-1") }
                                Text(text = strings.countTimes(targetCount), style = MaterialTheme.typography.headlineMedium)
                                TextButton(onClick = { targetCount += 1 }) { Text("+1") }
                            }
                        } else {
                            Text(
                                text = strings.targetQuickSetHint,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.localReminder, style = MaterialTheme.typography.titleLarge)
                            Text(strings.localReminderDesc, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it }, colors = sheetSwitchColors)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        when {
                            reminderEnabled && !notificationsGranted -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(strings.notificationPermissionOff, color = MaterialTheme.colorScheme.tertiary)
                                    TextButton(onClick = onRequestNotificationPermission) { Text(strings.enableNotificationPermission) }
                                }
                            }

                            reminderEnabled -> {
                                Button(
                                    onClick = {
                                        showPulseTimePicker(context, reminderHour, reminderMinute) { hour, minute ->
                                            reminderHour = hour
                                            reminderMinute = minute
                                        }
                                    },
                                ) {
                                    Text(strings.reminderTime(reminderHour, reminderMinute))
                                }
                            }

                            else -> {
                                Text(
                                    text = strings.setReminderHint,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Button(
                        onClick = {
                            onSave(
                                HabitDraft(
                                    id = initialDraft.id,
                                    name = name,
                                    glyph = if (glyphInputMode == HabitGlyphInputMode.ICON) {
                                        selectedIconGlyph
                                    } else {
                                        letterGlyph.ifBlank { habitGlyphOptions.first() }
                                    },
                                    colorArgb = colorArgb,
                                    reminderEnabled = reminderEnabled,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute,
                                    targetEnabled = targetEnabled,
                                    targetCount = targetCount,
                                ),
                            )
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }
    }
}

private enum class HabitGlyphInputMode {
    ICON,
    LETTER,
}

private fun resolveInitialIconToken(glyph: String): String = resolveHabitIconToken(glyph) ?: habitIconOptions.first().token

private fun normalizeLetterGlyph(input: String): String {
    val normalized = buildString {
        input.uppercase().forEach { char ->
            if (char.isLetterOrDigit() && length < 2) append(char)
        }
    }
    return normalized
}
