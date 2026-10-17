package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.CountdownListEntry
import io.github.zzpby.tickcount.domain.daysFromToday
import io.github.zzpby.tickcount.ui.theme.CardGap
import io.github.zzpby.tickcount.ui.theme.CardShape
import io.github.zzpby.tickcount.ui.theme.EVENT_WASH_ALPHA
import io.github.zzpby.tickcount.ui.theme.PillShape
import io.github.zzpby.tickcount.ui.theme.eventAccent
import io.github.zzpby.tickcount.ui.theme.hairline
import java.time.LocalDate
import kotlin.math.abs

/**
 * One countdown as a card.
 *
 * The day count is the largest thing on it, in a column of its own on the left, because the
 * number is what the card exists to say: the name only says which countdown it is. Everything
 * else — the date, the time, the tags — is quiet underneath, on one line each, so a list of
 * these can be read by scanning the left column alone.
 *
 * Shared between the home screen and the calendar's day list, which are the same list of the
 * same things seen through two different filters.
 */
@Composable
fun EventCard(
    entry: CountdownListEntry,
    today: LocalDate,
    /**
     * The quiet line under the name. The date on the home list, the time on a calendar day,
     * where every card already shares one date.
     */
    detailText: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val event = entry.event
    val scheme = MaterialTheme.colorScheme
    // Null hue means the countdown has no colour of its own, and the fallback is the theme's
    // accent — which is what keeps the default card looking deliberate rather than like a
    // swatch that failed to load.
    val accent = eventAccent(event.colorHue)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = CardGap / 2)
            .clip(CardShape)
            .background(scheme.surfaceContainerHigh)
            // Only a countdown that was given a colour tints its card. Washing the default
            // would wash it in the theme's own accent, making every card look chosen when
            // none of them are.
            .then(
                if (event.colorHue != null) {
                    Modifier.background(accent.copy(alpha = EVENT_WASH_ALPHA))
                } else {
                    Modifier
                }
            )
            .hairline(CardShape)
            // A long press is the way to the three things a countdown's own screen does
            // without the trip into it; a tap still opens that screen.
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayCount(
            days = daysFromToday(event.date, today),
            isPast = entry.isPast,
            accent = accent,
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A pinned countdown says so on the card; otherwise the only sign of it is
                // that it keeps turning up first, which looks like a bug.
                if (event.pinned) {
                    PinnedBadge()
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (entry.isPast) scheme.onSurfaceVariant else scheme.onSurface,
                )
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = scheme.onSurfaceVariant,
            )

            // The tags get a line of their own rather than sharing one with the date: they
            // are what someone searches by and what tells two similar countdowns apart.
            if (event.tags.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = event.tags.joinToString("   "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = accent,
                )
            }
        }

        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(accent),
        )
    }
}

/**
 * How far off the countdown is, in the three-line form the left column uses: a caption, the
 * number, and what it counts.
 *
 * Today, tomorrow and yesterday stay words. A "0 days" would be arithmetic where a word is
 * shorter and says more, and it would be the one card in the list that reads as a bug.
 */
@Composable
private fun DayCount(days: Long, isPast: Boolean, accent: Color) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.width(70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (days) {
            0L -> CountWord(stringResource(R.string.widget_today), accent)
            1L -> CountWord(stringResource(R.string.widget_tomorrow), accent)
            -1L -> CountWord(stringResource(R.string.list_yesterday), scheme.onSurfaceVariant)
            else -> {
                Text(
                    text = stringResource(
                        if (isPast) R.string.card_caption_past else R.string.card_caption_future
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = abs(days).toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (isPast) scheme.onSurfaceVariant else accent,
                )
                Text(
                    text = stringResource(
                        if (isPast) R.string.card_unit_past else R.string.card_unit_future
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One of the three short words that stands in for a number: today, tomorrow, yesterday. */
@Composable
private fun CountWord(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = color,
    )
}

@Composable
private fun PinnedBadge() {
    Text(
        text = stringResource(R.string.label_pinned),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}
