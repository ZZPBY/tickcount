package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The list ordering and the past/future boundary. The boundary is the part worth
 * pinning down: an off-by-one there would dim the countdown for *today*, which is
 * exactly the one the user is most likely looking at.
 */
class CountdownListTest {

    private val today = LocalDate.of(2026, 9, 25)

    private fun event(
        date: LocalDate,
        title: String,
        colorHue: Float? = null,
        time: LocalTime? = null,
        addedAt: Long = 0L,
    ) = CountdownEvent(
        id = "$date-$title",
        date = date,
        title = title,
        time = time,
        colorHue = colorHue,
        createdAt = addedAt,
    )

    // ------------------------------------------------------------------ order

    @Test
    fun `an empty store produces an empty list`() {
        assertEquals(emptyList<CountdownListEntry>(), countdownList(emptyList(), today))
    }

    @Test
    fun `entries come out nearest first, with anything already past below`() {
        val events = listOf(
            event(LocalDate.of(2026, 12, 31), "new year"),
            event(LocalDate.of(2026, 10, 1), "birthday"),
            event(LocalDate.of(2026, 9, 15), "meeting"),
        )

        val titles = countdownList(events, today).map { it.event.title }

        // The meeting is ten days behind the fixture's today, so it sorts below the
        // two still ahead of it rather than leading the list as it used to.
        assertEquals(listOf("birthday", "new year", "meeting"), titles)
    }

    @Test
    fun `a single countdown is returned unchanged`() {
        val only = event(LocalDate.of(2026, 10, 1), "birthday", colorHue = 210f)

        val entries = countdownList(listOf(only), today)

        assertEquals(1, entries.size)
        assertEquals(only, entries.single().event)
    }

    @Test
    fun `a countdown's own colour survives the round trip`() {
        val events = listOf(
            event(LocalDate.of(2026, 10, 1), "a", colorHue = 12f),
            event(LocalDate.of(2026, 10, 2), "b", colorHue = 300f),
        )

        assertEquals(listOf(12f, 300f), countdownList(events, today).map { it.event.colorHue })
    }

    @Test
    fun `same-day countdowns come out in time order with the all-day one first`() {
        val date = LocalDate.of(2026, 10, 1)
        val events = listOf(
            event(date, "evening", time = LocalTime.of(18, 30)),
            event(date, "all day"),
            event(date, "morning", time = LocalTime.of(9, 0)),
        )

        val titles = countdownList(events, today).map { it.event.title }

        // The all-day one targets midnight, which is the earliest moment that day
        // can mean, so it leads.
        assertEquals(listOf("all day", "morning", "evening"), titles)
    }

    // ------------------------------------------------------------ past marking

    @Test
    fun `today's own countdown is not past`() {
        val entries = countdownList(listOf(event(today, "today")), today)

        assertFalse(entries.single().isPast)
    }

    @Test
    fun `yesterday is past and tomorrow is not`() {
        val events = listOf(
            event(today.minusDays(1), "yesterday"),
            event(today.plusDays(1), "tomorrow"),
        )

        val byTitle = countdownList(events, today).associateBy { it.event.title }

        assertTrue(byTitle.getValue("yesterday").isPast)
        assertFalse(byTitle.getValue("tomorrow").isPast)
    }

    @Test
    fun `one day either side of the boundary flips the flag`() {
        val events = listOf(
            event(today.minusDays(1), "past"),
            event(today, "now"),
            event(today.plusDays(1), "future"),
        )

        val flags = countdownList(events, today).associate { it.event.title to it.isPast }

        assertEquals(mapOf("past" to true, "now" to false, "future" to false), flags)
    }

    @Test
    fun `past entries stay in the list, below the ones still ahead`() {
        val events = listOf(
            event(LocalDate.of(2020, 1, 1), "long gone"),
            event(LocalDate.of(2026, 10, 1), "upcoming"),
        )

        val entries = countdownList(events, today)

        assertEquals(2, entries.size)
        assertEquals(listOf("upcoming", "long gone"), entries.map { it.event.title })
        assertFalse(entries.first().isPast)
        assertTrue(entries.last().isPast)
    }

    @Test
    fun `an all-past list runs most recent first and is fully flagged`() {
        val events = listOf(
            event(LocalDate.of(2026, 9, 20), "b"),
            event(LocalDate.of(2026, 9, 10), "a"),
        )

        val entries = countdownList(events, today)

        assertEquals(listOf("b", "a"), entries.map { it.event.title })
        assertTrue(entries.all { it.isPast })
    }

    @Test
    fun `the heading is flagged on the first past row and nowhere else`() {
        val events = listOf(
            event(LocalDate.of(2026, 10, 1), "ahead"),
            event(LocalDate.of(2026, 9, 10), "older"),
            event(LocalDate.of(2026, 9, 20), "newer"),
        )

        val entries = countdownList(events, today)

        assertEquals(listOf("ahead", "newer", "older"), entries.map { it.event.title })
        assertEquals(listOf(false, true, false), entries.map { it.opensPastSection })
    }

    @Test
    fun `a list with nothing past carries no heading flag`() {
        val events = listOf(
            event(LocalDate.of(2026, 10, 1), "ahead"),
            event(LocalDate.of(2026, 12, 1), "further ahead"),
        )

        assertTrue(countdownList(events, today).none { it.opensPastSection })
    }

    @Test
    fun `a pinned past countdown leads the list without carrying the heading`() {
        val events = listOf(
            event(LocalDate.of(2020, 1, 1), "memorial").copy(pinned = true),
            event(LocalDate.of(2026, 10, 1), "ahead"),
            event(LocalDate.of(2026, 9, 10), "gone"),
        )

        val entries = countdownList(events, today)

        assertEquals(listOf("memorial", "ahead", "gone"), entries.map { it.event.title })
        // The heading belongs to the section, not to the pinned row sitting above it.
        assertEquals(listOf(false, false, true), entries.map { it.opensPastSection })
    }

    @Test
    fun `the past section runs most recent first in every order`() {
        val events = listOf(
            event(LocalDate.of(2026, 9, 1), "oldest"),
            event(LocalDate.of(2026, 9, 20), "newest"),
            event(LocalDate.of(2026, 10, 1), "ahead"),
            event(LocalDate.of(2026, 12, 1), "further ahead"),
        )

        val ascending = countdownList(events, today, SortOrder.DATE_ASC).map { it.event.title }
        val descending = countdownList(events, today, SortOrder.DATE_DESC).map { it.event.title }

        // The order governs the part still ahead; the past part is a record either way.
        assertEquals(listOf("ahead", "further ahead", "newest", "oldest"), ascending)
        assertEquals(listOf("further ahead", "ahead", "newest", "oldest"), descending)
    }

    // ------------------------------------------------------------- sort orders

    @Test
    fun `the default order is soonest first`() {
        val events = listOf(
            event(LocalDate.of(2026, 12, 31), "far"),
            event(LocalDate.of(2026, 10, 1), "near"),
        )

        assertEquals(listOf("near", "far"), countdownList(events, today).map { it.event.title })
    }

    @Test
    fun `descending puts the furthest away first`() {
        val events = listOf(
            event(LocalDate.of(2026, 10, 1), "near"),
            event(LocalDate.of(2026, 12, 31), "far"),
        )

        val titles = countdownList(events, today, SortOrder.DATE_DESC).map { it.event.title }

        assertEquals(listOf("far", "near"), titles)
    }

    @Test
    fun `descending also reverses the times within one day`() {
        val date = LocalDate.of(2026, 10, 1)
        val events = listOf(
            event(date, "morning", time = LocalTime.of(9, 0)),
            event(date, "evening", time = LocalTime.of(18, 0)),
        )

        val titles = countdownList(events, today, SortOrder.DATE_DESC).map { it.event.title }

        assertEquals(listOf("evening", "morning"), titles)
    }

    @Test
    fun `added order puts the newest addition first among the ones still ahead`() {
        val events = listOf(
            event(LocalDate.of(2026, 10, 1), "added first", addedAt = 100L),
            event(LocalDate.of(2030, 1, 1), "added second", addedAt = 200L),
            event(LocalDate.of(2020, 1, 1), "added last", addedAt = 300L),
        )

        val titles = countdownList(events, today, SortOrder.ADDED).map { it.event.title }

        // "Added last" is the newest addition and would lead on that count alone, but its
        // day is behind us, so it sits under the heading with the rest of the past.
        assertEquals(listOf("added second", "added first", "added last"), titles)
    }

    @Test
    fun `countdowns saved before the stamp existed fall back to the date`() {
        // Everything written by an older build reads back with a zero stamp. The
        // fallback has to be the date rather than the order the store happened to
        // hand them over in, so soonest still leads.
        val events = listOf(
            event(LocalDate.of(2026, 12, 31), "far"),
            event(LocalDate.of(2026, 10, 1), "near"),
        )

        val titles = countdownList(events, today, SortOrder.ADDED).map { it.event.title }

        assertEquals(listOf("near", "far"), titles)
    }

    @Test
    fun `a stamped countdown sorts above an unstamped one`() {
        val events = listOf(
            event(LocalDate.of(2026, 10, 1), "from the old build"),
            event(LocalDate.of(2030, 1, 1), "just added", addedAt = 1L),
        )

        val titles = countdownList(events, today, SortOrder.ADDED).map { it.event.title }

        assertEquals(listOf("just added", "from the old build"), titles)
    }

    // ----------------------------------------------------------- day distance

    @Test
    fun `days from today counts calendar days in both directions`() {
        assertEquals(0L, daysFromToday(today, today))
        assertEquals(1L, daysFromToday(today.plusDays(1), today))
        assertEquals(12L, daysFromToday(today.plusDays(12), today))
        assertEquals(-1L, daysFromToday(today.minusDays(1), today))
        assertEquals(-40L, daysFromToday(today.minusDays(40), today))
    }

    @Test
    fun `a countdown later today is still zero days away`() {
        // The number the list shows is a count of days, not of hours, so a times
        // countdown this evening has to read as "today" all morning.
        val thisEvening = event(today, "dinner", time = LocalTime.of(20, 0))

        assertEquals(0L, daysFromToday(thisEvening.date, today))
    }
}
