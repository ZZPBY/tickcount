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

    // -------------------------------------------------------- significantUnits

    @Test
    fun `leading zero units are dropped`() {
        assertEquals(
            listOf(TimeUnit.MONTHS, TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS),
            parts(mo = 4, d = 11, h = 6).significantUnits(),
        )
    }

    @Test
    fun `a year-long countdown shows every unit`() {
        assertEquals(TimeUnit.entries.toList(), parts(y = 1, mo = 2, d = 3, h = 4, mi = 5, s = 6).significantUnits())
    }

    @Test
    fun `seconds are always shown so the display keeps ticking`() {
        assertEquals(listOf(TimeUnit.SECONDS), parts().significantUnits())
        assertEquals(listOf(TimeUnit.SECONDS), parts(s = 7).significantUnits())
    }

    @Test
    fun `an hour-long countdown starts at hours`() {
        assertEquals(
            listOf(TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS),
            parts(h = 1).significantUnits(),
        )
    }

    @Test
    fun `isZero only for an all-zero breakdown`() {
        assertEquals(true, parts().isZero)
        assertEquals(false, parts(s = 1).isZero)
    }
}
