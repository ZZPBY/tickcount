package io.github.zzpby.tickcount.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.AppSettings
import io.github.zzpby.tickcount.notify.Reminders
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
    onAdvance: (Int) -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Bumped by the refresh action: the permission is read from the system, and Compose has
    // no reason to re-read it when the user comes back from the system settings.
    var refreshKey by remember { mutableIntStateOf(0) }
    var timeDialogOpen by remember { mutableStateOf(false) }

    val allowed = remember(refreshKey, settings.remind) { Reminders.canNotify(context) }
    val granted = allowed || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    val scheduled = settings.remind && next != null
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
                        else -> notificationPermission.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                },
            )
        }

        Spacer(Modifier.height(10.dp))

        // ---------------------------------------------------------- anything in the way
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                value = stringResource(
                    if (granted) R.string.notify_granted else R.string.notify_not_granted
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
                        text = stringResource(R.string.notify_time),
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

                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.notify_advance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Reminders.ADVANCE_CHOICES.forEach { days ->
                        ChoicePill(
                            label = advanceLabel(days),
                            selected = settings.remindAdvanceDays == days,
                            onClick = { onAdvance(days) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ---------------------------------------------------------- prove it works
        Button(
            onClick = {
                if (granted) {
                    onTest()
                } else {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                    if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                enabled = !granted,
            ) {
                Text(stringResource(R.string.notify_grant))
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

/** "On the day" for zero, "3 days before" otherwise. */
@Composable
private fun advanceLabel(days: Int): String =
    if (days == 0) {
        stringResource(R.string.notify_advance_same_day)
    } else {
        stringResource(R.string.notify_advance_option, days)
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

/** A small pill that is filled when chosen. */
@Composable
private fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
            .hairline(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                        Text("−")
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
