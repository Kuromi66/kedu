package com.pulse.checkin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.ui.HabitDraft
import com.pulse.checkin.ui.util.habitColorOptions
import com.pulse.checkin.ui.util.habitGlyphOptions
import com.pulse.checkin.ui.util.showPulseTimePicker
import com.pulse.checkin.ui.util.toPulseColor

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
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = configuration.screenHeightDp.dp * 0.84f
    val actionAreaHeight = 88.dp
    var name by remember(initialDraft.id) { mutableStateOf(initialDraft.name) }
    var glyph by remember(initialDraft.id) { mutableStateOf(initialDraft.glyph) }
    var colorArgb by remember(initialDraft.id) { mutableLongStateOf(initialDraft.colorArgb) }
    var reminderEnabled by remember(initialDraft.id) { mutableStateOf(initialDraft.reminderEnabled) }
    var reminderHour by remember(initialDraft.id) { mutableIntStateOf(initialDraft.reminderHour) }
    var reminderMinute by remember(initialDraft.id) { mutableIntStateOf(initialDraft.reminderMinute) }
    var targetEnabled by remember(initialDraft.id) { mutableStateOf(initialDraft.targetEnabled) }
    var targetCount by remember(initialDraft.id) { mutableIntStateOf(initialDraft.targetCount.coerceAtLeast(1)) }

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
                    text = if (initialDraft.id == 0L) "\u65b0\u5efa\u4e60\u60ef" else "\u7f16\u8f91\u4e60\u60ef",
                    style = MaterialTheme.typography.headlineMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(18) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    label = { Text("\u4e60\u60ef\u540d\u79f0") },
                    placeholder = { Text("\u4f8b\u5982 \u559d\u6c34\u3001\u62c9\u4f38\u3001\u9605\u8bfb") },
                    singleLine = true,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("\u56fe\u6807\u5b57\u6bcd", style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(habitGlyphOptions) { option ->
                            FilterChip(
                                selected = glyph == option,
                                onClick = { glyph = option },
                                label = { Text(option, fontWeight = FontWeight.SemiBold) },
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("\u4e3b\u8272", style = MaterialTheme.typography.titleLarge)
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
                            Text("\u6bcf\u65e5\u76ee\u6807", style = MaterialTheme.typography.titleLarge)
                            Text("\u8fbe\u5230\u6b21\u6570\u540e\u5f53\u5929\u8bb0\u4e3a\u8fbe\u6807", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = targetEnabled, onCheckedChange = { targetEnabled = it })
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
                                Text(text = "$targetCount \u6b21", style = MaterialTheme.typography.headlineMedium)
                                TextButton(onClick = { targetCount += 1 }) { Text("+1") }
                            }
                        } else {
                            Text(
                                text = "\u5f00\u542f\u540e\u53ef\u5728\u8fd9\u91cc\u5feb\u901f\u8bbe\u7f6e\u76ee\u6807\u6b21\u6570",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("\u672c\u5730\u63d0\u9192", style = MaterialTheme.typography.titleLarge)
                            Text("\u6bcf\u5929\u56fa\u5b9a\u65f6\u95f4\u63d0\u9192\u4f60\u8865\u4e0a\u6b21\u6570", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
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
                                    Text("\u7cfb\u7edf\u901a\u77e5\u6743\u9650\u5c1a\u672a\u5f00\u542f", color = MaterialTheme.colorScheme.tertiary)
                                    TextButton(onClick = onRequestNotificationPermission) { Text("\u53bb\u5f00\u542f\u901a\u77e5") }
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
                                    Text(String.format("\u63d0\u9192\u65f6\u95f4 %02d:%02d", reminderHour, reminderMinute))
                                }
                            }

                            else -> {
                                Text(
                                    text = "\u5f00\u542f\u540e\u53ef\u5728\u8fd9\u91cc\u8bbe\u7f6e\u6bcf\u65e5\u63d0\u9192\u65f6\u95f4",
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
                                    glyph = glyph,
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
                        Text("\u4fdd\u5b58")
                    }
                }
            }
        }
    }
}


