package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.CountdownListEntry
import io.github.zzpby.tickcount.domain.SearchScope
import io.github.zzpby.tickcount.domain.SortOrder
import io.github.zzpby.tickcount.domain.matchesQuery
import io.github.zzpby.tickcount.domain.searchText
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import io.github.zzpby.tickcount.ui.theme.Gutter
import io.github.zzpby.tickcount.ui.theme.hairline
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Every countdown in one list, with a title over it and a search box above the list.
 *
 * The title is set at display size rather than in the bar at the top of the window, because it
 * is the name of what the screen is showing and not a navigation label: the bar keeps the
 * buttons, the screen keeps the words. Under it, one line says how many are still ahead, which
 * is the question the list is here to answer at a glance.
 */
@Composable
fun HomeScreen(
    entries: List<CountdownListEntry>,
    today: LocalDate,
    query: String,
    onQueryChange: (String) -> Unit,
    scope: SearchScope,
    onScopeChange: (SearchScope) -> Unit,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onOpen: (String) -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = rememberDateFormatter(R.string.date_format_short)
    val timeFormatter = rememberDateFormatter(R.string.time_format)
    val allDay = stringResource(R.string.field_all_day)

    // Two filters rather than one, because they answer different questions: the tag narrows
    // *what* is listed and the week narrows *when*. Kept in the screen rather than in the
    // settings: they are a way of reading the list right now, not a preference.
    var withinWeek by rememberSaveable { mutableStateOf(false) }
    var tagFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val allTags = remember(entries) {
        entries.flatMap { it.event.tags }.distinct().sorted()
    }

    fun dateText(entry: CountdownListEntry) = entry.event.date.format(dateFormatter)

    fun timeText(entry: CountdownListEntry) = entry.event.time?.format(timeFormatter) ?: allDay

    val visible = entries
        .filter { tagFilter == null || tagFilter in it.event.tags }
        .filter {
            !withinWeek ||
                (!it.isPast && ChronoUnit.DAYS.between(today, it.event.date) in 0..7)
        }
        .filter { entry ->
            matchesQuery(
                haystack = searchText(entry.event, scope, dateText(entry), timeText(entry)),
                query = query,
            )
        }

    Column(modifier = modifier.fillMaxSize()) {
        HomeHeader(entries, today)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                scope = scope,
                onScopeChange = onScopeChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SortButton(current = sortOrder, onPick = onSortChange)
        }

        Spacer(Modifier.height(10.dp))

        FilterRow(
            withinWeek = withinWeek,
            onWithinWeek = { withinWeek = it },
            tags = allTags,
            tagFilter = tagFilter,
            onTag = { tagFilter = if (tagFilter == it) null else it },
        )

        when {
            entries.isEmpty() -> CentredNotice(stringResource(R.string.home_empty))

            // An empty list after a search is a different thing from an empty list
            // altogether, and saying so is the difference between "nothing here" and
            // "nothing found".
            visible.isEmpty() && (query.isNotBlank() || withinWeek || tagFilter != null) ->
                CentredNotice(stringResource(R.string.home_nothing_filtered))

            visible.isEmpty() -> CentredNotice(
                stringResource(R.string.home_no_matches, query.trim())
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Gutter, vertical = 4.dp),
            ) {
                items(visible, key = { it.event.id }) { entry ->
                    if (entry.opensPastSection) {
                        PastSectionHeading()
                    }
                    EventCard(
                        entry = entry,
                        today = today,
                        detailText = cardDetail(entry, dateText(entry), timeFormatter),
                        onClick = { onOpen(entry.event.id) },
                        onLongClick = { onLongPress(entry.event.id) },
                    )
                }
            }
        }
    }
}

/**
 * The screen's own title, and the one line that summarises the list under it.
 *
 * Both are left out when there is nothing to count yet: "0 countdowns coming up" over an
 * empty screen says less than the empty screen's own message, which also says what to do.
 */
@Composable
private fun HomeHeader(entries: List<CountdownListEntry>, today: LocalDate) {
    val upcoming = entries.count { !it.isPast }
    // How many of those land in the current year, which is the time horizon most people
    // plan a birthday or a deadline against.
    val thisYear = entries.count { !it.isPast && it.event.date.year == today.year }
    val summary = when {
        entries.isEmpty() -> null
        upcoming > 0 -> pluralStringResource(R.plurals.home_summary, upcoming, upcoming)
        else -> pluralStringResource(R.plurals.home_summary_past, entries.size, entries.size)
    }
    val yearPart = if (thisYear > 0) {
        pluralStringResource(R.plurals.home_this_year, thisYear, thisYear)
    } else {
        null
    }
    val line = listOfNotNull(summary, yearPart).joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Gutter, end = Gutter, top = 2.dp, bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (line.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The two quick filters, above the list and under the search box.
 *
 * Nothing is shown when there is nothing to filter by: a row holding only "All" would be a
 * control that cannot change anything, and on an empty list it would be the loudest thing on
 * the screen.
 */
@Composable
private fun FilterRow(
    withinWeek: Boolean,
    onWithinWeek: (Boolean) -> Unit,
    tags: List<String>,
    tagFilter: String?,
    onTag: (String) -> Unit,
) {
    if (tags.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Gutter, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(
            label = stringResource(R.string.filter_next_7),
            selected = withinWeek,
            onClick = { onWithinWeek(!withinWeek) },
        )
        tags.forEach { tag ->
            FilterPill(
                label = tag,
                selected = tagFilter == tag,
                onClick = { onTag(tag) },
            )
        }
    }
}

/** One pill of [FilterRow]: outlined when off, tinted when on. */
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
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
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .hairline(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/**
 * The heading over the countdowns whose day has gone by.
 *
 * They are still listed — being able to look back is half of what a countdown is for —
 * but they are no longer mixed in with what is coming, which is what the list is read for.
 */
@Composable
private fun PastSectionHeading(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.home_past_section),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
    )
}

/**
 * What a card says under the name: the date, and the time when the countdown names one.
 *
 * The home list is where a countdown is read without opening it, so leaving the time out made
 * one set to 09:30 look exactly like an all-day one. The calendar's own list still shows the
 * time alone, because every card there shares the date in the grid above it.
 */
@Composable
private fun cardDetail(
    entry: CountdownListEntry,
    dateText: String,
    timeFormatter: DateTimeFormatter,
): String {
    val time = entry.event.time ?: return dateText
    return stringResource(R.string.text_with_time, dateText, time.format(timeFormatter))
}

/** A centred line of explanation, for a list that has nothing to show. */
@Composable
fun CentredNotice(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
