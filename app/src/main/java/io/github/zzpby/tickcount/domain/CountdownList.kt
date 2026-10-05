package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One row of the countdown list: a saved countdown, whether its day has already gone
 * by, and whether it opens the section of ones that have.
 */
data class CountdownListEntry(
    val event: CountdownEvent,
    val isPast: Boolean,
    /** True on the first row of the past section, which the home list heads. */
    val opensPastSection: Boolean = false,
)

/**
 * How the list is ordered.
 *
 * Named for what each does to the countdowns still ahead, which is the part of the
 * list anyone is reading it for. The past section keeps its own order — see
 * [countdownList] — so "ascending" would have been a claim the list as a whole no
 * longer keeps.
 */
enum class SortOrder {
    /** The nearest date first. */
    DATE_ASC,

    /** The furthest date first. */
    DATE_DESC,

    /** Most recently added first. */
    ADDED,
}

/**
 * Every saved countdown as a list: pinned first, then the ones still ahead, then the
 * ones already behind.
 *
 * The split is the point. Ordering the whole list by date alone put a countdown from
 * years ago above tomorrow's, so the further back the oldest one was, the more the top
 * of the list became a record of things that no longer matter. What is coming is what
 * the list is for, so it leads; what has gone is kept and dimmed under its own heading.
 *
 * Within the part still ahead the chosen order applies. The past part always runs most
 * recent first whatever the order says, because a record reads that way, and within one
 * day it reads forwards like any other day.
 *
 * Pinned countdowns lead the whole list, past or not: keeping one at the top is what
 * pinning means, and a pin the list could sort away would not be one. The heading still
 * belongs to the section rather than to whichever row happens to sit above it, so a
 * pinned past countdown does not drag the heading up with it.
 *
 * A countdown counts as past only once its *day* is behind us. Today's own countdown is
 * not past, which matches [CountdownPhase.TODAY] and is why the comparison is against
 * [today] rather than the current instant.
 */
fun countdownList(
    events: List<CountdownEvent>,
    today: LocalDate,
    order: SortOrder = SortOrder.DATE_ASC,
): List<CountdownListEntry> {
    val pinned = events.filter { it.pinned }
    val (past, ahead) = events.filterNot { it.pinned }.partition { it.date.isBefore(today) }

    val ordered = pinned.sortedWith(byOrder(order)) +
        ahead.sortedWith(byOrder(order)) +
        past.sortedWith(PAST_ORDER)

    val pastStart = pinned.size + ahead.size
    return ordered.mapIndexed { index, event ->
        CountdownListEntry(
            event = event,
            isPast = event.date.isBefore(today),
            opensPastSection = past.isNotEmpty() && index == pastStart,
        )
    }
}

/**
 * The order of the past section: the most recent day first, and within a day the
 * earliest time first, the same way every other day in the app reads.
 *
 * It ignores [SortOrder] on purpose. Reversing it along with the section above would
 * put the oldest thing in the app directly under the heading, which is the arrangement
 * the split exists to get away from.
 */
private val PAST_ORDER: Comparator<CountdownEvent> =
    compareByDescending<CountdownEvent> { it.epochDay }
        .thenBy { it.time }
        .thenBy { it.title }

/**
 * The comparator for [order].
 *
 * Each order ends in the same date-and-title tie-break, so a list never shuffles between
 * two draws for a reason the user cannot see.
 *
 * The tie-break matters most for [SortOrder.ADDED]: countdowns saved before the added-at
 * stamp existed all carry zero, and the fallback keeps those in date order rather than in
 * whatever order the store happened to read them back in.
 */
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
