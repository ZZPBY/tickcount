package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Spacing a batch of countdowns out from the date the editor was opened on.
 *
 * The arithmetic is one `plusDays` per entry and looks after itself; what these pin down is
 * the three decisions around it — that the date it was opened on is not repeated here, that
 * the interval is the gap between neighbours rather than a multiple of the index, and that
 * the batch inherits everything else from the one countdown that was filled in.
 */
class BatchAddTest {

    private val base = LocalDate.of(2026, 10, 14)

    private fun template(
        date: LocalDate = base,
        title: String = "打副本",
        time: LocalTime? = null,
        tags: List<String> = emptyList(),
        hue: Float? = null,
        pinned: Boolean = false,
    ) = CountdownEvent(
        id = "base",
        date = date,
        title = title,
        time = time,
        tags = tags,
        colorHue = hue,
        createdAt = 1_000L,
        pinned = pinned,
    )

    // ------------------------------------------------------------------ the example

    @Test
    fun `a step of two and a count of three lands on the second, fourth and sixth day`() {
        // The case from the request: the 14th is created by the screen itself, and these
        // are the three that join it.
        val added = batchOf(template(), stepDays = 2, count = 3, createdAt = 5_000L)

        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalDate.of(2026, 10, 20),
            ),
            added.map { it.date },
        )
    }

    @Test
    fun `the date the editor was opened on is not in the batch`() {
        val added = batchOf(template(), stepDays = 2, count = 3, createdAt = 5_000L)

        assertTrue("the base date was duplicated", added.none { it.date == base })
    }

    @Test
    fun `the interval is the gap between neighbours, not the distance from the start`() {
        val added = batchOf(template(), stepDays = 7, count = 4, createdAt = 5_000L)
        val gaps = added.map { it.date }.zipWithNext { earlier, later ->
            java.time.temporal.ChronoUnit.DAYS.between(earlier, later)
        }

        assertEquals(listOf(7L, 7L, 7L), gaps)
    }

    // ------------------------------------------------------------------- the copies

    @Test
    fun `everything but the date and the id travels with the batch`() {
        val source = template(
            title = "例会",
            time = LocalTime.of(9, 30),
            tags = listOf("工作", "固定"),
            hue = 210f,
            pinned = true,
        )

        val added = batchOf(source, stepDays = 1, count = 2, createdAt = 5_000L)

        added.forEach { copy ->
            assertEquals("例会", copy.title)
            assertEquals(LocalTime.of(9, 30), copy.time)
            assertEquals(listOf("工作", "固定"), copy.tags)
            assertEquals(210f, copy.colorHue)
            assertTrue("the pin was dropped", copy.pinned)
        }
    }

    @Test
    fun `every entry carries an id of its own`() {
        // A repeated id would make the copies the same countdown to the store, to the list's
        // keys and to anything a widget is bound to: one row would survive.
        val added = batchOf(template(), stepDays = 1, count = 5, createdAt = 5_000L)
        val ids = added.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        assertTrue("an entry kept the template's id", ids.none { it == "base" })
    }

    @Test
    fun `the whole batch is stamped with the moment it was added`() {
        val added = batchOf(template(), stepDays = 3, count = 3, createdAt = 5_000L)

        assertTrue(added.all { it.createdAt == 5_000L })
    }

    @Test
    fun `an all-day countdown stays all day`() {
        val added = batchOf(template(time = null), stepDays = 1, count = 2, createdAt = 5_000L)

        assertTrue(added.all { it.time == null })
    }

    // ------------------------------------------------------------------ the limits

    @Test
    fun `a count of zero adds nothing`() {
        // Not one entry rather than none: the count is what the batch adds, so zero is the
        // way to ask for only the countdown already being filled in.
        assertEquals(emptyList<CountdownEvent>(), batchOf(template(), 2, 0, 5_000L))
    }

    @Test
    fun `a step of zero or less adds nothing`() {
        // Every entry would otherwise land on the base date, which reads as the app ignoring
        // the interval rather than as the interval being refused.
        assertEquals(emptyList<CountdownEvent>(), batchOf(template(), 0, 3, 5_000L))
        assertEquals(emptyList<CountdownEvent>(), batchOf(template(), -2, 3, 5_000L))
        assertEquals(emptyList<CountdownEvent>(), batchOf(template(), 2, -1, 5_000L))
    }

    @Test
    fun `the batch holds exactly the count asked for`() {
        assertEquals(1, batchOf(template(), 1, 1, 5_000L).size)
        assertEquals(MAX_BATCH_COUNT, batchOf(template(), 1, MAX_BATCH_COUNT, 5_000L).size)
    }

    // -------------------------------------------------------------------- the edges

    @Test
    fun `a batch steps across a month and a year without drifting`() {
        val source = template(date = LocalDate.of(2026, 12, 30))

        val added = batchOf(source, stepDays = 3, count = 3, createdAt = 5_000L)

        assertEquals(
            listOf(
                LocalDate.of(2027, 1, 2),
                LocalDate.of(2027, 1, 5),
                LocalDate.of(2027, 1, 8),
            ),
            added.map { it.date },
        )
    }

    @Test
    fun `a batch steps over a leap day`() {
        val source = template(date = LocalDate.of(2028, 2, 27))

        val added = batchOf(source, stepDays = 1, count = 3, createdAt = 5_000L)

        assertEquals(
            listOf(
                LocalDate.of(2028, 2, 28),
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2028, 3, 1),
            ),
            added.map { it.date },
        )
    }

    @Test
    fun `a step is counted as days on the calendar, leap days included`() {
        // The far end of the interval field, where a batch is a single entry. `plusDays` walks
        // the timeline rather than the page, so ten years of days is 3650 days and not the
        // same day of the month ten years on: the leap days of 2028, 2032 and 2036 make it
        // land three days earlier. An interval of days is what the screen offers and what
        // this adds, so the arithmetic is right and the expectation has to be the real one.
        val added = batchOf(template(), stepDays = MAX_BATCH_STEP_DAYS, count = 1, createdAt = 0L)

        assertEquals(LocalDate.of(2036, 10, 11), added.single().date)
    }
}
