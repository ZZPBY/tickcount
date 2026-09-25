package io.github.zzpby.tickcount.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The countdown arithmetic is pure and zone-aware, which makes it the one part of
 * this app worth pinning down with tests: month lengths, leap years and
 * daylight-saving transitions are exactly where an off-by-one shows up.
 */
class CountdownTest {

    private val shanghai = ZoneId.of("Asia/Shanghai")
    private val newYork = ZoneId.of("America/New_York")

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        zone: ZoneId = shanghai,
    ): ZonedDateTime = ZonedDateTime.of(year, month, day, hour, minute, second, 0, zone)

    private fun parts(y: Long = 0, mo: Long = 0, d: Long = 0, h: Long = 0, mi: Long = 0, s: Long = 0) =
        TimeParts(y, mo, d, h, mi, s)

    // ---------------------------------------------------------------- decompose

    @Test
    fun `splits a short interval into hours, minutes and seconds`() {
        val result = decompose(at(2026, 9, 25, 18, 0, 0), at(2026, 9, 25, 20, 30, 15))

        assertEquals(parts(h = 2, mi = 30, s = 15), result)
    }

    @Test
    fun `splits months and days out of a medium interval`() {
        // 2026-09-25 18:00 -> 2027-02-06 00:00 is 4 months, 11 days and 6 hours.
        val result = decompose(at(2026, 9, 25, 18, 0, 0), at(2027, 2, 6, 0, 0, 0))

        assertEquals(parts(mo = 4, d = 11, h = 6), result)
    }

    @Test
    fun `splits whole years out of a long interval`() {
        val result = decompose(at(2026, 9, 25, 0, 0, 0), at(2028, 11, 30, 0, 0, 0))

        assertEquals(parts(y = 2, mo = 2, d = 5), result)
    }

    @Test
    fun `an exact interval has no remainder`() {
        assertEquals(
            parts(y = 1, mo = 1, d = 1, h = 1, mi = 1, s = 1),
            decompose(at(2026, 1, 1, 1, 1, 1), at(2027, 2, 2, 2, 2, 2)),
        )
    }

    @Test
    fun `components always add back up to the interval`() {
        val from = at(2026, 9, 25, 17, 43, 21)
        val to = at(2029, 3, 1, 4, 15, 59)

        val result = decompose(from, to)

        var rebuilt = from
        rebuilt = rebuilt.plusYears(result.years)
        rebuilt = rebuilt.plusMonths(result.months)
        rebuilt = rebuilt.plusDays(result.days)
        rebuilt = rebuilt.plusHours(result.hours)
        rebuilt = rebuilt.plusMinutes(result.minutes)
        rebuilt = rebuilt.plusSeconds(result.seconds)
        assertEquals(to.toInstant(), rebuilt.toInstant())
    }

    @Test
    fun `handles a leap day`() {
        assertEquals(parts(d = 1), decompose(at(2028, 2, 28), at(2028, 2, 29)))
        assertEquals(parts(d = 2), decompose(at(2028, 2, 28), at(2028, 3, 1)))
        // 2027 is a common year, so it has no 29 February to land on.
        assertEquals(parts(d = 1), decompose(at(2027, 2, 28), at(2027, 3, 1)))
    }

    @Test
    fun `handles month and year boundaries`() {
        assertEquals(parts(d = 1), decompose(at(2026, 9, 30, 12), at(2026, 10, 1, 12)))
        assertEquals(parts(d = 1), decompose(at(2026, 12, 31, 12), at(2027, 1, 1, 12)))
        assertEquals(parts(y = 1), decompose(at(2026, 12, 31), at(2027, 12, 31)))
    }

    @Test
    fun `a short daylight-saving day is still one calendar day`() {
        // US DST starts on 2026-03-08: only 23 hours of real time elapse.
        assertEquals(parts(d = 1), decompose(at(2026, 3, 8, 0, 0, 0, newYork), at(2026, 3, 9, 0, 0, 0, newYork)))
    }

    @Test
    fun `a long daylight-saving day is still one calendar day`() {
        // US DST ends on 2026-11-01: 25 hours of real time elapse.
        assertEquals(parts(d = 1), decompose(at(2026, 11, 1, 0, 0, 0, newYork), at(2026, 11, 2, 0, 0, 0, newYork)))
    }

    @Test
    fun `a zero-length interval is all zeros`() {
        val moment = at(2026, 9, 25, 18, 0, 0)
        assertEquals(parts(), decompose(moment, moment))
    }

    // ------------------------------------------------------------- countdownTo

    @Test
    fun `a future date counts down to its midnight`() {
        val countdown = countdownTo(LocalDate.of(2027, 2, 6), at(2026, 9, 25, 18, 0, 0))

        assertEquals(CountdownPhase.FUTURE, countdown.phase)
        assertEquals(parts(mo = 4, d = 11, h = 6), countdown.parts)
    }

    @Test
    fun `tomorrow evening is hours away, not a whole day`() {
        // The countdown targets midnight, so at 18:00 the honest answer is 6 hours.
        val countdown = countdownTo(LocalDate.of(2026, 9, 26), at(2026, 9, 25, 18, 0, 0))

        assertEquals(CountdownPhase.FUTURE, countdown.phase)
        assertEquals(parts(h = 6), countdown.parts)
    }

    @Test
    fun `the selected date being today counts up from its midnight`() {
        val countdown = countdownTo(LocalDate.of(2026, 9, 25), at(2026, 9, 25, 18, 30, 15))

        assertEquals(CountdownPhase.TODAY, countdown.phase)
        assertEquals(parts(h = 18, mi = 30, s = 15), countdown.parts)
    }

    @Test
    fun `a past date counts up from its midnight`() {
        val countdown = countdownTo(LocalDate.of(2026, 9, 13), at(2026, 9, 25, 18, 0, 0))

        assertEquals(CountdownPhase.PAST, countdown.phase)
        assertEquals(parts(d = 12, h = 18), countdown.parts)
    }

    @Test
    fun `a date one second away is a future countdown`() {
        val countdown = countdownTo(LocalDate.of(2026, 9, 26), at(2026, 9, 25, 23, 59, 59))

        assertEquals(CountdownPhase.FUTURE, countdown.phase)
        assertEquals(parts(s = 1), countdown.parts)
    }

    @Test
    fun `the phase flips at exactly midnight`() {
        val target = LocalDate.of(2026, 9, 26)

        assertEquals(CountdownPhase.FUTURE, countdownTo(target, at(2026, 9, 25, 23, 59, 59)).phase)
        assertEquals(CountdownPhase.TODAY, countdownTo(target, at(2026, 9, 26, 0, 0, 0)).phase)
        assertEquals(CountdownPhase.PAST, countdownTo(target, at(2026, 9, 27, 0, 0, 0)).phase)
    }

    // ------------------------------------------------------------ formatSlots

    @Test
    fun `every slot of an unset countdown is a placeholder`() {
        assertEquals(
            listOf("----", "--", "--", "--", "--", "--"),
            formatSlots(TimeParts.ZERO),
        )
    }

    @Test
    fun `only leading units are blanked`() {
        // Four months in: the year is still leading, everything below it counts.
        assertEquals(
            listOf("----", "04", "11", "06", "30", "15"),
            formatSlots(TimeParts(years = 0, months = 4, days = 11, hours = 6, minutes = 30, seconds = 15)),
        )
    }

    @Test
    fun `a zero between counting units is a real zero, not a dash`() {
        // The whole point of "leading only": two days and thirty seconds must
        // not blank out the hours and minutes it passes through on the way.
        assertEquals(
            listOf("----", "--", "02", "00", "00", "30"),
            formatSlots(TimeParts(years = 0, months = 0, days = 2, hours = 0, minutes = 0, seconds = 30)),
        )
    }

    @Test
    fun `the last hour of a countdown keeps its hours`() {
        assertEquals(
            listOf("----", "--", "05", "00", "00", "01"),
            formatSlots(TimeParts(years = 0, months = 0, days = 5, hours = 0, minutes = 0, seconds = 1)),
        )
    }

    @Test
    fun `seconds are shown as a number even when nothing larger is counting`() {
        assertEquals(
            listOf("----", "--", "--", "--", "--", "07"),
            formatSlots(TimeParts(0, 0, 0, 0, 0, 7)),
        )
    }

    @Test
    fun `a multi-year countdown pads the year to four digits`() {
        val slots = formatSlots(TimeParts(years = 3, months = 4, days = 11, hours = 6, minutes = 30, seconds = 15))

        assertEquals(listOf("0003", "04", "11", "06", "30", "15"), slots)
        assertEquals(listOf(4, 2, 2, 2, 2, 2), slots.map { it.length })
    }

    @Test
    fun `a value wider than its slot is passed through rather than truncated`() {
        // A five-digit year would rather overflow the line than silently show
        // the wrong number.
        assertEquals(
            listOf("12345", "00", "00", "00", "00", "00"),
            formatSlots(TimeParts(years = 12345, months = 0, days = 0, hours = 0, minutes = 0, seconds = 0)),
        )
    }
}
