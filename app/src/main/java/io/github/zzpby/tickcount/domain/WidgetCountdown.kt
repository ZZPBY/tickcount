package io.github.zzpby.tickcount.domain

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

private const val SECONDS_PER_DAY = 86_400L

/**
 * What a home-screen widget needs in order to draw itself.
 *
 * A widget cannot be redrawn every second — Android allows one periodic update per
 * half hour at best — so the line is split in two. [days] is static text refreshed
 * on that slow schedule; [secondsToBoundary] feeds a `Chronometer`, which the
 * system ticks on its own without waking the app at all.
 *
 * The boundary is always the nearer **midnight**, which is what keeps the live part
 * at `HH:MM:SS`. Counting down to a date months away would otherwise render as
 * thousands of hours.
 */
data class WidgetCountdown(
    val phase: CountdownPhase,
    /** Whole days counted to, or since, the target's midnight. */
    val days: Long,
    /** The part of the interval inside the current day; always under 24 hours. */
    val secondsToBoundary: Long,
    /** True while the live clock counts down, false once it counts up. */
    val countingDown: Boolean,
)

/** The calendar units a countdown can be spelled out in, largest first. */
enum class CountdownUnit { YEAR, MONTH, DAY }

/** How many of one calendar unit a countdown is worth. */
data class UnitCount(val unit: CountdownUnit, val count: Long)

/**
 * How much a widget instance has room to show.
 *
 * Not a layout choice — the widget uses one layout at every size, and this only
 * decides which rows of it are visible. See [widgetShapeFor] for the thresholds.
 */
enum class WidgetShape {
    /** Room for the live clock and nothing else. */
    CLOCK,

    /** The clock plus the countdown, stacked. */
    MINI,

    /** One line: name, countdown and clock side by side. */
    ROW,

    /** Everything: name, countdown, clock and the target date. */
    STACK,
}

/**
 * Picks the arrangement that fits a widget of [widthDp] by [heightDp].
 *
 * The numbers come from the launcher's own cell arithmetic and are therefore only
 * approximate — a "4x2" widget is around 250x110dp on a stock grid, but launchers
 * disagree about cell size. They are picked so the common sizes land sensibly:
 * roughly, under 150dp wide there is no room for the clock *beside* anything, and
 * under 80dp tall there is only ever one line.
 *
 * A size of zero means the launcher has not measured the widget yet. [STACK] is
 * what the initial layout draws, so staying there avoids a visible reflow.
 */
fun widgetShapeFor(widthDp: Int, heightDp: Int): WidgetShape = when {
    widthDp <= 0 -> WidgetShape.STACK
    widthDp < 150 -> if (heightDp < 80) WidgetShape.CLOCK else WidgetShape.MINI
    heightDp < 80 -> WidgetShape.ROW
    else -> WidgetShape.STACK
}

/**
 * Picks the calendar units worth showing for [parts], dropping the ones that are
 * zero.
 *
 * The app's own line keeps a *trailing* zero — "1年00个月00天" — because its slots
 * are fixed-width and blanking one in the middle would read as a glitch. A widget
 * has no such slots, so exactly one year is simply "1 year", which is how anyone
 * would say it out loud. Order is always largest first, and an all-zero countdown
 * yields an empty list for the caller to fall back on.
 */
fun breakdownOf(parts: TimeParts): List<UnitCount> = listOfNotNull(
    parts.years.takeIf { it > 0 }?.let { UnitCount(CountdownUnit.YEAR, it) },
    parts.months.takeIf { it > 0 }?.let { UnitCount(CountdownUnit.MONTH, it) },
    parts.days.takeIf { it > 0 }?.let { UnitCount(CountdownUnit.DAY, it) },
)

/**
 * Splits the interval between [now] and midnight on [date] into whole days and the
 * remainder inside the current day.
 *
 * The remainder is computed by subtraction rather than with `Duration.minusDays`,
 * which only exists from Java 9 and is absent from Android's `java.time`.
 */
fun widgetCountdownFor(date: LocalDate, now: ZonedDateTime): WidgetCountdown {
    val target = date.atStartOfDay(now.zone)
    val wholeDays: (Duration) -> Long = { it.seconds / SECONDS_PER_DAY }
    val remainder: (Duration, Long) -> Long = { d, days -> d.seconds - days * SECONDS_PER_DAY }

    val remaining = Duration.between(now, target)
    if (remaining.seconds > 0) {
        val days = wholeDays(remaining)
        return WidgetCountdown(
            phase = CountdownPhase.FUTURE,
            days = days,
            secondsToBoundary = remainder(remaining, days),
            countingDown = true,
        )
    }

    val elapsed = Duration.between(target, now)
    val days = wholeDays(elapsed)
    return WidgetCountdown(
        // The target's own midnight has passed once we are on the day itself; that
        // is CountdownPhase.TODAY, and it is what the app shows too.
        phase = if (now.toLocalDate() == date) CountdownPhase.TODAY else CountdownPhase.PAST,
        days = days,
        secondsToBoundary = remainder(elapsed, days),
        countingDown = false,
    )
}
