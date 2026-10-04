package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.CountdownListEntry
import io.github.zzpby.tickcount.domain.daysFromToday
import io.github.zzpby.tickcount.ui.theme.EVENT_WASH_ALPHA
import io.github.zzpby.tickcount.ui.theme.eventAccent
import java.time.LocalDate

/**
 * One countdown as a card: its name, then its tags, then its date, with how far off
 * it is on the right.
 *
 * Shared between the home screen and the calendar's day list, which are the same
 * list of the same things seen through two different filters.
 */
@Composable
fun EventCard(
    entry: CountdownListEntry,
    today: LocalDate,
    /**
     * The quiet line under the tags. The date on the home list, the time on a
     * calendar day, where every card already shares one date.
     */
    detailText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val event = entry.event
    val scheme = MaterialTheme.colorScheme
    val shaped = RoundedCornerShape(18.dp)
    // Null hue means the countdown has no colour of its own, and the fallback is the
    // theme's accent — which is what keeps the default card looking deliberate rather
    // than like a swatch that failed to load.
    val accent = eventAccent(event.colorHue)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(shaped)
            .background(scheme.surfaceContainerHigh)
            // Only a countdown that was given a colour tints its card. Washing the
            // default would wash it in the theme's own accent, which would make every
            // card look chosen when none of them are.
            .then(
                if (event.colorHue != null) {
                    Modifier.background(accent.copy(alpha = EVENT_WASH_ALPHA))
                } else {
                    Modifier
                }
            )
            .border(1.dp, scheme.outlineVariant, shaped)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A pinned countdown says so on the card; otherwise the only sign of
                // it is that it keeps turning up first, which looks like a bug.
                if (event.pinned) {
                    Text(
                        text = stringResource(R.string.label_pinned),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(scheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (entry.isPast) scheme.onSurfaceVariant else scheme.onSurface,
                )
            }

            // The tags get a line of their own between the name and the date, rather
            // than sharing one with the date: they are what someone searches by and
            // what tells two similar countdowns apart.
            if (event.tags.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = event.tags.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = accent,
                )
            }

            Spacer(Modifier.height(3.dp))
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = scheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = distanceLabel(event.date, today),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            color = if (entry.isPast) scheme.onSurfaceVariant else accent,
        )
    }
}

/** "Today", "Tomorrow", "12 days left", "Yesterday", "3 days ago". */
@Composable
fun distanceLabel(date: LocalDate, today: LocalDate): String {
    val days = daysFromToday(date, today)
    return when {
        days == 0L -> stringResource(R.string.widget_today)
        days == 1L -> stringResource(R.string.widget_tomorrow)
        days == -1L -> stringResource(R.string.list_yesterday)
        days > 1L -> pluralStringResource(R.plurals.list_days_left, days.toInt(), days)
        else -> pluralStringResource(R.plurals.list_days_ago, (-days).toInt(), -days)
    }
}
