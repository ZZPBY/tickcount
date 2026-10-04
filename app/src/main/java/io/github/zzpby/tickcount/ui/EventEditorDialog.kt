package io.github.zzpby.tickcount.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.ui.components.HueSlider
import io.github.zzpby.tickcount.ui.components.PlusIcon
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Names a countdown, gives it any number of tags, and picks the moment it counts
 * to — either the whole day, or an exact minute.
 *
 * The several fields are local state rather than a copy of a stored countdown:
 * nothing is written until Save, so dismissing really does discard. [initial]
 * carries the id through untouched, which is what lets the home-screen widget stay
 * bound to a countdown whose date has been changed.
 *
 * Clearing the name is treated as "remove this countdown", which keeps the dialog
 * down to one primary action and matches what people expect from a rename box.
 */
@SuppressLint("MutableCollectionMutableState")
@Composable
fun EventEditorDialog(
    initial: CountdownEvent,
    isNew: Boolean,
    onSave: (CountdownEvent) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    // An ArrayList because that is what a Bundle can carry across a rotation, and
    // replaced rather than mutated on every edit, so the lint warning about a
    // mutable collection in state does not apply here.
    var tags by rememberSaveable(initial.id) { mutableStateOf(ArrayList(initial.tags)) }
    var epochDay by rememberSaveable(initial.id) { mutableLongStateOf(initial.epochDay) }
    var timed by rememberSaveable(initial.id) { mutableStateOf(initial.hasTime) }
    var minuteOfDay by rememberSaveable(initial.id) {
        mutableIntStateOf(initial.time?.let { it.hour * 60 + it.minute } ?: DEFAULT_MINUTE)
    }

    var tagDraft by rememberSaveable(initial.id) { mutableStateOf("") }
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    var timePickerOpen by rememberSaveable { mutableStateOf(false) }

    // Null means "no colour of its own". Kept separately from the hue so that
    // unticking the box and ticking it again returns the colour last mixed.
    var coloured by rememberSaveable(initial.id) { mutableStateOf(initial.colorHue != null) }
    var hue by rememberSaveable(initial.id) { mutableFloatStateOf(initial.colorHue ?: DEFAULT_HUE) }

    val date = LocalDate.ofEpochDay(epochDay)
    val dateFormatter = rememberDateFormatter(R.string.date_format_full)
    val timeFormatter = rememberDateFormatter(R.string.time_format)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.dialog_new_title else R.string.dialog_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(CountdownEvent.MAX_TITLE_LENGTH) },
                    label = { Text(stringResource(R.string.field_name)) },
                    placeholder = { Text(stringResource(R.string.field_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                TagEditor(
                    tags = tags,
                    draft = tagDraft,
                    onDraftChange = { tagDraft = it.take(CountdownEvent.MAX_TAG_LENGTH) },
                    onAdd = {
                        val tag = tagDraft.trim()
                        if (tag.isNotEmpty() && tags.size < CountdownEvent.MAX_TAG_COUNT) {
                            tags = ArrayList(tags + tag)
                        }
                        tagDraft = ""
                    },
                    onRemove = { tag -> tags = ArrayList(tags - tag) },
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.field_color),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = coloured, onCheckedChange = { coloured = it })
                }
                if (coloured) {
                    HueSlider(hue = hue, onHueChange = { hue = it })
                }

                Spacer(Modifier.height(12.dp))
                FieldRow(
                    label = stringResource(R.string.field_date),
                    value = date.format(dateFormatter),
                    onClick = { datePickerOpen = true },
                )

                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !timed,
                        onClick = { timed = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text(stringResource(R.string.field_all_day)) }
                    SegmentedButton(
                        selected = timed,
                        onClick = { timed = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text(stringResource(R.string.field_exact_minute)) }
                }

                if (timed) {
                    Spacer(Modifier.height(8.dp))
                    FieldRow(
                        label = stringResource(R.string.field_time),
                        value = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
                            .format(timeFormatter),
                        onClick = { timePickerOpen = true },
                    )
                }

                if (!isNew) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.dialog_clear_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                // A blank name with existing tags would be a countdown with nothing
                // to show, so the name is the one field that has to be there.
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            tags = tags.toList(),
                            date = date,
                            time = if (timed) LocalTime.of(minuteOfDay / 60, minuteOfDay % 60) else null,
                            colorHue = hue.takeIf { coloured },
                        )
                    )
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )

    if (datePickerOpen) {
        DatePickerSheet(
            initial = date,
            onPick = {
                epochDay = it.toEpochDay()
                datePickerOpen = false
            },
            onDismiss = { datePickerOpen = false },
        )
    }

    if (timePickerOpen) {
        TimePickerSheet(
            initialMinute = minuteOfDay,
            onPick = {
                minuteOfDay = it
                timePickerOpen = false
            },
            onDismiss = { timePickerOpen = false },
        )
    }
}

/**
 * The tags field: a box to type one in, then the ones already added below it as
 * removable chips.
 *
 * Tags are entered one at a time rather than as a comma-separated string so that a
 * tag containing a comma stays a single tag.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditor(
    tags: List<String>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val full = tags.size >= CountdownEvent.MAX_TAG_COUNT

    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = draft,
        onValueChange = onDraftChange,
        label = { Text(stringResource(R.string.field_tags)) },
        placeholder = { Text(stringResource(R.string.field_tags_hint)) },
        singleLine = true,
        enabled = !full,
        trailingIcon = {
            IconButton(onClick = onAdd, enabled = draft.isNotBlank() && !full) {
                PlusIcon()
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = ImeAction.Done,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { onAdd() },
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    if (tags.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = { onRemove(tag) },
                    label = { Text(tag) },
                    trailingIcon = {
                        Text("×", style = MaterialTheme.typography.titleMedium)
                    },
                )
            }
        }
    }
}

/** A label and a value that opens a picker when tapped. */
@Composable
private fun FieldRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The Material picker speaks in UTC milliseconds, so the conversion has to go
    // through UTC in both directions or the day shifts by the zone offset.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(initialMinute: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = true,
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = state, modifier = Modifier.padding(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        }
    }
}

/** Where the time switch lands when it is turned on without a time having been chosen. */
private const val DEFAULT_MINUTE = 9 * 60

/** Where the colour switch lands when it is turned on without a colour having been mixed. */
private const val DEFAULT_HUE = 222f
