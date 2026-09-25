package com.tickcount.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

/** One cell of the month grid. */
data class CalendarCell(
    val date: LocalDate,
    /** False for the leading/trailing days borrowed from the neighbouring months. */
    val inMonth: Boolean,
)

/** The first day of the week for [locale] — Sunday in the US, Monday in most of Europe and China. */
fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek =
    WeekFields.of(locale).firstDayOfWeek

/** The seven weekday column headers, in the order the grid is laid out. */
fun weekDaysInOrder(locale: Locale = Locale.getDefault()): List<DayOfWeek> {
    val first = firstDayOfWeek(locale)
    return (0..6).map { first.plus(it.toLong()) }
}

/**
 * Builds the whole month grid, padded to complete weeks.
 *
 * The grid always starts on [firstDayOfWeek] and always ends on a complete week,
 * so there is no special "5 or 6 rows" branch anywhere in the UI — the cell count
 * simply falls out of the arithmetic.
 */
fun monthCells(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek = firstDayOfWeek(),
): List<CalendarCell> {
    val firstOfMonth = month.atDay(1)
    // How many days of the previous month are needed to reach the first column.
    val leading = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val totalCells = leading + month.lengthOfMonth()
    val rows = (totalCells + 6) / 7
    val start = firstOfMonth.minusDays(leading.toLong())

    return (0 until rows * 7).map { offset ->
        val date = start.plusDays(offset.toLong())
        CalendarCell(date = date, inMonth = YearMonth.from(date) == month)
    }
}
