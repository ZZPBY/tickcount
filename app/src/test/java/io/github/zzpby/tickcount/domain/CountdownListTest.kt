package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The list ordering and the past/future boundary. The boundary is the part worth
 * pinning down: an off-by-one there would dim the countdown for *today*, which
 * is exactly the one the user is most likely looking at.
 */
class CountdownListTest {

    private val today = LocalDate.of(2026, 9, 25)

    private fun event(date: LocalDate, title: String, colorIndex: Int = 0) =
        CountdownEvent(date = date, title = title, colorIndex = colorIndex)

    private fun mapOfEvents(vararg events: CountdownEvent) =
        events.associateBy { it.date }

    // ------------------------------------------------------------------ order

    @Test
    fun `an empty store produces an empty list`() {
        assertEquals(emptyList<CountdownListEntry>(), countdownList(emptyMap(), today))
    }

    @Test
    fun `entries come out oldest first`() {
        val events = mapOfEvents(
            event(LocalDate.of(2026, 12, 31), "new year"),
            event(LocalDate.of(2026, 10, 1), "birthday"),
            event(LocalDate.of(2026, 9, 15), "meeting"),
        )

        val titles = countdownList(events, today).map { it.event.title }

        assertEquals(listOf("meeting", "birthday", "new year"), titles)
    }

    @Test
    fun `a single countdown is returned unchanged`() {
        val only = event(LocalDate.of(2026, 10, 1), "birthday", colorIndex = 3)

        val entries = countdownList(mapOfEvents(only), today)

        assertEquals(1, entries.size)
        assertEquals(only, entries.single().event)
    }

    @Test
    fun `the colour index survives the round trip`() {
        val events = mapOfEvents(
            event(LocalDate.of(2026, 10, 1), "a", colorIndex = 1),
            event(LocalDate.of(2026, 10, 2), "b", colorIndex = 5),
        )

        assertEquals(listOf(1, 5), countdownList(events, today).map { it.event.colorIndex })
    }

    // ------------------------------------------------------------ past marking

    @Test
    fun `today's own countdown is not past`() {
        val entries = countdownList(mapOfEvents(event(today, "today")), today)

        assertFalse(entries.single().isPast)
    }

    @Test
    fun `yesterday is past and tomorrow is not`() {
        val events = mapOfEvents(
            event(today.minusDays(1), "yesterday"),
            event(today.plusDays(1), "tomorrow"),
        )

        val byTitle = countdownList(events, today).associateBy { it.event.title }

        assertTrue(byTitle.getValue("yesterday").isPast)
        assertFalse(byTitle.getValue("tomorrow").isPast)
    }

    @Test
    fun `one day either side of the boundary flips the flag`() {
        val events = mapOfEvents(
            event(today.minusDays(1), "past"),
            event(today, "now"),
            event(today.plusDays(1), "future"),
        )

        val flags = countdownList(events, today).map { it.isPast }

        assertEquals(listOf(true, false, false), flags)
    }

    @Test
    fun `past entries stay in the list instead of being dropped`() {
        val events = mapOfEvents(
            event(LocalDate.of(2020, 1, 1), "long gone"),
            event(LocalDate.of(2026, 10, 1), "upcoming"),
        )

        val entries = countdownList(events, today)

        assertEquals(2, entries.size)
        assertTrue(entries.first().isPast)
        assertFalse(entries.last().isPast)
    }

    @Test
    fun `an all-past list is ordered oldest first and fully flagged`() {
        val events = mapOfEvents(
            event(LocalDate.of(2026, 9, 20), "b"),
            event(LocalDate.of(2026, 9, 10), "a"),
        )

        val entries = countdownList(events, today)

        assertEquals(listOf("a", "b"), entries.map { it.event.title })
        assertTrue(entries.all { it.isPast })
    }
}
