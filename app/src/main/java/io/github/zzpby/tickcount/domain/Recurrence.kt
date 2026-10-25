package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.LocalDate
import java.time.YearMonth

/**
 * The day a countdown next lands on, counting from [today].
 *
 * A one-off countdown keeps its own date, even once that is behind us: the list keeps
 * those under its own heading, and moving them would lose what they record. A yearly one
 * moves forward instead, and today's own day counts — so it reads "today" all day and only
 * rolls over to next year at midnight, the same way every other countdown treats the day
 * it is on.
 *
 * A 29 February date lands on the 28th in the years that have no 29th. Skipping the year
 * entirely is what iCalendar does with `BYMONTHDAY=29`, and it would make a leap-day
 * birthday disappear for three years in four — not what writing one down means.
 */
fun CountdownEvent.occurrenceOn(today: LocalDate): LocalDate {
    if (!repeatsYearly) return date
    val thisYear = onMonthDayIn(date, today.year)
    return if (thisYear.isBefore(today)) onMonthDayIn(date, today.year + 1) else thisYear
}

/** The month and day of [date] as they fall in [year], shortened when the day is missing. */
private fun onMonthDayIn(date: LocalDate, year: Int): LocalDate {
    val day = minOf(date.dayOfMonth, YearMonth.of(year, date.month).lengthOfMonth())
    return LocalDate.of(year, date.month, day)
}

/**
 * The countdown as it should be shown on [today].
 *
 * A copy rather than a stored rewrite: what the user entered stays what is saved, and an
 * edit made today does not quietly re-date a birthday to this year in the document.
 */
fun CountdownEvent.resolvedOn(today: LocalDate): CountdownEvent =
    if (repeatsYearly) copy(date = occurrenceOn(today)) else this

/** The same resolution over a whole list, leaving it untouched when nothing repeats. */
fun List<CountdownEvent>.resolvedOn(today: LocalDate): List<CountdownEvent> =
    if (none { it.repeatsYearly }) this else map { it.resolvedOn(today) }

/**
 * The countdowns that still have a day ahead of them on [today], soonest first.
 *
 * The filter runs *after* the resolution, and that order is the whole point: a yearly
 * countdown stored years ago is behind us as stored and ahead of us as it next comes round.
 * Filtering first drops it from every list of what is coming — which is how the reminders
 * page came to lose its per-countdown rows.
 */
fun List<CountdownEvent>.upcomingOn(today: LocalDate): List<CountdownEvent> =
    resolvedOn(today).filterNot { it.date.isBefore(today) }.sortedBy { it.date }
