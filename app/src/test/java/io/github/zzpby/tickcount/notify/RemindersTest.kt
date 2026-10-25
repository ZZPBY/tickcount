package io.github.zzpby.tickcount.notify

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * When a reminder falls.
 *
 * The arithmetic is the whole feature — an alarm set at the wrong instant fails silently —
 * so it is tested away from the alarm manager.
 */
class RemindersTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun allDay(
        id: String = "a",
        date: LocalDate = LocalDate.of(2026, 10, 26),
        lead: Int? = null,
    ) = CountdownEvent(id = id, date = date, title = "旅行", remindLeadMinutes = lead)

    private fun timed(
        id: String = "b",
        date: LocalDate = LocalDate.of(2026, 10, 26),
        time: LocalTime = LocalTime.of(14, 30),
        lead: Int? = null,
    ) = CountdownEvent(id = id, date = date, title = "开会", time = time, remindLeadMinutes = lead)

    private fun local(instant: Instant) = instant.atZone(zone).toLocalDateTime()

    @Test
    fun `an all-day countdown is reminded at the app's time of day, less the lead`() {
        val moment = Reminders.momentOf(allDay(), minutes = 9 * 60, leadMinutes = 120, zone = zone)
        assertEquals(LocalDate.of(2026, 10, 26).atTime(7, 0), local(moment))
    }

    @Test
    fun `a countdown with its own time is reminded then, whatever the app-wide lead says`() {
        val moment = Reminders.momentOf(timed(), minutes = 9 * 60, leadMinutes = 30, zone = zone)
        assertEquals(LocalDate.of(2026, 10, 26).atTime(14, 30), local(moment))
    }

    @Test
    fun `a countdown's own lead wins over the app-wide one`() {
        val moment = Reminders.momentOf(
            timed(lead = 3 * 24 * 60),
            minutes = 9 * 60,
            leadMinutes = 30,
            zone = zone,
        )
        assertEquals(LocalDate.of(2026, 10, 23).atTime(14, 30), local(moment))
    }

    @Test
    fun `an all-day countdown's own lead moves it back from the app's time of day`() {
        // Its date has no time of day to be moved back from, so the app-wide time stands in
        // for one; the countdown's own lead is then applied to that.
        val moment = Reminders.momentOf(
            allDay(lead = 24 * 60),
            minutes = 9 * 60,
            leadMinutes = 30,
            zone = zone,
        )
        assertEquals(LocalDate.of(2026, 10, 25).atTime(9, 0), local(moment))
    }

    @Test
    fun `the next reminder is the first that has not already passed`() {
        val past = allDay(id = "past", date = LocalDate.of(2026, 10, 17))
        val soon = allDay(id = "soon", date = LocalDate.of(2026, 10, 26))
        val now = LocalDate.of(2026, 10, 18).atTime(12, 0).atZone(zone).toInstant()

        val planned = Reminders.planned(listOf(soon, past), 9 * 60, 0, now, zone)

        assertEquals("soon", planned?.event?.id)
        assertEquals(LocalDate.of(2026, 10, 26).atTime(9, 0), local(planned!!.at))    }

    @Test
    fun `a list whose reminders have all passed has nothing to schedule`() {
        val past = allDay(date = LocalDate.of(2026, 10, 20))
        val now = LocalDate.of(2026, 10, 21).atTime(12, 0).atZone(zone).toInstant()
        assertNull(Reminders.planned(listOf(past), 9 * 60, 0, now, zone))
    }

    @Test
    fun `a lead can move a reminder into the past, which is then skipped`() {
        val event = allDay(date = LocalDate.of(2026, 10, 26), lead = 7 * 24 * 60)
        val now = LocalDate.of(2026, 10, 20).atTime(12, 0).atZone(zone).toInstant()
        assertNull(Reminders.planned(listOf(event), 9 * 60, 0, now, zone))
    }
}
