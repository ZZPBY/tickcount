package com.tickcount.app.domain

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Which side of the selected date "now" falls on. */
enum class CountdownPhase {
    /** The date is still ahead of us. */
    FUTURE,

    /** The selected date is today, so its midnight has already passed. */
    TODAY,

    /** The date has already happened. */
    PAST,
}

/**
 * The units a countdown is broken into, largest first.
 *
 * The order of the constants is the display order and is relied upon by
 * [TimeParts.significantUnits].
 */
enum class TimeUnit {
    YEARS,
    MONTHS,
    DAYS,
    HOURS,
    MINUTES,
    SECONDS,
}

/**
 * A countdown broken into calendar and clock components.
 *
 * Years, months and days are *calendar* arithmetic (a month is a month, however
 * long it is), while hours, minutes and seconds are elapsed time. That is the
 * only way "1 year 2 months 5 days" can mean anything, and it is why the split is
 * computed with [ChronoUnit] rather than by dividing a millisecond total.
 */
data class TimeParts(
    val years: Long,
    val months: Long,
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
) {
    val isZero: Boolean
        get() = years == 0L && months == 0L && days == 0L &&
            hours == 0L && minutes == 0L && seconds == 0L

    fun value(unit: TimeUnit): Long = when (unit) {
        TimeUnit.YEARS -> years
        TimeUnit.MONTHS -> months
        TimeUnit.DAYS -> days
        TimeUnit.HOURS -> hours
        TimeUnit.MINUTES -> minutes
        TimeUnit.SECONDS -> seconds
    }

    /**
     * The units worth putting on screen: everything from the largest non-zero
     * unit down to seconds.
     *
     * Leading zeros are dropped so a three-day countdown reads "3 days 4 hours"
     * rather than "0 years 0 months 3 days 4 hours". Trailing units are always
     * kept, which is what makes the seconds visibly tick.
     */
    fun significantUnits(): List<TimeUnit> {
        val all = TimeUnit.entries
        val first = all.indexOfFirst { value(it) != 0L }
        return when {
            first < 0 -> listOf(TimeUnit.SECONDS)
            else -> all.subList(first, all.size)
        }
    }
}

/** A countdown for one date, as observed at one instant. */
data class Countdown(
    val phase: CountdownPhase,
    val parts: TimeParts,
)

/**
 * Works out the countdown for [date] as observed at [now].
 *
 * The countdown targets **00:00 local time on [date]**, which is the only
 * definition under which "how long until the 6th" is unambiguous. Once that
 * midnight has passed the same numbers are reported as elapsed time instead, so
 * the display simply switches from "还有" to "已过去".
 */
fun countdownTo(date: LocalDate, now: ZonedDateTime): Countdown {
    val target = date.atStartOfDay(now.zone)
    return when {
        target.isAfter(now) -> Countdown(CountdownPhase.FUTURE, decompose(now, target))
        now.toLocalDate() == date -> Countdown(CountdownPhase.TODAY, decompose(target, now))
        else -> Countdown(CountdownPhase.PAST, decompose(target, now))
    }
}

/**
 * Splits the interval `from..to` into years, months, days, hours, minutes and
 * seconds.
 *
 * Each step asks the calendar for the largest whole number of units that still
 * fits, then moves the cursor forward by exactly that much — so the components
 * always add back up to the original interval, including across leap days and
 * daylight-saving transitions. [from] must not be after [to].
 */
fun decompose(from: ZonedDateTime, to: ZonedDateTime): TimeParts {
    var cursor = from

    fun largestWhole(unit: ChronoUnit, plus: (ZonedDateTime, Long) -> ZonedDateTime): Long {
        val whole = unit.between(cursor, to)
        val advanced = plus(cursor, whole)
        val adjusted = if (advanced.isAfter(to)) whole - 1 else whole
        cursor = plus(cursor, adjusted)
        return adjusted.coerceAtLeast(0L)
    }

    val years = largestWhole(ChronoUnit.YEARS) { t, n -> t.plusYears(n) }
    val months = largestWhole(ChronoUnit.MONTHS) { t, n -> t.plusMonths(n) }
    val days = largestWhole(ChronoUnit.DAYS) { t, n -> t.plusDays(n) }
    val hours = largestWhole(ChronoUnit.HOURS) { t, n -> t.plusHours(n) }
    val minutes = largestWhole(ChronoUnit.MINUTES) { t, n -> t.plusMinutes(n) }
    val seconds = ChronoUnit.SECONDS.between(cursor, to).coerceAtLeast(0L)

    return TimeParts(
        years = years,
        months = months,
        days = days,
        hours = hours,
        minutes = minutes,
        seconds = seconds,
    )
}
