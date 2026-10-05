package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The widget's split between the static day count and the live clock.
 *
 * The boundaries are what matter: the whole reason for computing a remainder
 * rather than counting to the target outright is that a Chronometer counting to a
 * date months away would display thousands of hours.
 */
class WidgetCountdownTest {

    private val shanghai = ZoneId.of("Asia/Shanghai")
    private val target = LocalDate.of(2026, 10, 1)

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        zone: ZoneId = shanghai,
    ): ZonedDateTime = ZonedDateTime.of(year, month, day, hour, minute, second, 0, zone)

    // ------------------------------------------------------------------- future

    @Test
    fun `splits a far-off target into whole days plus the rest of today`() {
        val result = widgetCountdownFor(target, at(2026, 9, 25, 18, 0, 0))

        assertEquals(6L, result.days)
        assertEquals(6L * 3600, result.secondsToBoundary)
        assertTrue(result.countingDown)
    }

    @Test
    fun `the remainder never reaches a whole day`() {
        // Three years out: the days run to four figures, the clock still does not.
        val result = widgetCountdownFor(LocalDate.of(2029, 10, 1), at(2026, 9, 25, 18, 30, 15))

        assertTrue(result.days > 1000)
        assertTrue(result.secondsToBoundary < 86_400)
    }

    @Test
    fun `the last second before the target is one day away and one second off`() {
        val result = widgetCountdownFor(target, at(2026, 9, 30, 23, 59, 59))

        // A calendar day away, not zero: tomorrow is tomorrow from this side of midnight,
        // which is also what the list says.
        assertEquals(1L, result.days)
        assertEquals(1L, result.secondsToBoundary)
        assertTrue(result.countingDown)
    }

    @Test
    fun `exactly one day out is one day and a full clock`() {
        val result = widgetCountdownFor(target, at(2026, 9, 30, 0, 0, 0))

        assertEquals(1L, result.days)
        // Twenty-four hours is more than the clock's own day, so it counts to midnight and
        // reads 24:00:00 rather than the 00:00:00 the old split produced.
        assertEquals(86_400L, result.secondsToBoundary)
    }

    // -------------------------------------------------------------------- today

    @Test
    fun `the target's own midnight is today with nothing elapsed`() {
        val result = widgetCountdownFor(target, at(2026, 10, 1, 0, 0, 0))

        assertEquals(0L, result.days)
        assertEquals(0L, result.secondsToBoundary)
        assertFalse(result.countingDown)
    }

    @Test
    fun `later on the target day counts up from its midnight`() {
        val result = widgetCountdownFor(target, at(2026, 10, 1, 18, 30, 15))

        assertEquals(0L, result.days)
        assertEquals(18L * 3600 + 30 * 60 + 15, result.secondsToBoundary)
        assertFalse(result.countingDown)
    }

    @Test
    fun `one second past the target's midnight is still today`() {
        val result = widgetCountdownFor(target, at(2026, 10, 1, 0, 0, 1))

        assertEquals(0L, result.days)
        assertEquals(1L, result.secondsToBoundary)
    }

    // --------------------------------------------------------------- time of day

    @Test
    fun `a countdown with a time is counted to that time, not to midnight`() {
        // Half past nine on the first, seen at eight the same morning.
        val result = widgetCountdownFor(target, at(2026, 10, 1, 8, 0, 0), time = LocalTime.of(9, 30))

        assertEquals(0L, result.days)
        assertEquals(90L * 60, result.secondsToBoundary)
        assertTrue("the moment is still ahead", result.countingDown)
    }

    @Test
    fun `a moment later today still counts down rather than up`() {
        // The case that used to disagree with the app: the day's midnight had passed, so
        // the widget called it today and counted up while the app counted down.
        val morning = widgetCountdownFor(target, at(2026, 10, 1, 8, 0, 0), time = LocalTime.of(9, 30))
        val allDay = widgetCountdownFor(target, at(2026, 10, 1, 8, 0, 0))

        assertTrue(morning.countingDown)
        assertFalse("an all-day countdown is past its midnight", allDay.countingDown)
    }

    @Test
    fun `once the time has gone the clock counts up from it`() {
        val result = widgetCountdownFor(target, at(2026, 10, 1, 10, 0, 0), time = LocalTime.of(9, 30))

        assertEquals(0L, result.days)
        assertEquals(30L * 60, result.secondsToBoundary)
        assertFalse(result.countingDown)
    }

    @Test
    fun `a timed countdown tomorrow is a day away at any hour`() {
        val result = widgetCountdownFor(target, at(2026, 9, 30, 23, 0, 0), time = LocalTime.of(9, 30))

        assertEquals(1L, result.days)
        // Ten and a half hours to the moment itself, which is inside the clock's own day.
        assertEquals(10L * 3600 + 30 * 60, result.secondsToBoundary)
        assertTrue(result.countingDown)
    }

    // --------------------------------------------------------------------- past

    @Test
    fun `the day after the target has one whole day behind it`() {
        val result = widgetCountdownFor(target, at(2026, 10, 2, 0, 0, 0))

        // Negative because it is a calendar distance from today, signed: the label turns it
        // back into "1 day ago".
        assertEquals(-1L, result.days)
        assertEquals(0L, result.secondsToBoundary)
        assertFalse(result.countingDown)
    }

    @Test
    fun `a past target counts up from the most recent midnight`() {
        val result = widgetCountdownFor(target, at(2026, 10, 13, 18, 0, 0))

        assertEquals(-12L, result.days)
        assertEquals(18L * 3600, result.secondsToBoundary)
        assertFalse(result.countingDown)
    }

    @Test
    fun `a past target is never on zero days`() {
        // Yesterday at exactly midnight is already a whole day of elapsed time.
        val result = widgetCountdownFor(LocalDate.of(2026, 9, 30), at(2026, 10, 1, 0, 0, 0))

        assertEquals(-1L, result.days)
    }

    // ------------------------------------------------------------------ rebuild

    @Test
    fun `the day count is the calendar distance, whenever it is asked`() {
        listOf(
            at(2026, 9, 25, 18, 43, 21),
            at(2026, 10, 1, 0, 0, 1),
            at(2026, 10, 5, 3, 2, 1),
        ).forEach { moment ->
            val result = widgetCountdownFor(target, moment)
            val expected = java.time.temporal.ChronoUnit.DAYS.between(
                moment.toLocalDate(),
                target,
            )

            assertEquals("days at $moment", expected, result.days)
        }
    }

    @Test
    fun `the clock never asks for more than a day`() {
        // A whole year of moments: whatever the boundary rule picks, a Chronometer is never
        // handed thousands of hours to count.
        (0L until 365L).forEach { offset ->
            val moment = at(2026, 9, 25, 12, 0, 0).plusDays(offset)
            val result = widgetCountdownFor(target, moment)

            assertTrue("seconds at $moment", result.secondsToBoundary in 0..86_400)
        }
    }

    // -------------------------------------------------------------- breakdown

    @Test
    fun `a whole number of years drops the empty months and days`() {
        val units = breakdownOf(TimeParts(years = 1, months = 0, days = 0, hours = 0, minutes = 0, seconds = 0))

        assertEquals(listOf(UnitCount(CountdownUnit.YEAR, 1)), units)
    }

    @Test
    fun `an empty month between years and days is dropped`() {
        // The app's fixed-width line would show "1年00个月02天"; a widget has no
        // slots to keep aligned, so the zero is simply left out.
        val units = breakdownOf(TimeParts(years = 1, months = 0, days = 2, hours = 0, minutes = 0, seconds = 0))

        assertEquals(
            listOf(UnitCount(CountdownUnit.YEAR, 1), UnitCount(CountdownUnit.DAY, 2)),
            units,
        )
    }

    @Test
    fun `every unit that is set appears largest first`() {
        val units = breakdownOf(TimeParts(years = 2, months = 3, days = 4, hours = 5, minutes = 6, seconds = 7))

        assertEquals(
            listOf(
                UnitCount(CountdownUnit.YEAR, 2),
                UnitCount(CountdownUnit.MONTH, 3),
                UnitCount(CountdownUnit.DAY, 4),
            ),
            units,
        )
    }

    @Test
    fun `the clock components are never part of the breakdown`() {
        // The live clock covers hours and below; repeating them as text would be a
        // second, frozen copy of the same number.
        val units = breakdownOf(TimeParts(years = 0, months = 0, days = 0, hours = 5, minutes = 6, seconds = 7))

        assertTrue(units.isEmpty())
    }

    @Test
    fun `an all-zero countdown has nothing to spell out`() {
        assertTrue(breakdownOf(TimeParts.ZERO).isEmpty())
    }

    @Test
    fun `a date over a year out reads as years, months and days`() {
        val far = LocalDate.of(2027, 11, 5)
        val now = at(2026, 9, 25, 18, 0, 0)

        val units = breakdownOf(countdownTo(far, now).parts)

        assertEquals(
            listOf(
                UnitCount(CountdownUnit.YEAR, 1),
                UnitCount(CountdownUnit.MONTH, 1),
                UnitCount(CountdownUnit.DAY, 10),
            ),
            units,
        )
    }

    @Test
    fun `a target under a month away is days alone`() {
        val units = breakdownOf(countdownTo(target, at(2026, 9, 25, 18, 0, 0)).parts)

        assertEquals(listOf(UnitCount(CountdownUnit.DAY, 5)), units)
    }

    // ------------------------------------------------------------------- shape

    @Test
    fun `an unmeasured widget keeps the arrangement the initial layout draws`() {
        assertEquals(WidgetShape.STACK, widgetShapeFor(0, 0))
    }

    @Test
    fun `the default 4x2 has room for everything`() {
        // 250x110dp is what the metadata now asks for, and the size both launchers
        // have to agree on for the widget to look the same on each.
        assertEquals(WidgetShape.STACK, widgetShapeFor(250, 110))
    }

    @Test
    fun `a wide single row is the one-line arrangement`() {
        assertEquals(WidgetShape.ROW, widgetShapeFor(250, 40))
    }

    @Test
    fun `a 2x1 has room for the clock alone`() {
        assertEquals(WidgetShape.CLOCK, widgetShapeFor(110, 40))
    }

    @Test
    fun `a square 2x2 keeps the countdown above the clock`() {
        assertEquals(WidgetShape.MINI, widgetShapeFor(110, 110))
    }

    @Test
    fun `the width threshold is where the clock stops fitting beside anything`() {
        assertEquals(WidgetShape.MINI, widgetShapeFor(149, 200))
        assertEquals(WidgetShape.STACK, widgetShapeFor(150, 200))
    }

    @Test
    fun `the height threshold is where a second line starts to fit`() {
        assertEquals(WidgetShape.ROW, widgetShapeFor(250, 79))
        assertEquals(WidgetShape.STACK, widgetShapeFor(250, 80))
    }

    // -------------------------------------------------------------- the list widget

    @Test
    fun `the list shows what is coming, nearest first`() {
        val events = listOf(
            event(LocalDate.of(2026, 12, 1), "far"),
            event(LocalDate.of(2026, 9, 30), "near"),
            event(LocalDate.of(2026, 10, 1), "middle"),
        )

        val next = upcomingCountdowns(events, today = LocalDate.of(2026, 9, 25), count = 4)

        assertEquals(listOf("near", "middle", "far"), next.map { it.title })
    }

    @Test
    fun `the list leaves out what has already gone by`() {
        val events = listOf(
            event(LocalDate.of(2020, 1, 1), "long gone"),
            event(LocalDate.of(2026, 9, 25), "today"),
            event(LocalDate.of(2026, 10, 1), "ahead"),
        )

        val next = upcomingCountdowns(events, today = LocalDate.of(2026, 9, 25), count = 4)

        assertEquals(listOf("today", "ahead"), next.map { it.title })
    }

    @Test
    fun `the list stops at the rows there are`() {
        val events = (1..6).map { event(LocalDate.of(2026, 9, 25).plusDays(it.toLong()), "d$it") }

        val next = upcomingCountdowns(events, today = LocalDate.of(2026, 9, 25), count = 4)

        assertEquals(listOf("d1", "d2", "d3", "d4"), next.map { it.title })
    }

    @Test
    fun `a widget with nothing coming up has an empty list`() {
        val events = listOf(event(LocalDate.of(2020, 1, 1), "gone"))

        assertTrue(upcomingCountdowns(events, today = LocalDate.of(2026, 9, 25), count = 4).isEmpty())
    }

    @Test
    fun `an unmeasured widget draws every row it has`() {
        assertEquals(UPCOMING_WIDGET_ROWS, upcomingRowsFor(0))
        assertEquals(UPCOMING_WIDGET_ROWS, upcomingRowsFor(-1))
    }

    @Test
    fun `the default 4x2 has room for three rows`() {
        assertEquals(3, upcomingRowsFor(110))
    }

    @Test
    fun `a taller widget gains rows up to the ones the layout has`() {
        assertEquals(4, upcomingRowsFor(140))
        assertEquals(UPCOMING_WIDGET_ROWS, upcomingRowsFor(600))
    }

    @Test
    fun `a widget squeezed shorter than a row still shows one`() {
        assertEquals(1, upcomingRowsFor(40))
    }

    private fun event(date: LocalDate, title: String) = CountdownEvent(
        id = "$date-$title",
        date = date,
        title = title,
    )
}
