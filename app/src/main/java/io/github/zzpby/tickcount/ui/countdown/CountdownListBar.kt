package io.github.zzpby.tickcount.ui.countdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.CountdownListEntry
import io.github.zzpby.tickcount.ui.components.ChevronIcon
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A long rounded rectangle, deliberately not a pill — the pill is the name. */
private val BAR_SHAPE = RoundedCornerShape(16.dp)
private val ROW_SHAPE = RoundedCornerShape(12.dp)

/** How far a past countdown's text is faded. Enough to read, clearly retired. */
private const val PAST_ALPHA = 0.45f

/** Gap between the bar and the panel floating under it, so it reads as detached. */
private val POPUP_GAP = 6.dp

/** How many rows are visible before the list starts scrolling. */
private const val MAX_VISIBLE_ROWS = 4

/** Spacer above the date line plus the row's own vertical padding. */
private val ROW_EXTRA = 20.dp

/** Used only if the theme ever leaves a line height unspecified. */
private val TITLE_LINE_FALLBACK = 24.sp
private val DATE_LINE_FALLBACK = 16.sp

/**
 * The rounded bar above the name pill. It shows which countdown is selected and
 * opens a floating list of every saved one.
 *
 * The list is a [Popup] — a window of its own, not part of this layout — so
 * opening it leaves the calendar exactly where it was instead of shoving it down
 * the screen. It is anchored to the bar and sized to match it, and capped at
 * [MAX_VISIBLE_ROWS] rows; anything longer scrolls.
 *
 * Only the header row toggles. That has to be so: if the whole surface were
 * clickable, tapping a row would close the list instead of navigating.
 *
 * [selectedName] is null when the selected day has no countdown of its own, in
 * which case the bar still has to say something — hence the same placeholder the
 * name pill uses.
 */
@Composable
fun CountdownListBar(
    entries: List<CountdownListEntry>,
    selectedName: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = rememberDateFormatter(R.string.date_format_full)
    val density = LocalDensity.current
    val typography = MaterialTheme.typography
    val hasSelection = selectedName != null

    // The panel is a separate window, so it cannot inherit the bar's size the way
    // a child composable would; it has to be told.
    var barSize by remember { mutableStateOf(IntSize.Zero) }

    // Four rows, measured from the theme's own line heights rather than guessed.
    // Deriving it means the cap still means "four rows" after the user turns up
    // the accessibility font scale, instead of quietly fitting three.
    val panelMaxHeight = with(density) {
        val titleLine = typography.bodyLarge.lineHeight.takeOrElse { TITLE_LINE_FALLBACK }
        val dateLine = typography.bodySmall.lineHeight.takeOrElse { DATE_LINE_FALLBACK }
        (titleLine.toDp() + dateLine.toDp() + ROW_EXTRA) * MAX_VISIBLE_ROWS
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { barSize = it.size }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = BAR_SHAPE,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(
                            if (expanded) R.string.action_collapse_list
                            else R.string.action_expand_list
                        ),
                        onClick = onToggle,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedName ?: stringResource(R.string.label_no_countdown),
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (hasSelection) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                // One chevron rotated, rather than two assets.
                ChevronIcon(
                    pointsLeft = true,
                    size = 18.dp,
                    modifier = Modifier.rotate(if (expanded) 90f else -90f),
                )
            }
        }

        // Guarded on the measured size: before the first layout pass there is
        // nothing to anchor to, and a zero-width panel would flash.
        if (expanded && barSize.width > 0) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, barSize.height + with(density) { POPUP_GAP.roundToPx() }),
                onDismissRequest = onDismiss,
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier
                        .width(with(density) { barSize.width.toDp() })
                        .heightIn(max = panelMaxHeight),
                    shape = BAR_SHAPE,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    // Lifts the panel off the calendar it now covers.
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = panelMaxHeight)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (entries.isEmpty()) {
                            Text(
                                text = stringResource(R.string.label_no_countdown),
                                style = typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            )
                        } else {
                            entries.forEach { entry ->
                                CountdownRow(
                                    entry = entry,
                                    dateFormatter = dateFormatter,
                                    onClick = { onSelect(entry.event.date) },
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * One saved countdown: its name over its date, both flush left.
 *
 * The two lines carry the relationship the user asked for — the name at full
 * strength, the date lighter — expressed through the theme's `onSurface` /
 * `onSurfaceVariant` pair rather than a hard-coded black, so it still reads
 * correctly in dark mode and under wallpaper-derived colour.
 */
@Composable
private fun CountdownRow(
    entry: CountdownListEntry,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
) {
    val alpha = if (entry.isPast) PAST_ALPHA else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(ROW_SHAPE)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(
            text = entry.event.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = entry.event.date.format(dateFormatter),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
