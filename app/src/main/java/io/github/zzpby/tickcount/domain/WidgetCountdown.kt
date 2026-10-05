package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
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
 * The boundary is the countdown's own moment while that is less than a day away, and
 * the nearer **midnight** before that, which is what keeps the live part at
 * `HH:MM:SS`. Counting down to a date months away would otherwise render as
 * thousands of hours.
 */
data class WidgetCountdown(
    /**
     * Calendar days from today to the countdown's day, signed: positive while the day is
     * still ahead, negative once it has gone, zero on the day itself.
     *
     * Calendar days rather than 24-hour blocks, so that it says what the list says: a
     * countdown tomorrow morning is "tomorrow" at any hour of today.
     */
    val days: Long,
    /** The part of the interval inside the current day, or to the moment itself. */
    val secondsToBoundary: Long,
    /** True while the live clock counts down, false once it counts up. */
    val countingDown: Boolean,
)

/** The calendar units a countdown can be spelled out in, largest first. */
enum class CountdownUnit { YEAR, MONTH, DAY }

/**
 * Rows the list widget's layout has.
 *
 * The layout draws this many and hides the ones a widget is too short for, because a
 * `RemoteViews` layout cannot be built to fit: there is no recycling and no measuring, so
 * the choice is between fixed rows and no rows.
 */
const val UPCOMING_WIDGET_ROWS = 4

/** What the list widget spends on its heading strip, its own padding, and one row. */
private const val UPCOMING_HEADER_DP = 18
private const val UPCOMING_PADDING_DP = 16
private const val UPCOMING_ROW_DP = 24

/**
 * How many rows a list widget [heightDp] tall has room for.
 *
 * A widget the launcher has not measured yet draws every row, which is what the initial
 * layout does; drawing none would leave a blank rectangle until the first measurement
 * arrived, which is exactly when the user is looking at it.
 *
 * This decides how many countdowns are worth showing, and nothing else: the rows divide up
 * whatever height the widget has among themselves, so a widget showing fewer of them is
 * still a full widget rather than text stacked on top of an empty strip.
 */
fun upcomingRowsFor(heightDp: Int): Int = when {
    heightDp <= 0 -> UPCOMING_WIDGET_ROWS
    else -> ((heightDp - UPCOMING_HEADER_DP - UPCOMING_PADDING_DP) / UPCOMING_ROW_DP)
        .coerceIn(1, UPCOMING_WIDGET_ROWS)
}

/**
 * The countdowns a widget showing what is coming should put on screen: the next [count]
 * that have not gone by yet, nearest first.
 *
 * The ones already behind are left out rather than pushed to the bottom, and pinning is
 * ignored. A widget has four rows and no scrolling, so a row spent on something that has
 * happened is a row taken from something that has not; and the order that matters on a
 * home screen is the calendar's, since there is no way to change it from there.
 */
fun upcomingCountdowns(
    events: List<CountdownEvent>,
    today: LocalDate,
    count: Int,
): List<CountdownEvent> =
    events
        .filterNot { it.date.isBefore(today) }
        .sortedWith(compareBy({ it.epochDay }, { it.time }, { it.title }))
        .take(count)

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
 * Splits the interval between [now] and the countdown's moment — [date] at [time], or
 * midnight when it names no time — into whole calendar days and the rest.
 *
 * The time of day is honoured, not rounded away: a countdown set to 09:30 is counted to
 * 09:30. Leaving it out made the widget disagree with the app on the very day it mattered,
 * saying "today" and counting up at eight in the morning while the countdown screen was
 * still counting down to half past nine.
 *
 * The remainder is taken to the moment itself while that is under a day away, and to the
 * nearer midnight otherwise. A `Chronometer` is the only part of a widget that can tick by
 * itself, and it can only tick towards one instant: to the midnight keeps the display at
 * `HH:MM:SS` for a countdown months out, and to the moment is the honest answer once the
 * moment is the next thing to happen.
 *
 * The remainder is computed by subtraction rather than with `Duration.minusDays`,
 * which only exists from Java 9 and is absent from Android's `java.time`.
 */
fun widgetCountdownFor(
    date: LocalDate,
    now: ZonedDateTime,
    time: LocalTime? = null,
): WidgetCountdown {
    val target = date.atTime(time ?: LocalTime.MIDNIGHT).atZone(now.zone)
    val days = daysFromToday(date, now.toLocalDate())
    val remaining = Duration.between(now, target)

    if (remaining.isZero || remaining.isNegative) {
        val elapsed = remaining.negated()
        return WidgetCountdown(
            days = days,
            secondsToBoundary = if (elapsed.toHours() < 24L) {
                elapsed.seconds
            } else {
                now.toLocalTime().toSecondOfDay().toLong()
            },
            countingDown = false,
        )
    }

    return WidgetCountdown(
        days = days,
        secondsToBoundary = if (remaining.toHours() < 24L) {
            remaining.seconds
        } else {
            SECONDS_PER_DAY - now.toLocalTime().toSecondOfDay()
        },
        countingDown = true,
    )
}
