package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The iCalendar export: one file, read by calendars this app knows nothing about. */
class IcsTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, zone)

    private fun event(
        id: String = "a",
        date: LocalDate = LocalDate.of(2026, 10, 26),
        title: String = "旅行",
        time: LocalTime? = null,
        tags: List<String> = emptyList(),
    ) = CountdownEvent(id = id, date = date, title = title, time = time, tags = tags)

    private fun lines(text: String) = text.split("\r\n").filter { it.isNotEmpty() }

    @Test
    fun `an all-day countdown becomes an all-day event ending the next day`() {
        val out = icsFor(listOf(event()), zone, now)
        assertTrue(out.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(out.endsWith("END:VCALENDAR\r\n"))
        assertTrue("DTSTART;VALUE=DATE:20261026" in out)
        // DTEND is exclusive in iCalendar, so it is the day after.
        assertTrue("DTEND;VALUE=DATE:20261027" in out)
        assertTrue("SUMMARY:旅行" in out)
    }

    @Test
    fun `a countdown with a time becomes a one-hour event in UTC`() {
        val out = icsFor(
            listOf(event(time = LocalTime.of(9, 30))),
            zone,
            now,
        )
        // 09:30 in Shanghai is 01:30 UTC.
        assertTrue("DTSTART:20261026T013000Z" in out)
        assertTrue("DTEND:20261026T023000Z" in out)
    }

    @Test
    fun `text fields escape the characters the format reserves`() {
        val out = icsFor(
            listOf(event(title = "a,b;c\\d")),
            zone,
            now,
        )
        assertTrue("SUMMARY:a\\,b\\;c\\\\d" in out)
    }

    @Test
    fun `tags are written as categories`() {
        val out = icsFor(listOf(event(tags = listOf("生活", "计划"))), zone, now)
        assertTrue("CATEGORIES:生活\\,计划" in out)
    }

    @Test
    fun `every line fits in 75 octets, Chinese included`() {
        val title = "很".repeat(60)
        val out = icsFor(listOf(event(title = title)), zone, now)
        lines(out).forEach { line ->
            val octets = line.toByteArray(Charsets.UTF_8).size
            assertTrue("line of $octets octets: $line", octets <= 75)
        }
        // Folding must not lose anything: the title comes back when the lines are rejoined.
        val unfolded = out.replace("\r\n ", "")
        assertTrue(unfolded.contains("SUMMARY:$title"))
    }

    @Test
    fun `the file carries a stamp in UTC and one UID per event`() {
        val out = icsFor(listOf(event(id = "x"), event(id = "y")), zone, now)
        assertEquals(2, Regex("DTSTAMP:20261017T040000Z").findAll(out).count())
        assertTrue("UID:x@tickcount" in out)
        assertTrue("UID:y@tickcount" in out)
        assertEquals(2, Regex("BEGIN:VEVENT").findAll(out).count())
    }
}
