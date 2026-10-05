package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.CountdownListEntry
import io.github.zzpby.tickcount.domain.SearchScope
import io.github.zzpby.tickcount.domain.matchesQuery
import io.github.zzpby.tickcount.domain.searchText
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Every countdown in one list, with a search box above it.
 *
 * A plain `LazyColumn` rather than the single countdown header this screen used to
 * carry: with a day able to hold several countdowns there is no longer one for a
 * header to describe.
 */
@Composable
fun HomeScreen(
    entries: List<CountdownListEntry>,
    today: LocalDate,
    query: String,
    onQueryChange: (String) -> Unit,
    scope: SearchScope,
    onScopeChange: (SearchScope) -> Unit,
    onOpen: (String) -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = rememberDateFormatter(R.string.date_format_short)
    val timeFormatter = rememberDateFormatter(R.string.time_format)
    val allDay = stringResource(R.string.field_all_day)

    fun dateText(entry: CountdownListEntry) = entry.event.date.format(dateFormatter)

    fun timeText(entry: CountdownListEntry) = entry.event.time?.format(timeFormatter) ?: allDay

    val visible = entries.filter { entry ->
        matchesQuery(
            haystack = searchText(entry.event, scope, dateText(entry), timeText(entry)),
            query = query,
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            scope = scope,
            onScopeChange = onScopeChange,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )

        when {
            entries.isEmpty() -> CentredNotice(stringResource(R.string.home_empty))

            // An empty list after a search is a different thing from an empty list
            // altogether, and saying so is the difference between "nothing here" and
            // "nothing found".
            visible.isEmpty() -> CentredNotice(
                stringResource(R.string.home_no_matches, query.trim())
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
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
 * The heading over the countdowns whose day has gone by.
 *
 * They are still listed — being able to look back is half of what a countdown is for —
 * but they are no longer mixed in with what is coming, which is what the list is read
 * for.
 */
@Composable
private fun PastSectionHeading(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.home_past_section),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
    )
}

/**
 * What a card says under the name: the date, and the time when the countdown names one.
 *
 * The home list is where a countdown is read without opening it, so leaving the time out
 * made one set to 09:30 look exactly like an all-day one. The calendar's own list still
 * shows the time alone, because every card there shares the date in the grid above it.
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
