package com.pulse.checkin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.CalendarType
import com.pulse.checkin.domain.stats.LunarDate
import com.pulse.checkin.ui.DayEventDraft
import com.pulse.checkin.ui.i18n.LocalPulseStrings
import com.pulse.checkin.ui.util.AndroidLunarCalendar
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEventEditorSheet(
    initialDraft: DayEventDraft,
    onDismiss: () -> Unit,
    onSave: (DayEventDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val strings = LocalPulseStrings.current
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = configuration.screenHeightDp.dp * 0.84f
    var name by remember(initialDraft.id) { mutableStateOf(initialDraft.name) }
    var date by remember(initialDraft.id) { mutableStateOf(initialDraft.date) }
    var repeatsMonthly by remember(initialDraft.id) { mutableStateOf(initialDraft.repeatsMonthly) }
    var repeatsYearly by remember(initialDraft.id) { mutableStateOf(initialDraft.repeatsYearly) }
    var note by remember(initialDraft.id) { mutableStateOf(initialDraft.note ?: "") }
    var calendarType by remember(initialDraft.id) { mutableStateOf(initialDraft.calendarType) }
    var lunarMonth by remember(initialDraft.id) { mutableStateOf(initialDraft.lunarMonth ?: 1) }
    var lunarDay by remember(initialDraft.id) { mutableStateOf(initialDraft.lunarDay ?: 1) }
    var lunarLeap by remember(initialDraft.id) { mutableStateOf(initialDraft.lunarLeap) }
    var reminderEnabled by remember(initialDraft.id) { mutableStateOf(initialDraft.reminderEnabled) }
    var reminderDaysBefore by remember(initialDraft.id) {
        mutableStateOf(initialDraft.reminderDaysBefore.coerceIn(1, 7))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val switchColors = SwitchDefaults.colors(
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = if (initialDraft.id.isBlank()) strings.dayEventNew else strings.dayEventEdit,
                    style = MaterialTheme.typography.headlineMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text(strings.dayEventName) },
                    placeholder = { Text(strings.dayEventNamePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (calendarType == CalendarType.SOLAR && !repeatsMonthly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = strings.dayEventDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (repeatsYearly) {
                                    strings.historyDetailDate(date)
                                } else {
                                    strings.dayEventDateText(date)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text(strings.selectDate)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.repeatLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RepeatModeChip(
                            selected = !repeatsMonthly && !repeatsYearly,
                            label = strings.repeatNone,
                        ) {
                            repeatsMonthly = false
                            repeatsYearly = false
                            calendarType = CalendarType.SOLAR
                        }
                        RepeatModeChip(
                            selected = repeatsMonthly,
                            label = strings.repeatMonthly,
                        ) {
                            repeatsMonthly = true
                            repeatsYearly = false
                            calendarType = CalendarType.SOLAR
                        }
                        RepeatModeChip(
                            selected = repeatsYearly,
                            label = strings.repeatYearly,
                        ) {
                            repeatsMonthly = false
                            repeatsYearly = true
                        }
                    }
                }
                if (repeatsMonthly) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = strings.monthlyDayLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items((1..31).toList()) { day ->
                                FilterChip(
                                    selected = date.dayOfMonth == day,
                                    onClick = { date = LocalDate.of(2000, 1, day) },
                                    label = { Text("$day") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    }
                }
                if (repeatsYearly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = calendarType == CalendarType.SOLAR,
                            onClick = {
                                if (calendarType == CalendarType.LUNAR) {
                                    date = AndroidLunarCalendar.toGregorian(
                                        LocalDate.now().year,
                                        LunarDate(lunarMonth, lunarDay, lunarLeap),
                                    )
                                }
                                calendarType = CalendarType.SOLAR
                            },
                            label = { Text(strings.calendarSolar) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        FilterChip(
                            selected = calendarType == CalendarType.LUNAR,
                            onClick = { calendarType = CalendarType.LUNAR },
                            label = { Text(strings.calendarLunar) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
                if (repeatsYearly && calendarType == CalendarType.LUNAR) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = strings.lunarMonthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items((1..12).toList()) { month ->
                                FilterChip(
                                    selected = lunarMonth == month,
                                    onClick = { lunarMonth = month },
                                    label = { Text("$month") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                        Text(
                            text = strings.lunarDayLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items((1..30).toList()) { day ->
                                FilterChip(
                                    selected = lunarDay == day,
                                    onClick = { lunarDay = day },
                                    label = { Text("$day") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = strings.lunarLeapLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = lunarLeap,
                                onCheckedChange = { lunarLeap = it },
                                colors = switchColors,
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.dayEventReminder,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = strings.dayEventReminderDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = switchColors,
                        )
                    }
                    if (reminderEnabled) {
                        Text(
                            text = strings.dayEventReminderDays(reminderDaysBefore),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items((1..7).toList()) { day ->
                                FilterChip(
                                    selected = reminderDaysBefore == day,
                                    onClick = { reminderDaysBefore = day },
                                    label = { Text("$day") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(100) },
                    label = { Text(strings.dayEventNote) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        onSave(
                            DayEventDraft(
                                id = initialDraft.id,
                                name = name,
                                date = date,
                                repeatsMonthly = repeatsMonthly,
                                repeatsYearly = repeatsYearly,
                                reminderEnabled = reminderEnabled,
                                reminderDaysBefore = reminderDaysBefore,
                                note = note.trim().ifBlank { null },
                                calendarType = calendarType,
                                lunarMonth = if (calendarType == CalendarType.LUNAR) lunarMonth else null,
                                lunarDay = if (calendarType == CalendarType.LUNAR) lunarDay else null,
                                lunarLeap = if (calendarType == CalendarType.LUNAR) lunarLeap else false,
                            ),
                        )
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(strings.save)
                }
                if (initialDraft.id.isNotBlank() && onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    ) {
                        Text(strings.confirmDelete)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        PulseDatePickerDialog(
            initialDate = date,
            onConfirm = { picked ->
                date = picked
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun RepeatModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}
