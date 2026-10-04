package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Countdowns that name a time of day rather than a whole day.
 *
 * The interesting part is the boundary: an all-day countdown targets midnight, so
 * "today" begins the moment the date arrives, while a timed one does not begin
 * until its own minute — and is still "today" after it has passed, because the
 * phase is about the calendar day, not the instant.
 */
class TimedCountdownTest {

    private val shanghai = ZoneId.of("Asia/Shanghai")
    private val date = LocalDate.of(2026, 10, 16)

    private fun event(time: LocalTime?) = CountdownEvent(
        id = "e",
        date = date,
        title = "standup",
        time = time,
    )

    private fun at(hour: Int, minute: Int, day: Int = 16) =
        LocalDate.of(2026, 10, day).atTime(hour, minute).atZone(shanghai)

    // -------------------------------------------------------------- the target

    @Test
    fun `an all-day countdown targets midnight`() {
        val target = event(null).target(shanghai)

        assertEquals(at(0, 0), target)
    }

    @Test
    fun `a timed countdown targets its own minute`() {
        val target = event(LocalTime.of(9, 30)).target(shanghai)

        assertEquals(at(9, 30), target)
    }

    @Test
    fun `the target keeps the zone it was asked for`() {
        val target = event(LocalTime.of(9, 30)).target(shanghai)

        assertEquals(shanghai, target.zone)
    }

    // --------------------------------------------------------------- the phase

    @Test
    fun `before a timed countdown today it is still counting down`() {
        val result = countdownTo(event(LocalTime.of(18, 0)).target(shanghai), at(9, 0))

        assertEquals(CountdownPhase.FUTURE, result.phase)
        assertEquals(9L, result.parts.hours)
        assertEquals(0L, result.parts.minutes)
    }

    @Test
    fun `the minute itself is the moment it arrives`() {
        val result = countdownTo(event(LocalTime.of(18, 0)).target(shanghai), at(18, 0))

        // Not after, so not future: it is the target's own minute.
        assertEquals(CountdownPhase.TODAY, result.phase)
    }

    @Test
    fun `after a timed countdown today it counts up instead`() {
        val result = countdownTo(event(LocalTime.of(9, 0)).target(shanghai), at(12, 30))

        // Still "today" — the phase follows the calendar day — but the numbers now
        // run forward from the moment that has passed.
        assertEquals(CountdownPhase.TODAY, result.phase)
        assertEquals(3L, result.parts.hours)
        assertEquals(30L, result.parts.minutes)
    }

    @Test
    fun `a timed countdown late today is not tomorrow`() {
        val result = countdownTo(event(LocalTime.of(20, 0)).target(shanghai), at(0, 30))

        assertEquals(CountdownPhase.FUTURE, result.phase)
        assertEquals(0L, result.parts.days)
    }

    @Test
    fun `the day after a timed countdown it is past`() {
        val result = countdownTo(event(LocalTime.of(9, 0)).target(shanghai), at(9, 0, day = 17))

        assertEquals(CountdownPhase.PAST, result.phase)
        assertEquals(1L, result.parts.days)
    }

    @Test
    fun `two countdowns on one day target different minutes`() {
        val morning = event(LocalTime.of(9, 0))
        val evening = event(LocalTime.of(18, 0))

        assertTrue(morning.target(shanghai).isBefore(evening.target(shanghai)))
        assertFalse(morning.target(shanghai).isEqual(evening.target(shanghai)))
    }
}
