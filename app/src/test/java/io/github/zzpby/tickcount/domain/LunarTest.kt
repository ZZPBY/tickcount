package io.github.zzpby.tickcount.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The lunar table and both directions of the conversion.
 *
 * The table is carried in the source rather than a library, so the only thing standing behind
 * it is a test. The Spring Festival dates below are the ones to check first: lunar 1/1 is the
 * most-quoted date in the calendar, and a wrong entry in a year moves it by up to a month.
 */
class LunarTest {

    private fun springFestival(year: Int) =
        Lunar.toSolar(Lunar.Date(year = year, month = 1, day = 1))

    @Test
    fun `the spring festival falls on the dates it is known to fall on`() {
        val known = mapOf(
            2000 to LocalDate.of(2000, 2, 5),
            2001 to LocalDate.of(2001, 1, 24),
            2002 to LocalDate.of(2002, 2, 12),
            2003 to LocalDate.of(2003, 2, 1),
            2004 to LocalDate.of(2004, 1, 22),
            2005 to LocalDate.of(2005, 2, 9),
            2006 to LocalDate.of(2006, 1, 29),
            2007 to LocalDate.of(2007, 2, 18),
            2008 to LocalDate.of(2008, 2, 7),
            2009 to LocalDate.of(2009, 1, 26),
            2010 to LocalDate.of(2010, 2, 14),
            2011 to LocalDate.of(2011, 2, 3),
            2012 to LocalDate.of(2012, 1, 23),
            2013 to LocalDate.of(2013, 2, 10),
            2014 to LocalDate.of(2014, 1, 31),
            2015 to LocalDate.of(2015, 2, 19),
            2016 to LocalDate.of(2016, 2, 8),
            2017 to LocalDate.of(2017, 1, 28),
            2018 to LocalDate.of(2018, 2, 16),
            2019 to LocalDate.of(2019, 2, 5),
            2020 to LocalDate.of(2020, 1, 25),
            2021 to LocalDate.of(2021, 2, 12),
            2022 to LocalDate.of(2022, 2, 1),
            2023 to LocalDate.of(2023, 1, 22),
            2024 to LocalDate.of(2024, 2, 10),
            2025 to LocalDate.of(2025, 1, 29),
            2026 to LocalDate.of(2026, 2, 17),
            2027 to LocalDate.of(2027, 2, 6),
            2028 to LocalDate.of(2028, 1, 26),
            2029 to LocalDate.of(2029, 2, 13),
            2030 to LocalDate.of(2030, 2, 3),
        )
        known.forEach { (year, date) ->
            assertEquals("spring festival $year", date, springFestival(year))
        }
    }

    @Test
    fun `the mid-autumn festival falls on lunar 8-15 and the dates it is known by`() {
        assertEquals(
            LocalDate.of(2024, 9, 17),
            Lunar.toSolar(Lunar.Date(year = 2024, month = 8, day = 15)),
        )
        assertEquals(
            LocalDate.of(2025, 10, 6),
            Lunar.toSolar(Lunar.Date(year = 2025, month = 8, day = 15)),
        )
        assertEquals(
            LocalDate.of(2026, 9, 25),
            Lunar.toSolar(Lunar.Date(year = 2026, month = 8, day = 15)),
        )
    }

    @Test
    fun `the leap months are the ones the calendar has`() {
        assertEquals(4, Lunar.leapMonth(2020))
        assertEquals(2, Lunar.leapMonth(2023))
        assertEquals(6, Lunar.leapMonth(2025))
        assertEquals(0, Lunar.leapMonth(2024))
        assertEquals(0, Lunar.leapMonth(2026))
    }

    @Test
    fun `a leap month only exists in its own year`() {
        assertNull(Lunar.toSolar(Lunar.Date(year = 2024, month = 4, day = 1, leap = true)))
        assertNull(Lunar.toSolar(Lunar.Date(year = 2023, month = 4, day = 1, leap = true)))
    }

    @Test
    fun `every lunar day maps back to itself`() {
        // Consistency across the whole supported range: if any month length were wrong, the
        // two directions would drift apart somewhere in these two hundred years.
        var lunar = Lunar.Date(year = 1900, month = 1, day = 1)
        var solar = LocalDate.of(1900, 1, 31)
        var days = 0
        while (lunar.year < 2100) {
            assertEquals("solar $solar", lunar, Lunar.fromSolar(solar))
            assertEquals("lunar $lunar", solar, Lunar.toSolar(lunar))
            solar = solar.plusDays(1)
            days++
            lunar = Lunar.fromSolar(solar) ?: break
        }
        assertEquals("days walked", true, days > 72_000)
    }

    @Test
    fun `a date that does not exist is refused rather than moved`() {
        assertNull(Lunar.toSolar(Lunar.Date(year = 2026, month = 1, day = 31)))
        assertNull(Lunar.toSolar(Lunar.Date(year = 2026, month = 13, day = 1)))
        assertNull(Lunar.toSolar(Lunar.Date(year = 1800, month = 1, day = 1)))
        assertNull(Lunar.fromSolar(LocalDate.of(1900, 1, 30)))
    }
}
