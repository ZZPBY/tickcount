package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.domain.SearchScope
import io.github.zzpby.tickcount.domain.SortOrder
import io.github.zzpby.tickcount.domain.countdownList
import io.github.zzpby.tickcount.domain.matchesQuery
import io.github.zzpby.tickcount.domain.searchText
import io.github.zzpby.tickcount.ui.calendar.MonthCalendar
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import java.time.LocalDate
import java.time.YearMonth

/**
 * The month, and underneath it whatever the chosen day holds.
 *
 * Choosing a day used to open a dialog, which made the calendar a way of asking a
 * question one day at a time. Showing the day's countdowns below it instead means the
 * grid and the answer to it are on screen together, and the list underneath is the
 * same list as the home screen's — same cards, same search, same order — narrowed to
 * one date.
 */
@Composable
fun CalendarScreen(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    eventsByDate: Map<LocalDate, List<CountdownEvent>>,
    query: String,
    onQueryChange: (String) -> Unit,
    scope: SearchScope,
    onScopeChange: (SearchScope) -> Unit,
    sortOrder: SortOrder,
    onSelectDate: (LocalDate) -> Unit,
    onStepMonth: (Long) -> Unit,
    onMonthClick: () -> Unit,
    onToday: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onLongPressEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = rememberDateFormatter(R.string.date_format_full)
    val timeFormatter = rememberDateFormatter(R.string.time_format)
    val allDay = stringResource(R.string.field_all_day)

    Column(modifier = modifier.fillMaxSize()) {
        MonthCalendar(
            month = month,
            selectedDate = selectedDate,
            today = today,
            events = eventsByDate,
            onSelect = onSelectDate,
            onStepMonth = onStepMonth,
            onMonthClick = onMonthClick,
            onToday = onToday,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        if (selectedDate == null) {
            CentredNotice(stringResource(R.string.calendar_pick_a_day))
            return@Column
        }

        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            scope = scope,
            onScopeChange = onScopeChange,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )

        val entries = countdownList(eventsByDate[selectedDate].orEmpty(), today, sortOrder)
        val visible = entries.filter { entry ->
            matchesQuery(
                haystack = searchText(
                    event = entry.event,
                    scope = scope,
                    // Every card here is the same date, so the line under the name
                    // carries the time instead — the date is the grid above.
                    dateText = entry.event.date.format(dateFormatter),
                    timeText = entry.event.time?.format(timeFormatter) ?: allDay,
                ),
                query = query,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                entries.isEmpty() -> CentredNotice(stringResource(R.string.calendar_day_empty))

                visible.isEmpty() -> CentredNotice(
                    stringResource(R.string.home_no_matches, query.trim())
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                ) {
                    items(visible, key = { it.event.id }) { entry ->
                        EventCard(
                            entry = entry,
                            today = today,
                            detailText = entry.event.time?.format(timeFormatter) ?: allDay,
                            onClick = { onOpenEvent(entry.event.id) },
                            onLongClick = { onLongPressEvent(entry.event.id) },
                        )
                    }
                }
            }
        }
    }
}
