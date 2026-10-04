package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.domain.CountdownPhase
import io.github.zzpby.tickcount.domain.countdownTo
import io.github.zzpby.tickcount.domain.formatSlots
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import io.github.zzpby.tickcount.ui.theme.eventAccent
import java.time.ZonedDateTime

/**
 * One countdown on a screen of its own: what it is called, exactly how long is
 * left (or has passed), and everything else known about it.
 *
 * The breakdown is the app's original fixed-width line — years through seconds,
 * with the leading fields dashed out — rather than a second, differently worded
 * summary of the same numbers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    event: CountdownEvent,
    now: ZonedDateTime,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countdown = remember(now, event) { countdownTo(event.target(now.zone), now) }
    val line = stringResource(
        R.string.countdown_line_format,
        *formatSlots(countdown.parts).toTypedArray(),
    )
    val dateFormatter = rememberDateFormatter(R.string.date_format_full)
    val timeFormatter = rememberDateFormatter(R.string.time_format)
    val accent = eventAccent(event.colorHue)

    val heading = when (countdown.phase) {
        CountdownPhase.FUTURE -> stringResource(R.string.label_countdown_left)
        CountdownPhase.TODAY -> stringResource(R.string.label_today_elapsed)
        CountdownPhase.PAST -> stringResource(R.string.label_countdown_ago)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = accent,
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = heading,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = line,
            // The six fields run to about two dozen characters, so this is as large as
            // the line goes before it has to wrap. Wrapping is allowed rather than
            // shrinking: the space before 时 is a natural break, and two readable
            // lines beat one cramped one.
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.headlineSmall.fontSize * 1.25f,
        )

        Spacer(Modifier.height(28.dp))

        // First among the cards, because it is the one thing here that changes what
        // other screens do rather than describing this countdown.
        DetailCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.field_pinned),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Switch(checked = event.pinned, onCheckedChange = { onTogglePin() })
            }
        }

        Spacer(Modifier.height(10.dp))
        DetailCard(
            label = stringResource(R.string.field_date),
            value = event.date.format(dateFormatter),
        )

        Spacer(Modifier.height(10.dp))
        DetailCard(
            label = stringResource(R.string.field_time),
            value = event.time?.format(timeFormatter) ?: stringResource(R.string.field_all_day),
        )

        if (event.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            // The heading goes inside the card with the tags rather than above it, so
            // the whole block reads as one thing: these are the tags.
            DetailCard {
                Text(
                    text = stringResource(R.string.field_tags),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    event.tags.forEach { tag -> TagPill(tag) }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

/** The rounded panel both kinds of detail card are cut from. */
@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shaping = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shaping)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shaping)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

/** A label on the left, its value on the right, both inside one panel. */
@Composable
private fun DetailCard(label: String, value: String) {
    DetailCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * A tag, drawn as a pill rather than as a chip.
 *
 * There is nothing to press here, and a chip would ripple when tapped anyway — an
 * invitation that leads nowhere.
 */
@Composable
private fun TagPill(tag: String) {
    Text(
        text = tag,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
