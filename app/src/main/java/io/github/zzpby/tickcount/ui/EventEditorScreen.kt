package io.github.zzpby.tickcount.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import io.github.zzpby.tickcount.domain.Lunar
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.domain.MAX_BATCH_COUNT
import io.github.zzpby.tickcount.domain.MAX_BATCH_STEP_DAYS
import io.github.zzpby.tickcount.domain.batchOf
import io.github.zzpby.tickcount.ui.components.HueSlider
import io.github.zzpby.tickcount.ui.components.PlusIcon
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import io.github.zzpby.tickcount.ui.theme.FieldShape
import io.github.zzpby.tickcount.ui.theme.hairline
import io.github.zzpby.tickcount.ui.theme.PillShape
import io.github.zzpby.tickcount.ui.theme.tintWash
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Names a countdown, gives it any number of tags, and picks the moment it counts
 * to — either the whole day, or an exact minute.
 *
 * A screen rather than a dialog, because of the keyboard and the batch. A dialog is sized by
 * its content, so anything inside it that changes height — a section appearing, a keyboard
 * taking a share of the window — resizes the window, which re-measures the content, which
 * resizes the window again. That loop is what made this form flicker: the fields jumped
 * between two arrangements every frame. A screen has the window it has, and its own scrolling
 * is the only thing that moves, so there is nothing left to oscillate.
 *
 * The several fields are local state rather than a copy of a stored countdown:
 * nothing is written until Save, so leaving really does discard. [initial] carries the id
 * through untouched, which is what lets the home-screen widget stay bound to a countdown
 * whose date has been changed.
 *
 * Clearing the name is treated as "remove this countdown", which keeps the screen down to one
 * primary action and matches what people expect from a rename box.
 *
 * A new countdown can also be added as a *batch*: one answer about the gap between them and
 * one about how many, and a string of days comes out carrying the same name, time, tags and
 * colour. It is offered only while adding, because what it does is add countdowns — an edit
 * screen that quietly produced nine more of the thing being edited would be a trap.
 */
@SuppressLint("MutableCollectionMutableState")
@Composable
fun EventEditorScreen(
    initial: CountdownEvent,
    isNew: Boolean,
    onSave: (CountdownEvent) -> Unit,
    /** Asked instead of [onSave] when the batch switch is on: the whole batch, first in. */
    onSaveMany: (List<CountdownEvent>) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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
    var lunarPickerOpen by rememberSaveable { mutableStateOf(false) }
    var lunarMonth by rememberSaveable(initial.id) { mutableIntStateOf(initial.lunarMonth ?: 0) }
    var lunarDay by rememberSaveable(initial.id) { mutableIntStateOf(initial.lunarDay ?: 0) }
    var lunarLeap by rememberSaveable(initial.id) { mutableStateOf(initial.lunarLeap) }
    var timePickerOpen by rememberSaveable { mutableStateOf(false) }

    // Null means "no colour of its own". Kept separately from the hue so that
    // unticking the box and ticking it again returns the colour last mixed.
    var coloured by rememberSaveable(initial.id) { mutableStateOf(initial.colorHue != null) }
    var hue by rememberSaveable(initial.id) { mutableFloatStateOf(initial.colorHue ?: DEFAULT_HUE) }

    // The batch: whether it is on, and the two numbers that describe it. Held as the text
    // that was typed rather than as an Int, so that clearing a field to type another number
    // in is not read as a zero — see `intOr` below.
    var batch by rememberSaveable(initial.id) { mutableStateOf(false) }
    var batchStepText by rememberSaveable(initial.id) { mutableStateOf(DEFAULT_BATCH_STEP) }
    var batchCountText by rememberSaveable(initial.id) { mutableStateOf(DEFAULT_BATCH_COUNT) }

    val date = LocalDate.ofEpochDay(epochDay)
    val dateFormatter = rememberDateFormatter(R.string.date_format_full)
    val timeFormatter = rememberDateFormatter(R.string.time_format)

    val batchStep = intOr(batchStepText, DEFAULT_BATCH_STEP_NUMBER)
    val batchCount = intOr(batchCountText, DEFAULT_BATCH_COUNT_NUMBER)

    /** What has been filled in so far, as the one countdown the batch will copy. */
    fun filledIn(): CountdownEvent = initial.copy(
        title = title.trim(),
        tags = tags.toList(),
        date = date,
        time = if (timed) LocalTime.of(minuteOfDay / 60, minuteOfDay % 60) else null,
        colorHue = hue.takeIf { coloured },
        lunarMonth = lunarMonth.takeIf { it in 1..12 },
        lunarDay = lunarDay.takeIf { it in 1..30 },
        lunarLeap = lunarLeap && lunarMonth in 1..12,
    )

    /**
     * The batch the two number fields describe, or nothing while either of them is still
     * empty — there is no such thing as half a countdown.
     *
     * Rebuilt whenever anything the batch is made of changes, and not otherwise. That keeps
     * the ids stable while the preview is on screen: a list minted afresh on every frame
     * could not be saved as the batch that was shown before it was added.
     */
    val futures = remember(
        batchStepText,
        batchCountText,
        title,
        tags,
        epochDay,
        timed,
        minuteOfDay,
        hue,
        coloured,
        initial,
    ) {
        if (batchStepText.isEmpty() || batchCountText.isEmpty()) {
            emptyList()
        } else {
            batchOf(
                filledIn(),
                stepDays = batchStep,
                count = batchCount,
                createdAt = initial.createdAt,
            )
        }
    }

    // The batch as it stands, which is both what the last day on the preview line is read
    // from and what the screen goes on to add. Empty while the switch is off, and while
    // editing rather than adding — a batch adds countdowns, and an edit screen that quietly
    // produced nine more of the thing being edited would be a trap.
    val batchEvents = if (batch && isNew) futures else emptyList()

    /**
     * The whole batch, the countdown being filled in included, or null while there is not
     * one to add.
     *
     * Null while either number field is empty: with nothing said about the gap or the count
     * there is nothing to add, and the button that adds it stays disabled rather than
     * guessing a number on the user's behalf.
     */
    val wholeBatch = if (batchEvents.isNotEmpty()) listOf(filledIn()) + batchEvents else null

    Column(modifier = modifier.fillMaxSize()) {
        // The screen's own actions, along the top: save on the right where the title bar's
        // action slot sits on every other screen, and delete on the left where a form of this
        // kind usually keeps the destructive one — out of the way of the thumb that is saving.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }

            Spacer(Modifier.width(6.dp))

            // The one filled control on the screen: saving is what the form is for, and on a
            // page of hairline outlines a solid pill is the only thing that reads as the way
            // out of it.
            Button(
                // A blank name with existing tags would be a countdown with nothing
                // to show, so the name is the one field that has to be there. A batch
                // also has to be whole before it can be added.
                enabled = title.isNotBlank() && (!batch || wholeBatch != null),
                onClick = {
                    val batch = wholeBatch
                    if (batch == null) {
                        onSave(filledIn())
                    } else {
                        onSaveMany(batch)
                    }
                },
                shape = PillShape,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
            ) {
                Text(
                    stringResource(
                        if (batch && isNew) R.string.action_add else R.string.action_save
                    )
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Everything below the actions scrolls. On a screen rather than in a dialog, this is
        // the only thing that ever moves: the fields can be as tall as they like, and the
        // screen does not change size underneath them.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(CountdownEvent.MAX_TITLE_LENGTH) },
                    label = { Text(stringResource(R.string.field_name)) },
                    placeholder = { Text(stringResource(R.string.field_name_hint)) },
                    singleLine = true,
                    shape = FieldShape,
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
                FieldRow(
                    label = stringResource(R.string.field_date),
                    value = date.format(dateFormatter),
                    onClick = { datePickerOpen = true },
                )

                // Directly under the date: the lunar day is another way of saying the same
                // day, and the all-day / exact-minute choice is about that day too, so the
                // two belong together rather than below a strip about colour.
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FieldShape)
                        .clickable { lunarPickerOpen = true }
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .hairline(FieldShape)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.field_lunar),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (lunarMonth in 1..12) {
                            lunarLabel(lunarMonth, lunarDay.coerceAtLeast(1), lunarLeap)
                        } else {
                            stringResource(R.string.lunar_unset)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (lunarMonth in 1..12) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

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

                // Colour comes after the day is settled: it says how the countdown looks,
                // not which day it is.
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

                if (!isNew) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.dialog_clear_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Adding several at once is offered where a countdown is being added, and
                // never while one is being edited: what it does is add countdowns.
                if (isNew) {
                    Spacer(Modifier.height(14.dp))

                    // On a tint of the accent, so the optional half of the form reads as
                    // optional: everything above is what the countdown is, and this is what
                    // can also be made out of it.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FieldShape)
                            .background(tintWash())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.field_batch),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = batch, onCheckedChange = { batch = it })
                    }

                    if (batch) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BatchNumberField(
                                value = batchStepText,
                                onValueChange = { batchStepText = it },
                                label = stringResource(R.string.field_batch_interval),
                                maximum = MAX_BATCH_STEP_DAYS,
                                imeAction = ImeAction.Next,
                                modifier = Modifier.weight(1f),
                            )
                            BatchNumberField(
                                value = batchCountText,
                                onValueChange = { batchCountText = it },
                                label = stringResource(R.string.field_batch_count),
                                maximum = MAX_BATCH_COUNT,
                                imeAction = ImeAction.Done,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        batchEvents.lastOrNull()?.let { last ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = pluralStringResource(
                                    R.plurals.field_batch_preview,
                                    batchEvents.size,
                                    batchEvents.size,
                                    last.date.format(dateFormatter),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.field_batch_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    }
                }
            }
    }

    if (lunarPickerOpen) {
        LunarPickerDialog(
            initial = date,
            onPick = { lunar ->
                val solar = Lunar.toSolar(lunar)
                if (solar != null) {
                    epochDay = solar.toEpochDay()
                    lunarMonth = lunar.month
                    lunarDay = lunar.day
                    lunarLeap = lunar.leap
                }
                lunarPickerOpen = false
            },
            onDismiss = { lunarPickerOpen = false },
        )
    }
    if (datePickerOpen) {
        DatePickerSheet(
            initial = date,
            onPick = {
                epochDay = it.toEpochDay()
                // Chosen as a solar day, so there is no lunar day left to name.
                lunarMonth = 0
                lunarDay = 0
                lunarLeap = false
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
 * One of the batch's two numbers: the gap between entries, or how many there are.
 *
 * A text field typed into rather than a stepper, because a gap is often a number nobody
 * wants to reach by tapping — 30 days, 365 days. Input is filtered to digits and refused
 * outright above [maximum]: a value that is out of range is not stored, so the number on
 * screen is always the number the batch will use.
 */
@Composable
private fun BatchNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maximum: Int,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { typed ->
            val digits = typed.filter { it.isDigit() }.take(4)
            if (digits.isEmpty() || digits.toIntOrNull()?.let { it <= maximum } == true) {
                onValueChange(digits)
            }
        },
        label = { Text(label) },
        singleLine = true,
        shape = FieldShape,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
        modifier = modifier,
    )
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
        shape = FieldShape,
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
        shape = FieldShape,
        // The panel colour, not the card colour: a card is white in the white and teal
        // themes, which left the date and time rows as the only white things on a tinted
        // page. This is the tone that moves with the theme.
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(16.dp))
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

/**
 * What the batch fields hold when they are first shown: a gap of two days and two more of
 * the same countdown.
 *
 * Small numbers, because the fields are what the switch reveals and a batch of two is what
 * someone reaching for the switch is most likely to want. Any other gap or count is typed.
 */
private const val DEFAULT_BATCH_STEP = "2"
private const val DEFAULT_BATCH_STEP_NUMBER = 2
private const val DEFAULT_BATCH_COUNT = "2"
private const val DEFAULT_BATCH_COUNT_NUMBER = 2

/**
 * [text] as a number, or [fallback] while it is empty.
 *
 * The fields are digit-filtered as they are typed into, so an empty one is the only thing
 * that can fail to parse — and it means "not answered yet", which the preview reads as
 * nothing to add rather than as a zero. A cleared field therefore disables Add instead of
 * quietly adding a countdown a day for the next two days.
 */
private fun intOr(text: String, fallback: Int): Int = text.toIntOrNull() ?: fallback


/**
 * Picks a lunar date, and shows the solar day it lands on as it is chosen.
 *
 * Steppers rather than a wheel calendar: the interesting lunar dates are a handful per year,
 * and stepping to "8-15" is faster than scrolling a grid that has to explain leap months.
 */
@Composable
private fun LunarPickerDialog(
    initial: LocalDate,
    onPick: (Lunar.Date) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = remember(initial) {
        Lunar.fromSolar(initial) ?: Lunar.Date(initial.year, 1, 1)
    }
    var year by remember { mutableIntStateOf(start.year) }
    var month by remember { mutableIntStateOf(start.month) }
    var leap by remember { mutableStateOf(start.leap) }
    var day by remember { mutableIntStateOf(start.day) }

    val leapMonthOfYear = Lunar.leapMonth(year)
    val effectiveLeap = leap && leapMonthOfYear == month
    val maxDay = Lunar.daysIn(year, month, effectiveLeap).coerceAtLeast(1)
    val effectiveDay = day.coerceIn(1, maxDay)
    val solar = Lunar.toSolar(Lunar.Date(year, month, effectiveDay, effectiveLeap))
    val formatter = rememberDateFormatter(R.string.date_format_full)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lunar_picker_title)) },
        text = {
            Column {
                StepperRow(stringResource(R.string.lunar_year), year, 1900..2100) { year = it }
                StepperRow(stringResource(R.string.lunar_month), month, 1..12) { month = it }
                StepperRow(stringResource(R.string.lunar_day), effectiveDay, 1..maxDay) { day = it }

                // Only offered when the year actually repeats this month, so the switch
                // cannot be turned on for a leap month that does not exist.
                if (leapMonthOfYear == month) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.lunar_leap),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = effectiveLeap, onCheckedChange = { leap = it })
                    }
                }

                if (solar != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.lunar_lands, solar.format(formatter)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPick(Lunar.Date(year, month, effectiveDay, effectiveLeap)) },
                enabled = solar != null,
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** One "label — − value +" line of [LunarPickerDialog]. */
@Composable
private fun StepperRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onChange((value - 1).coerceIn(range)) }) { Text("-") }
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { onChange((value + 1).coerceIn(range)) }) { Text("+") }
    }
}