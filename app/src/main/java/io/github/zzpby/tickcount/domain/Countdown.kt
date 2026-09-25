package io.github.zzpby.tickcount.domain

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
    companion object {
        /** Every component zero — what a day with no saved countdown renders as. */
        val ZERO = TimeParts(0, 0, 0, 0, 0, 0)
    }
}

/** A countdown for one date, as observed at one instant. */
data class Countdown(
    val phase: CountdownPhase,
    val parts: TimeParts,
)

/** Width of each field of the countdown pattern, in display order: yyyy MM dd HH mm ss. */
private val SLOT_WIDTHS = intArrayOf(4, 2, 2, 2, 2, 2)

/**
 * Formats [parts] into the six slots of the countdown line, in display order:
 * years, months, days, hours, minutes, seconds.
 *
 * A slot is blanked with dashes the width of its pattern letter only while it is
 * still *leading* — that is, while it and every larger unit are zero. So a
 * countdown under a year reads `----年04月11日 06时30分15秒`, while a zero that
 * sits *between* counting units is a real number: two days and thirty seconds
 * reads `----年--月02日 00时00分30秒`, not a screen full of dashes.
 *
 * Values that overflow their slot (a five-digit year) are passed through rather
 * than truncated — silently showing a wrong number would be worse.
 */
fun formatSlots(parts: TimeParts): List<String> {
    val values = longArrayOf(
        parts.years,
        parts.months,
        parts.days,
        parts.hours,
        parts.minutes,
        parts.seconds,
    )

    var leading = true
    return values.mapIndexed { index, value ->
        if (value != 0L) leading = false
        val width = SLOT_WIDTHS[index]
        if (leading) "-".repeat(width) else padToWidth(value, width)
    }
}

private fun padToWidth(value: Long, width: Int): String {
    val digits = value.toString()
    return if (digits.length >= width) digits else "0".repeat(width - digits.length) + digits
}

/**
 * Works out the countdown for [date] as observed at [now].
 *
 * The countdown targets **00:00 local time on [date]**, which is the only
 * definition under which "how long until the 6th" is unambiguous. Once that
 * midnight has passed the same numbers are reported as elapsed time instead, so
 * the display simply switches from "Time left" to "Time since".
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
