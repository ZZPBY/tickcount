package io.github.zzpby.tickcount.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class CalendarMathTest {

    @Test
    fun `the grid is always a whole number of weeks`() {
        var month = YearMonth.of(2026, 1)
        repeat(36) {
            listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.SATURDAY).forEach { first ->
                val cells = monthCells(month, first)
                assertEquals(
                    "rows must be complete weeks for $month starting $first",
                    0,
                    cells.size % 7,
                )
                assertTrue("a month never needs more than 6 rows", cells.size <= 42)
                assertTrue("a month never fits in fewer than 4 rows", cells.size >= 28)
            }
            month = month.plusMonths(1)
        }
    }

    @Test
    fun `every day of the month appears exactly once and in order`() {
        val month = YearMonth.of(2026, 9)
        val cells = monthCells(month, DayOfWeek.SUNDAY)

        val inMonth = cells.filter { it.inMonth }
        assertEquals(month.lengthOfMonth(), inMonth.size)
        assertEquals(
            (1..month.lengthOfMonth()).map { month.atDay(it) },
            inMonth.map { it.date },
        )
    }

    @Test
    fun `cells are one unbroken run of consecutive days`() {
        val cells = monthCells(YearMonth.of(2027, 2), DayOfWeek.MONDAY)

        cells.zipWithNext { a, b ->
            assertEquals(1L, ChronoUnit.DAYS.between(a.date, b.date))
        }
    }

    @Test
    fun `the grid starts on the configured first day of week`() {
        val month = YearMonth.of(2026, 9)

        assertEquals(DayOfWeek.SUNDAY, monthCells(month, DayOfWeek.SUNDAY).first().date.dayOfWeek)
        assertEquals(DayOfWeek.MONDAY, monthCells(month, DayOfWeek.MONDAY).first().date.dayOfWeek)
        assertEquals(DayOfWeek.SATURDAY, monthCells(month, DayOfWeek.SATURDAY).first().date.dayOfWeek)
    }

    @Test
    fun `september 2026 pads with two leading days when weeks start on sunday`() {
        // 2026-09-01 is a Tuesday, so Sunday-start needs Sun 30 and Mon 31 of August.
        val cells = monthCells(YearMonth.of(2026, 9), DayOfWeek.SUNDAY)

        assertEquals(LocalDate.of(2026, 8, 30), cells.first().date)
        assertEquals(false, cells[0].inMonth)
        assertEquals(false, cells[1].inMonth)
        assertEquals(LocalDate.of(2026, 9, 1), cells[2].date)
        assertEquals(true, cells[2].inMonth)
        assertEquals(35, cells.size)
    }

    @Test
    fun `august 2026 needs six rows`() {
        // 2026-08-01 is a Saturday, the worst case for a Sunday-start grid.
        val cells = monthCells(YearMonth.of(2026, 8), DayOfWeek.SUNDAY)

        assertEquals(42, cells.size)
        assertEquals(LocalDate.of(2026, 7, 26), cells.first().date)
    }

    @Test
    fun `february 2027 fits in four rows when weeks start on monday`() {
        // 2027-02-01 is a Monday: a perfectly aligned 28-day month.
        val cells = monthCells(YearMonth.of(2027, 2), DayOfWeek.MONDAY)

        assertEquals(28, cells.size)
        assertEquals(LocalDate.of(2027, 2, 1), cells.first().date)
        assertTrue(cells.all { it.inMonth })
    }

    @Test
    fun `no grid ever contains a duplicate day`() {
        val month = YearMonth.of(2028, 2) // leap February
        val cells = monthCells(month, DayOfWeek.MONDAY)

        assertEquals(cells.size, cells.map { it.date }.toSet().size)
    }

    @Test
    fun `weekDaysInOrder follows the locale first day`() {
        listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.SATURDAY).forEach { first ->
            val rotation = (0..6).map { first.plus(it.toLong()) }
            assertEquals(first, rotation.first())
            assertEquals(7, rotation.toSet().size)
        }
    }
}
