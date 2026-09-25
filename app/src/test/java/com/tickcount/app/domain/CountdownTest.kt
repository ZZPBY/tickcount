package com.tickcount.app.domain

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

    @Test
    fun `counts whole calendar days to a future date`() {
        val countdown = countdownTo(LocalDate.of(2027, 2, 6), at(2026, 9, 25, 18, 0))

        assertEquals(CountdownPhase.FUTURE, countdown.phase)
        assertEquals(134L, countdown.days)
    }

    @Test
    fun `a date later today is today, not tomorrow`() {
        val countdown = countdownTo(LocalDate.of(2026, 9, 25), at(2026, 9, 25, 23, 59, 59))

        assertEquals(CountdownPhase.TODAY, countdown.phase)
        assertEquals(0L, countdown.days)
    }

    @Test
    fun `tomorrow is one day away even late at night`() {
        val countdown = countdownTo(LocalDate.of(2026, 9, 26), at(2026, 9, 25, 23, 30))

        assertEquals(CountdownPhase.FUTURE, countdown.phase)
        assertEquals(1L, countdown.days)
    }

    @Test
    fun `counts elapsed calendar days for a past date`() {
        val countdown = countdownTo(LocalDate.of(2026, 9, 13), at(2026, 9, 25, 18, 0))

        assertEquals(CountdownPhase.PAST, countdown.phase)
        assertEquals(12L, countdown.days)
    }

    @Test
    fun `the live clock measures the time left in the current day`() {
        val countdown = countdownTo(LocalDate.of(2027, 2, 6), at(2026, 9, 25, 18, 30, 15))

        // 5h 29m 45s until midnight.
        assertEquals((5 * 3600 + 29 * 60 + 45) * 1000L, countdown.dayProgressMillis)
        assertEquals("05:29:45", formatDayProgress(countdown.dayProgressMillis))
    }

    @Test
    fun `the live clock measures the time since midnight for a past date`() {
        val countdown = countdownTo(LocalDate.of(2026, 9, 13), at(2026, 9, 25, 18, 30, 15))

        assertEquals((18 * 3600 + 30 * 60 + 15) * 1000L, countdown.dayProgressMillis)
        assertEquals("18:30:15", formatDayProgress(countdown.dayProgressMillis))
    }

    @Test
    fun `handles a month boundary`() {
        assertEquals(1L, countdownTo(LocalDate.of(2026, 10, 1), at(2026, 9, 30, 12)).days)
    }

    @Test
    fun `handles a year boundary`() {
        assertEquals(1L, countdownTo(LocalDate.of(2027, 1, 1), at(2026, 12, 31, 12)).days)
        // 2027 is a common year, so its first day to the next first day is 365 days.
        assertEquals(365L, countdownTo(LocalDate.of(2028, 1, 1), at(2027, 1, 1, 0)).days)
        assertEquals(364L, countdownTo(LocalDate.of(2027, 12, 31), at(2027, 1, 1, 0)).days)
    }

    @Test
    fun `handles a leap day`() {
        assertEquals(1L, countdownTo(LocalDate.of(2028, 2, 29), at(2028, 2, 28, 12)).days)
        assertEquals(2L, countdownTo(LocalDate.of(2028, 3, 1), at(2028, 2, 28, 12)).days)
    }

    @Test
    fun `a short daylight-saving day is 23 hours long, not 24`() {
        // US DST starts on 2026-03-08: the local day is 23 hours.
        val countdown = countdownTo(LocalDate.of(2026, 3, 9), at(2026, 3, 8, 0, 0, 0, newYork))

        assertEquals(1L, countdown.days)
        assertEquals(23 * 3600 * 1000L, countdown.dayProgressMillis)
    }

    @Test
    fun `a long daylight-saving day reports its extra hour instead of wrapping`() {
        // US DST ends on 2026-11-01: the local day is 25 hours.
        val countdown = countdownTo(LocalDate.of(2026, 11, 2), at(2026, 11, 1, 0, 0, 0, newYork))

        assertEquals(1L, countdown.days)
        assertEquals(25 * 3600 * 1000L, countdown.dayProgressMillis)
        assertEquals("25:00:00", formatDayProgress(countdown.dayProgressMillis))
    }

    @Test
    fun `the day counter and the clock roll over at the same instant`() {
        val target = LocalDate.of(2027, 2, 6)

        val justBefore = countdownTo(target, at(2026, 9, 25, 23, 59, 59))
        val justAfter = countdownTo(target, at(2026, 9, 26, 0, 0, 0))

        // One second before midnight the clock is almost spent...
        assertEquals(1_000L, justBefore.dayProgressMillis)
        // ...and at midnight the day count drops by exactly one and the clock
        // restarts at a full day. The two numbers never contradict each other.
        assertEquals(justBefore.days - 1, justAfter.days)
        assertEquals(24 * 3600 * 1000L, justAfter.dayProgressMillis)
    }

    @Test
    fun `formatDayProgress never goes negative and pads every field`() {
        assertEquals("00:00:00", formatDayProgress(0))
        assertEquals("00:00:00", formatDayProgress(-5_000))
        assertEquals("00:00:01", formatDayProgress(1_400))
        assertEquals("01:02:03", formatDayProgress((3600 + 120 + 3) * 1000L))
    }

    @Test
    fun `days are identical regardless of the time of day they are measured at`() {
        val target = LocalDate.of(2027, 2, 6)
        val expected = countdownTo(target, at(2026, 9, 25, 0, 0, 1)).days

        listOf(6, 12, 18, 23).forEach { hour ->
            assertEquals(expected, countdownTo(target, at(2026, 9, 25, hour, 30)).days)
        }
    }
}
