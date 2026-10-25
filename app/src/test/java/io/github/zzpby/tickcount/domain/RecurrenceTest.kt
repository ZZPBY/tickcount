package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The day a countdown next lands on.
 *
 * A yearly countdown is the only one whose shown date moves, so this is where that move is
 * pinned down — including the 29th of February, the one date a year cannot always name.
 */
class RecurrenceTest {

    private fun event(date: String, repeats: Boolean) = CountdownEvent(
        id = "e",
        date = LocalDate.parse(date),
        title = "生日",
        repeatsYearly = repeats,
    )

    private fun day(text: String) = LocalDate.parse(text)

    @Test
    fun `a one-off countdown keeps the day it was entered on`() {
        val past = event("2020-03-01", repeats = false)
        assertEquals(day("2020-03-01"), past.occurrenceOn(day("2026-10-25")))
    }

    @Test
    fun `a yearly countdown lands on this year's day while that is still ahead`() {
        val birthday = event("1990-12-24", repeats = true)
        assertEquals(day("2026-12-24"), birthday.occurrenceOn(day("2026-10-25")))
    }

    @Test
    fun `a yearly countdown whose day has gone by lands next year`() {
        val birthday = event("1990-10-01", repeats = true)
        assertEquals(day("2027-10-01"), birthday.occurrenceOn(day("2026-10-25")))
    }

    @Test
    fun `the day itself is the next occurrence, so it reads today until midnight`() {
        val birthday = event("1990-10-25", repeats = true)
        assertEquals(day("2026-10-25"), birthday.occurrenceOn(day("2026-10-25")))
    }

    @Test
    fun `a 29 February countdown falls on the 28th in a year without one`() {
        val birthday = event("2000-02-29", repeats = true)
        assertEquals(day("2027-02-28"), birthday.occurrenceOn(day("2026-10-25")))
        // And keeps the 29th when the year has it.
        assertEquals(day("2028-02-29"), birthday.occurrenceOn(day("2028-01-01")))
    }

    @Test
    fun `resolving is a copy and leaves the stored date alone`() {
        val birthday = event("1990-01-05", repeats = true)

        assertEquals(day("2027-01-05"), birthday.resolvedOn(day("2026-10-25")).date)
        assertEquals(day("1990-01-05"), birthday.date)
    }
}
