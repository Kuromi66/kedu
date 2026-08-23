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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    var repeatsYearly by remember(initialDraft.id) { mutableStateOf(initialDraft.repeatsYearly) }
    var note by remember(initialDraft.id) { mutableStateOf(initialDraft.note ?: "") }
    var calendarType by remember(initialDraft.id) { mutableStateOf(initialDraft.calendarType) }
    var lunarMonth by remember(initialDraft.id) { mutableStateOf(initialDraft.lunarMonth ?: 1) }
    var lunarDay by remember(initialDraft.id) { mutableStateOf(initialDraft.lunarDay ?: 1) }
    var lunarLeap by remember(initialDraft.id) { mutableStateOf(initialDraft.lunarLeap) }
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
                if (calendarType == CalendarType.SOLAR) {
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
                                text = strings.dayEventDateText(date),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.repeatsYearly,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = strings.repeatsYearlyDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = repeatsYearly,
                        onCheckedChange = { checked ->
                            repeatsYearly = checked
                            if (!checked) calendarType = CalendarType.SOLAR
                        },
                        colors = switchColors,
                    )
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
                                repeatsYearly = repeatsYearly,
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
