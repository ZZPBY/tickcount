package com.tickcount.app.data

import java.time.LocalDate

/**
 * A single saved countdown: a name attached to a calendar date.
 *
 * Dates are stored as the date itself (not an instant) because the whole app
 * counts *calendar days*, and a calendar day is a local-time concept. The
 * countdown always targets 00:00 local time on [date].
 */
data class CountdownEvent(
    val date: LocalDate,
    val title: String,
    /** Index into the accent palette, so each countdown can have its own colour. */
    val colorIndex: Int = 0,
) {
    val epochDay: Long get() = date.toEpochDay()

    companion object {
        const val MAX_TITLE_LENGTH = 60
        const val COLOR_COUNT = 6
    }
}
