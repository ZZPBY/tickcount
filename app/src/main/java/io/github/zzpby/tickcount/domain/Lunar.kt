package io.github.zzpby.tickcount.domain

import java.time.LocalDate

/**
 * The Chinese lunar calendar, for the dates a countdown is usually about.
 *
 * There is no lunar calendar in `java.time`, and adding a library for one would undo the "no
 * third-party runtime" line the project is built on, so the table is carried here. Each year
 * is one integer: the high bits say which of the twelve months have thirty days rather than
 * twenty-nine, the low four bits give the leap month (zero when there is none), and bit 16
 * says whether that leap month is thirty days long.
 *
 * The range is 1900 to 2100. Outside it the conversions return null rather than guessing: a
 * wrong lunar date is worse than an honest refusal, because the user would not see it.
 */
object Lunar {

    /** A lunar year, month (1-12) and day (1-30), with its leap month flagged. */
    data class Date(val year: Int, val month: Int, val day: Int, val leap: Boolean = false)

    private const val MIN_YEAR = 1900
    private const val MAX_YEAR = 2100

    /** 1900-01-31 was lunar 1900-1-1, and every conversion below counts from it. */
    private val BASE = LocalDate.of(1900, 1, 31)

    fun inRange(year: Int): Boolean = year in MIN_YEAR..MAX_YEAR

    /**
     * Which month repeats in [year], or zero when none does.
     *
     * A lunar year may contain a thirteenth month — the same month twice, the second one being
     * the leap. It happens in roughly seven years out of nineteen.
     */
    fun leapMonth(year: Int): Int =
        if (!inRange(year)) 0 else INFO[year - MIN_YEAR] and 0xF

    private fun leapDays(year: Int): Int = when {
        leapMonth(year) == 0 -> 0
        INFO[year - MIN_YEAR] and 0x10000 != 0 -> 30
        else -> 29
    }

    /** 29 or 30 days, depending on the month's bit. */
    fun monthDays(year: Int, month: Int): Int {
        if (!inRange(year) || month !in 1..12) return 0
        return if (INFO[year - MIN_YEAR] and (0x10000 shr month) != 0) 30 else 29
    }

    /** The days of an ordinary month, or of the leap one when [leap] is set. */
    fun daysIn(year: Int, month: Int, leap: Boolean): Int =
        if (leap) leapDays(year) else monthDays(year, month)

    private fun yearDays(year: Int): Int {
        var sum = 348
        // The month bits run from 0x8000 down to 0x10, one per month; halving the mask each
        // time is what walks them, not stepping the value by one bit at a time.
        var bit = 0x8000
        while (bit >= 0x10) {
            if (INFO[year - MIN_YEAR] and bit != 0) sum += 1
            bit = bit shr 1
        }
        return sum + leapDays(year)
    }

    /** The solar day a lunar date falls on, or null when the date does not exist. */
    fun toSolar(lunar: Date): LocalDate? {
        if (!inRange(lunar.year)) return null
        if (lunar.month !in 1..12) return null
        val leap = leapMonth(lunar.year)
        if (lunar.leap && leap != lunar.month) return null
        val days = daysIn(lunar.year, lunar.month, lunar.leap)
        if (lunar.day !in 1..days) return null

        var offset = 0L
        for (year in MIN_YEAR until lunar.year) offset += yearDays(year)
        for (month in 1 until lunar.month) {
            offset += monthDays(lunar.year, month)
            if (leap == month) offset += leapDays(lunar.year)
        }
        if (lunar.leap) offset += monthDays(lunar.year, lunar.month)
        offset += lunar.day - 1
        return BASE.plusDays(offset)
    }

    /** The lunar date [solar] falls on, or null for days before the table starts. */
    fun fromSolar(solar: LocalDate): Date? {
        if (solar.isBefore(BASE)) return null
        var remaining = solar.toEpochDay() - BASE.toEpochDay()
        var year = MIN_YEAR
        while (year <= MAX_YEAR) {
            val length = yearDays(year)
            if (remaining < length) break
            remaining -= length
            year++
        }
        if (year > MAX_YEAR) return null

        val leap = leapMonth(year)
        var month = 1
        var leapUsed = false
        while (month <= 12) {
            val length = monthDays(year, month)
            if (remaining < length) return Date(year, month, remaining.toInt() + 1)
            remaining -= length
            // The leap month follows the month it repeats, and only once.
            if (leap == month && !leapUsed) {
                val extra = leapDays(year)
                if (remaining < extra) return Date(year, month, remaining.toInt() + 1, leap = true)
                remaining -= extra
                leapUsed = true
            }
            month++
        }
        return null
    }

    // ---------------------------------------------------------------- the table

    /**
     * One entry per year from 1900 to 2100.
     *
     * Bit 16 is set when the leap month has thirty days, bits 15 down to 4 are the twelve
     * months (set means thirty days), and the low four bits are the leap month, or zero.
     */
    private val INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520,
    )
}
