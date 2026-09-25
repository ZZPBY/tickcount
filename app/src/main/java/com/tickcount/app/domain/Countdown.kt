package com.tickcount.app.domain

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Which side of "today" the selected date falls on. */
enum class CountdownPhase {
    /** The date is still ahead of us. */
    FUTURE,

    /** The date is today. */
    TODAY,

    /** The date has already happened. */
    PAST,
}

/**
 * The countdown to (or since) a calendar date, split into the two numbers the
 * UI shows.
 *
 * There are deliberately two independent values here rather than one duration,
 * because "how many days until the 6th" and "how long until the clock strikes
 * midnight" are genuinely different questions:
 *
 *  - [days] is the *calendar day* difference, which is what "距离某日还有多少天"
 *    means. Someone who picks a date two days out expects to read "2", even at
 *    23:00 on the first day, when only 25 hours remain.
 *  - [dayProgressMillis] is the position within the *current local day*: the time
 *    left until the next midnight for [CountdownPhase.FUTURE]/[CountdownPhase.TODAY],
 *    or the time elapsed since the last midnight for [CountdownPhase.PAST].
 *
 * [dayProgressMillis] is what makes the digits tick, and it rolls over to a new
 * day exactly when [days] decrements (or increments, for past dates), so the two
 * numbers can never disagree.
 */
data class Countdown(
    val phase: CountdownPhase,
    /** Absolute calendar-day distance between today and the target date. */
    val days: Long,
    /** Milliseconds until the next local midnight, or since the last one. */
    val dayProgressMillis: Long,
) {
    /** `remaining` while counting down, `elapsed` once the date has passed. */
    val isCountingDown: Boolean get() = phase != CountdownPhase.PAST
}

/**
 * Formats a millisecond count as `HH:MM:SS`, saturating at zero.
 *
 * The hours field is *not* wrapped at 24: on a daylight-saving day the local day
 * really can be 25 hours long, and showing `24:30:00` for that hour is honest,
 * whereas wrapping it to `00:30:00` would look like a bug.
 */
fun formatDayProgress(millis: Long): String {
    val totalSeconds = Duration.ofMillis(millis.coerceAtLeast(0L)).seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

/**
 * Works out the countdown for [date] as observed at [now].
 *
 * All arithmetic goes through [ZonedDateTime], so DST transitions are handled by
 * the platform: a day that is 23 or 25 hours long still reports "1 day".
 */
fun countdownTo(date: LocalDate, now: ZonedDateTime): Countdown {
    val today = now.toLocalDate()
    val dayDelta = ChronoUnit.DAYS.between(today, date)

    val phase = when {
        dayDelta > 0 -> CountdownPhase.FUTURE
        dayDelta == 0L -> CountdownPhase.TODAY
        else -> CountdownPhase.PAST
    }

    val progress = if (phase == CountdownPhase.PAST) {
        // Time already spent since the target date began.
        Duration.between(today.atStartOfDay(now.zone), now).toMillis()
    } else {
        // Time left until the current local day ends. On a daylight-saving day
        // this is naturally 23 or 25 hours rather than 24.
        Duration.between(now, today.plusDays(1).atStartOfDay(now.zone)).toMillis()
    }

    return Countdown(
        phase = phase,
        days = kotlin.math.abs(dayDelta),
        dayProgressMillis = progress.coerceAtLeast(0L),
    )
}
