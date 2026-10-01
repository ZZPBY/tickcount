package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.LocalDate

/**
 * One row of the countdown list: a saved countdown plus whether its day has
 * already gone by, which the list uses to dim the row.
 */
data class CountdownListEntry(
    val event: CountdownEvent,
    val isPast: Boolean,
)

/**
 * Every saved countdown as a list, oldest date first.
 *
 * Past countdowns stay in the list rather than being filtered out: this list is
 * the only place that shows everything at once, and being able to jump back to
 * "the birthday that already happened" is the point of showing it. They are only
 * marked, so the UI can dim them.
 *
 * A countdown counts as past only once its *day* is behind us. Today's own
 * countdown is not past, which matches [CountdownPhase.TODAY] and is why the
 * comparison is against [today] rather than the current instant.
 */
fun countdownList(
    events: Map<LocalDate, CountdownEvent>,
    today: LocalDate,
): List<CountdownListEntry> =
    events.values
        .sortedBy { it.epochDay }
        .map { event -> CountdownListEntry(event = event, isPast = event.date.isBefore(today)) }
