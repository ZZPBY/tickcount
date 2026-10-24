package io.github.zzpby.tickcount.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.AppSettings
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.notify.Reminders
import io.github.zzpby.tickcount.ui.components.ChoicePill
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import io.github.zzpby.tickcount.ui.theme.CardShape
import io.github.zzpby.tickcount.ui.theme.FieldShape
import io.github.zzpby.tickcount.ui.theme.Gutter
import io.github.zzpby.tickcount.ui.theme.PillShape
import io.github.zzpby.tickcount.ui.theme.hairline
import io.github.zzpby.tickcount.ui.theme.tintWash
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The reminder, and whether it will actually arrive.
 *
 * Laid out as a statement first and a list of facts second: the top strip says when the next
 * notification is due, the three tiles under it say whether anything is standing in its way,
 * and the settings only appear once the reminder is on. A settings page that leads with
 * switches asks the reader to work out what they add up to.
 */
@Composable
fun NotificationsScreen(
    settings: AppSettings,
    next: Reminders.Planned?,
    onRemind: (Boolean) -> Unit,
    onMinutes: (Int) -> Unit,
    onLead: (Int) -> Unit,
    onEventLead: (String, Int?) -> Unit,
    events: List<CountdownEvent>,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Bumped by the refresh action: the permission is read from the system, and Compose has
    // no reason to re-read it when the user comes back from the system settings.
    var refreshKey by remember { mutableIntStateOf(0) }
    var timeDialogOpen by remember { mutableStateOf(false) }
    var leadDialogOpen by remember { mutableStateOf(false) }

    // Coming back from the system settings is the one moment the answer can change without
    // this screen hearing a click, and the system offers no callback for it.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshKey++ }

    // Read from the system, so it is re-read when the refresh action or a permission answer
    // says to. Which switch is missing decides what the screen can offer: below Android 13
    // there is no permission to ask for, and the system settings are the only way back.
    val granted = remember(refreshKey) { Reminders.canNotify(context) }
    val permissionMissing = remember(refreshKey) { Reminders.needsPermission(context) }
    val scheduled = settings.remind && next != null
    var eventQuery by rememberSaveable { mutableStateOf("") }
    var eventFilter by rememberSaveable { mutableStateOf(EventFilter.ALL.name) }
    val upcoming = remember(events) {
        events.filter { !it.date.isBefore(java.time.LocalDate.now()) }.sortedBy { it.date }
    }
    val shown = upcoming.filter { event ->
        val query = eventQuery.trim()
        val matchesQuery = query.isEmpty() ||
            event.title.contains(query, ignoreCase = true) ||
            event.tags.any { it.contains(query, ignoreCase = true) }
        val matchesFilter = when (eventFilter) {
            EventFilter.CUSTOM.name -> event.remindLeadMinutes != null
            EventFilter.GLOBAL.name -> event.remindLeadMinutes == null
            else -> true
        }
        matchesQuery && matchesFilter
    }
    var eventDialogId by remember { mutableStateOf<String?>(null) }
    val nextText = when {
        !settings.remind -> stringResource(R.string.notify_off_summary)
        next == null -> stringResource(R.string.notify_none)
        else -> rememberDateFormatter(R.string.date_format_full)
            .format(next.at.atZone(ZoneId.systemDefault())) + " " +
            timeText(minutesOf(next))
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        if (ok) onRemind(true)
        refreshKey++
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Gutter),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_notifications),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(16.dp))

        // ---------------------------------------------------------- what will happen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(tintWash())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notify_next),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = nextText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = settings.remind,
                onCheckedChange = { want ->
                    when {
                        !want -> onRemind(false)
                        granted -> onRemind(true)
                        permissionMissing -> notificationPermission.launch(
                            Reminders.POST_NOTIFICATIONS
                        )
                        // Nothing this app may ask for: the switch is in the system settings,
                        // and opening it is the only way to make the reminder audible.
                        else -> Reminders.openNotificationSettings(context)
                    }
                },
            )
        }

        Spacer(Modifier.height(10.dp))

        // ---------------------------------------------------------- anything in the way
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                value = stringResource(
                    when {
                        granted -> R.string.notify_granted
                        permissionMissing -> R.string.notify_not_granted
                        // The permission is there; the system's own switch is off.
                        else -> R.string.notify_blocked
                    }
                ),
                label = stringResource(R.string.notify_permission),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = if (scheduled) "1" else "0",
                label = stringResource(R.string.notify_pending),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = stringResource(
                    if (settings.remind) R.string.notify_on else R.string.notify_off
                ),
                label = stringResource(R.string.notify_switch),
                modifier = Modifier.weight(1f),
            )
        }

        if (settings.remind) {
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .hairline(CardShape)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FieldShape)
                        .clickable { timeDialogOpen = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.notify_time_all_day),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = timeText(settings.remindMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // The reminder time is only consulted for countdowns that do not name a
                // time of their own, and a row that looks universal without saying so reads
                // as a setting that has been ignored.
                Text(
                    text = stringResource(R.string.notify_time_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FieldShape)
                        .clickable { leadDialogOpen = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.notify_advance),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = leadText(settings.remindLeadMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ---------------------------------------------------------- per countdown
        if (upcoming.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .hairline(CardShape)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(
                    text = stringResource(R.string.notify_events_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.notify_events_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = eventQuery,
                    onValueChange = { eventQuery = it },
                    placeholder = { Text(stringResource(R.string.notify_search_hint)) },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(
                        label = stringResource(R.string.notify_filter_all),
                        selected = eventFilter == EventFilter.ALL.name,
                        onClick = { eventFilter = EventFilter.ALL.name },
                    )
                    ChoicePill(
                        label = stringResource(R.string.notify_custom),
                        selected = eventFilter == EventFilter.CUSTOM.name,
                        onClick = { eventFilter = EventFilter.CUSTOM.name },
                    )
                    ChoicePill(
                        label = stringResource(R.string.notify_follow_global),
                        selected = eventFilter == EventFilter.GLOBAL.name,
                        onClick = { eventFilter = EventFilter.GLOBAL.name },
                    )
                }
                Spacer(Modifier.height(10.dp))

                if (shown.isEmpty()) {
                    Text(
                        text = stringResource(R.string.notify_events_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Three rows high, however many there are: the card is a part of a
                    // settings page, not a list to fill the screen. The bar beside it says
                    // how much is below the fold.
                    val scroll = rememberScrollState()
                    Row(modifier = Modifier.height(EVENT_VIEWPORT)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scroll),
                        ) {
                            shown.forEachIndexed { index, event ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                EventRow(
                                    event = event,
                                    moment = momentText(event, settings),
                                    customised = event.remindLeadMinutes != null,
                                    onClick = { eventDialogId = event.id },
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        ScrollIndicator(state = scroll, viewport = EVENT_VIEWPORT)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // ---------------------------------------------------------- prove it works
        Button(
            onClick = {
                when {
                    granted -> onTest()
                    permissionMissing -> notificationPermission.launch(
                        Reminders.POST_NOTIFICATIONS
                    )
                    else -> Reminders.openNotificationSettings(context)
                }
            },
            shape = PillShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.notify_test))
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { refreshKey++ }) {
                Text(stringResource(R.string.notify_refresh))
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    if (permissionMissing) {
                        notificationPermission.launch(Reminders.POST_NOTIFICATIONS)
                    } else {
                        Reminders.openNotificationSettings(context)
                    }
                },
                enabled = !granted,
            ) {
                Text(
                    stringResource(
                        if (permissionMissing) R.string.notify_grant
                        else R.string.notify_open_settings
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.notify_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        Spacer(Modifier.height(28.dp))
    }

    val editing = events.firstOrNull { it.id == eventDialogId }
    if (editing != null) {
        EventLeadDialog(
            title = editing.title,
            current = editing.remindLeadMinutes,
            onPick = {
                onEventLead(editing.id, it)
                eventDialogId = null
            },
            onDismiss = { eventDialogId = null },
        )
    }
    if (leadDialogOpen) {
        LeadDialog(
            current = settings.remindLeadMinutes,
            onPick = {
                onLead(it)
                leadDialogOpen = false
            },
            onDismiss = { leadDialogOpen = false },
        )
    }

    if (timeDialogOpen) {
        TimeDialog(
            current = settings.remindMinutes,
            onPick = {
                onMinutes(it)
                timeDialogOpen = false
            },
            onDismiss = { timeDialogOpen = false },
        )
    }
}

/** Minutes after midnight for the instant a reminder was planned at, in local time. */
private fun minutesOf(planned: Reminders.Planned): Int {
    val local = planned.at.atZone(ZoneId.systemDefault()).toLocalTime()
    return local.hour * 60 + local.minute
}

/** "On time", or the offset written in the largest whole unit it fits. */
@Composable
private fun leadText(minutes: Int): String = when {
    minutes <= 0 -> stringResource(R.string.notify_lead_none)
    minutes % (24 * 60) == 0 -> {
        val days = minutes / (24 * 60)
        pluralStringResource(R.plurals.notify_lead_days, days, days)
    }
    minutes % 60 == 0 -> {
        val hours = minutes / 60
        pluralStringResource(R.plurals.notify_lead_hours, hours, hours)
    }
    else -> pluralStringResource(R.plurals.notify_lead_minutes, minutes, minutes)
}

/** 780 becomes `13:00`. */
private fun timeText(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("HH:mm"))

/**
 * One fact about the reminder, in a small square.
 *
 * A number first and a word under it, so the three tiles read as a row of readings rather than
 * as three sentences to parse.
 */
@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(FieldShape)
            .hairline(FieldShape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Picks how early the reminder arrives, in any number of minutes, hours or days.
 *
 * One number and one unit rather than a list of choices: the useful offsets are as varied as
 * the things being counted, and a menu would only ever hold the ones somebody thought of.
 */
@Composable
private fun LeadDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var unit by remember {
        mutableIntStateOf(
            when {
                current >= 24 * 60 && current % (24 * 60) == 0 -> 24 * 60
                current >= 60 && current % 60 == 0 -> 60
                else -> 1
            }
        )
    }
    var amount by remember { mutableStateOf((current / unit).toString()) }

    fun total(): Int = ((amount.toIntOrNull() ?: 0) * unit)
        .coerceIn(0, Reminders.MAX_LEAD_MINUTES)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notify_lead_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        // Digits only: the unit pills say what the number means, so a sign or
                        // a decimal point would only make it ambiguous.
                        amount = input.filter { it.isDigit() }.take(5)
                    },
                    label = { Text(stringResource(R.string.notify_lead_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to R.string.notify_lead_unit_minutes,
                        60 to R.string.notify_lead_unit_hours,
                        24 * 60 to R.string.notify_lead_unit_days,
                    ).forEach { (size, label) ->
                        ChoicePill(
                            label = stringResource(label),
                            selected = unit == size,
                            onClick = {
                                val kept = total()
                                unit = size
                                amount = (kept / size).toString()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = leadText(total()) + " · " + stringResource(R.string.notify_lead_max),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(total()) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Picks the time the reminder arrives.
 *
 * Half-hour steps and four presets rather than a wheel: a reminder for a countdown is set
 * once and then left alone, and a wheel is three gestures to change something most people
 * will never change twice.
 */
@Composable
private fun TimeDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notify_time)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { minutes = (minutes + 1440 - 30) % 1440 }) {
                        Text("\u2212")
                    }
                    Text(
                        text = timeText(minutes),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextButton(onClick = { minutes = (minutes + 30) % 1440 }) {
                        Text("+")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8 * 60, 9 * 60, 12 * 60, 21 * 60).forEach { preset ->
                        ChoicePill(
                            label = timeText(preset),
                            selected = minutes == preset,
                            onClick = { minutes = preset },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(minutes) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** The moment a countdown's reminder falls, as a date and a time. */
@Composable
private fun momentText(event: CountdownEvent, settings: AppSettings): String {
    val at = Reminders.momentOf(event, settings.remindMinutes, settings.remindLeadMinutes)
        .atZone(ZoneId.systemDefault())
    val date = rememberDateFormatter(R.string.date_format_short).format(at)
    return date + " " + at.format(DateTimeFormatter.ofPattern("HH:mm"))
}

/** One countdown, with the moment it will be reminded at. */
@Composable
private fun EventRow(
    event: CountdownEvent,
    moment: String,
    customised: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (customised) {
                    moment + " · " + stringResource(R.string.notify_custom)
                } else {
                    moment + " · " + stringResource(R.string.notify_follow_global)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "\u203A",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One countdown's own reminder offset.
 *
 * Two answers rather than three: follow the app-wide setting, or name an offset. "On time" is
 * an offset of zero, and giving it a third button would be a menu of one special case.
 */
@Composable
private fun EventLeadDialog(
    title: String,
    current: Int?,
    onPick: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var follow by remember { mutableStateOf(current == null) }
    var unit by remember {
        mutableIntStateOf(
            when {
                (current ?: 0) >= 24 * 60 && (current ?: 0) % (24 * 60) == 0 -> 24 * 60
                (current ?: 0) >= 60 && (current ?: 0) % 60 == 0 -> 60
                else -> 1
            }
        )
    }
    var amount by remember { mutableStateOf(((current ?: 0) / unit).toString()) }

    fun total(): Int = ((amount.toIntOrNull() ?: 0) * unit)
        .coerceIn(0, Reminders.MAX_LEAD_MINUTES)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(
                        label = stringResource(R.string.notify_follow_global),
                        selected = follow,
                        onClick = { follow = true },
                    )
                    ChoicePill(
                        label = stringResource(R.string.notify_custom),
                        selected = !follow,
                        onClick = { follow = false },
                    )
                }
                if (!follow) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { input ->
                            amount = input.filter { it.isDigit() }.take(5)
                        },
                        label = { Text(stringResource(R.string.notify_lead_amount)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to R.string.notify_lead_unit_minutes,
                            60 to R.string.notify_lead_unit_hours,
                            24 * 60 to R.string.notify_lead_unit_days,
                        ).forEach { (size, label) ->
                            ChoicePill(
                                label = stringResource(label),
                                selected = unit == size,
                                onClick = {
                                    val kept = total()
                                    unit = size
                                    amount = (kept / size).toString()
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = leadText(total()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (follow) onPick(null) else onPick(total()) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
/** How much of the per-countdown list is visible: three rows. */
private val EVENT_VIEWPORT = 66.dp * 3

/** Which countdowns the per-event list is narrowed to. */
private enum class EventFilter { ALL, CUSTOM, GLOBAL }

/**
 * A scroll bar for [state].
 *
 * Drawn rather than taken from the platform: the bar Android offers on a scrollable column is
 * invisible until a drag starts, and the point of this one is to say, before anyone touches
 * it, that there are more countdowns below the three on show.
 */
@Composable
private fun ScrollIndicator(state: ScrollState, viewport: Dp) {
    // Drawn rather than laid out: the thumb's position is read inside the draw phase, so a
    // scroll does not recompose the page it belongs to.
    val track = MaterialTheme.colorScheme.outlineVariant
    val thumb = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(viewport)
            .drawBehind {
                val corner = CornerRadius(size.width / 2f)
                drawRoundRect(color = track, cornerRadius = corner)
                val max = state.maxValue
                if (max > 0) {
                    val visible = size.height
                    val ratio = visible / (visible + max)
                    val thumbHeight = (visible * ratio).coerceAtLeast(visible * 0.15f)
                    val top = (visible - thumbHeight) * (state.value.toFloat() / max)
                    drawRoundRect(
                        color = thumb,
                        topLeft = Offset(0f, top),
                        size = Size(size.width, thumbHeight),
                        cornerRadius = corner,
                    )
                }
            },
    )
}