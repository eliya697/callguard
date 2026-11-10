package com.eliya.callguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eliya.callguard.data.PhoneNumbers
import com.eliya.callguard.data.Rule
import com.eliya.callguard.data.RuleMode
import com.eliya.callguard.ui.components.ContactAvatar
import com.eliya.callguard.ui.components.description
import com.eliya.callguard.ui.components.icon
import com.eliya.callguard.ui.components.modeContainerColors
import com.eliya.callguard.ui.components.ringsLabel
import com.eliya.callguard.ui.components.title
import com.eliya.callguard.data.Schedule
import com.eliya.callguard.ui.components.dayLetter
import com.eliya.callguard.ui.components.formatMinute
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorSheet(
    draft: Rule,
    isNew: Boolean,
    canHangUp: Boolean,
    onRequestCallControl: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit,
    onDelete: (Rule) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var mode by remember(draft.id) { mutableStateOf(draft.mode) }
    var rings by remember(draft.id) { mutableFloatStateOf(draft.rings.toFloat()) }
    var silent by remember(draft.id) { mutableStateOf(draft.silent) }
    var scheduleOn by remember(draft.id) { mutableStateOf(draft.schedule != null) }
    var schedule by remember(draft.id) { mutableStateOf(draft.schedule ?: Schedule()) }
    val canSave = !scheduleOn || schedule.days.isNotEmpty()

    fun close(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContactAvatar(draft.name, draft.photoUri, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(draft.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        draft.numbers.joinToString(" · ") { PhoneNumbers.pretty(it) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "מה יקרה כשהוא מתקשר?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RuleMode.entries.forEach { option ->
                    ModeOption(mode = option, selected = mode == option, onSelect = { mode = option })
                }
            }

            AnimatedVisibility(visible = mode == RuleMode.HANG_UP) {
                HangUpSettings(
                    rings = rings,
                    onRings = { rings = it },
                    silent = silent,
                    onSilent = { silent = it },
                    canHangUp = canHangUp,
                    onRequestCallControl = onRequestCallControl,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "מתי?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(12.dp))
            ScheduleSettings(
                enabled = scheduleOn,
                onEnabled = { scheduleOn = it },
                schedule = schedule,
                onSchedule = { schedule = it },
            )

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isNew) {
                    TextButton(onClick = { close { onDelete(draft) } }) {
                        Text("הסרה", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { close(onDismiss) }) { Text("ביטול") }
                Spacer(Modifier.width(8.dp))
                Button(enabled = canSave, onClick = {
                    val updated = draft.copy(
                        mode = mode,
                        rings = rings.roundToInt(),
                        silent = silent,
                        schedule = if (scheduleOn) schedule else null,
                        enabled = true,
                    )
                    close { onSave(updated) }
                }) { Text(if (isNew) "הוספה" else "שמירה") }
            }
        }
    }
}

@Composable
private fun ModeOption(mode: RuleMode, selected: Boolean, onSelect: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (accent, onAccent) = modeContainerColors(mode)
    val border by animateColorAsState(if (selected) scheme.primary else scheme.outlineVariant, label = "border")
    val bg by animateColorAsState(if (selected) scheme.surfaceContainerLowest else scheme.surfaceContainerLow, label = "bg")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        shape = MaterialTheme.shapes.large,
        color = bg,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(mode.icon, contentDescription = null, tint = onAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(mode.title, style = MaterialTheme.typography.titleMedium)
                Text(mode.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSettings(
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    schedule: Schedule,
    onSchedule: (Schedule) -> Unit,
) {
    // Which time is being edited: true = start, false = end, null = no dialog.
    var editingStart by remember { mutableStateOf<Boolean?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("רק בשעות מסוימות", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (enabled) "מחוץ לשעות האלה השיחות יגיעו כרגיל" else "הכלל פעיל כל הזמן",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabled)
            }

            AnimatedVisibility(visible = enabled) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField(
                            label = "משעה",
                            minute = schedule.startMinute,
                            onClick = { editingStart = true },
                            modifier = Modifier.weight(1f),
                        )
                        TimeField(
                            label = "עד שעה",
                            minute = schedule.endMinute,
                            onClick = { editingStart = false },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (schedule.endMinute < schedule.startMinute) {
                        Text(
                            "עובר את חצות, ומסתיים למחרת ב-${formatMinute(schedule.endMinute)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else if (schedule.endMinute == schedule.startMinute) {
                        Text(
                            "שעת התחלה וסיום זהות: הכלל פעיל כל היום בימים שנבחרו",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("בימים", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        (Calendar.SUNDAY..Calendar.SATURDAY).forEach { day ->
                            DayToggle(
                                label = dayLetter(day),
                                selected = day in schedule.days,
                                onClick = {
                                    val days = if (day in schedule.days) schedule.days - day else schedule.days + day
                                    onSchedule(schedule.copy(days = days))
                                },
                            )
                        }
                    }
                    if (schedule.days.isEmpty()) {
                        Text(
                            "צריך לבחור לפחות יום אחד",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }

    editingStart?.let { isStart ->
        val initial = if (isStart) schedule.startMinute else schedule.endMinute
        TimePickerDialog(
            title = if (isStart) "משעה" else "עד שעה",
            initialMinute = initial,
            onDismiss = { editingStart = null },
            onConfirm = { minute ->
                onSchedule(if (isStart) schedule.copy(startMinute = minute) else schedule.copy(endMinute = minute))
                editingStart = null
            },
        )
    }
}

@Composable
private fun TimeField(label: String, minute: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMinute(minute), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainerHigh, label = "day")
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = bg,
        contentColor = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(title: String, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // The clock face reads naturally left-to-right, even in a Hebrew UI.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("אישור") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )
}

@Composable
private fun HangUpSettings(
    rings: Float,
    onRings: (Float) -> Unit,
    silent: Boolean,
    onSilent: (Boolean) -> Unit,
    canHangUp: Boolean,
    onRequestCallControl: () -> Unit,
) {
    val count = rings.roundToInt()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("ניתוק אחרי", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                Text(ringsLabel(count), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Text(
                    "כ-${count * PhoneNumbers.SECONDS_PER_RING} שניות",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(value = rings, onValueChange = onRings, valueRange = 1f..10f, steps = 8)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("השתק בזמן ההמתנה", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "הטלפון לא יצלצל עד הניתוק",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = silent, onCheckedChange = onSilent)
            }

            if (!canHangUp) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "לניתוק אוטומטי צריך לאשר הרשאת ניהול שיחות.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onRequestCallControl) { Text("אישור") }
                    }
                }
            }
        }
    }
}
