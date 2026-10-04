package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One row of the countdown list: a saved countdown plus whether its day has
 * already gone by, which the list uses to dim the row.
 */
data class CountdownListEntry(
    val event: CountdownEvent,
    val isPast: Boolean,
)

/**
 * How the list is ordered.
 *
 * [DATE_ASC] is the original order and the default; the other two are the ones
 * anyone actually asks for — what is coming up furthest away, and what did I just
 * put in.
 */
enum class SortOrder {
    /** Soonest first. */
    DATE_ASC,

    /** Furthest away first. */
    DATE_DESC,

    /** Most recently added first. */
    ADDED,
}

/**
 * Every saved countdown as a list.
 *
 * Same-day countdowns are ordered by their time of day, with the all-day ones
 * first — they target midnight, which is the earliest moment that day can mean.
 * The title breaks any remaining tie so the order never depends on insertion.
 *
 * Past countdowns stay in the list rather than being filtered out: this list is
 * the only place that shows everything at once, and being able to look back at
 * "the birthday that already happened" is the point of showing it. They are only
 * marked, so the UI can dim them.
 *
 * A countdown counts as past only once its *day* is behind us. Today's own
 * countdown is not past, which matches [CountdownPhase.TODAY] and is why the
 * comparison is against [today] rather than the current instant.
 */
fun countdownList(
    events: List<CountdownEvent>,
    today: LocalDate,
    order: SortOrder = SortOrder.DATE_ASC,
): List<CountdownListEntry> =
    events
        .sortedWith(comparatorFor(order))
        .map { event -> CountdownListEntry(event = event, isPast = event.date.isBefore(today)) }

/**
 * The comparator for [order].
 *
 * Every one of them starts by lifting the pinned countdowns and ends in the same
 * date-and-title tie-break, so a list never shuffles between two draws for a reason
 * the user cannot see. The pin leads in every order rather than only in the default
 * one, because "keep this at the top" is what pinning means.
 *
 * The tie-break matters most for [SortOrder.ADDED]: countdowns saved before the
 * added-at stamp existed all carry zero, and the fallback keeps those in date order
 * rather than in whatever order the store happened to read them back in.
 */
private fun comparatorFor(order: SortOrder): Comparator<CountdownEvent> =
    compareByDescending<CountdownEvent> { it.pinned }.then(byOrder(order))

private fun byOrder(order: SortOrder): Comparator<CountdownEvent> = when (order) {
    SortOrder.DATE_ASC -> compareBy({ it.epochDay }, { it.time }, { it.title })

    SortOrder.DATE_DESC -> compareByDescending<CountdownEvent> { it.epochDay }
        .thenByDescending { it.time }
        .thenBy { it.title }

    SortOrder.ADDED -> compareByDescending<CountdownEvent> { it.createdAt }
        .thenBy { it.epochDay }
        .thenBy { it.time }
        .thenBy { it.title }
}

/**
 * Whole days from [today] to [date], signed: positive while the day is still
 * ahead, negative once it has gone, zero on the day itself.
 *
 * Counted in calendar days rather than elapsed time, so a countdown tomorrow
 * morning reads as one day away at any hour of today — which is what a list of
 * dates is expected to say.
 */
fun daysFromToday(date: LocalDate, today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, date)
