package com.FreeRave.remindme.screens.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {
    @Test
    fun aFourWeekFebruaryHasExactlyFourCompleteRows() {
        val dates = calendarDates(YearMonth.of(2021, 2), DayOfWeek.MONDAY)
        assertEquals(28, dates.size)
        assertEquals(LocalDate.of(2021, 2, 1), dates.first())
        assertEquals(LocalDate.of(2021, 2, 28), dates.last())
    }

    @Test
    fun aSixWeekMonthIncludesItsLastWeekWithoutClipping() {
        val dates = calendarDates(YearMonth.of(2026, 8), DayOfWeek.MONDAY)
        assertEquals(42, dates.size)
        assertEquals(LocalDate.of(2026, 7, 27), dates.first())
        assertEquals(LocalDate.of(2026, 9, 6), dates.last())
        assertTrue(LocalDate.of(2026, 8, 31) in dates)
    }

    @Test
    fun leapDayIsIncluded() {
        val dates = calendarDates(YearMonth.of(2024, 2), DayOfWeek.MONDAY)
        assertEquals(35, dates.size)
        assertTrue(LocalDate.of(2024, 2, 29) in dates)
    }

    @Test
    fun weekStartsFollowTheRequestedLocaleConvention() {
        val month = YearMonth.of(2026, 10)
        assertEquals(LocalDate.of(2026, 9, 26), calendarDates(month, DayOfWeek.SATURDAY).first())
        assertEquals(LocalDate.of(2026, 9, 27), calendarDates(month, DayOfWeek.SUNDAY).first())
        assertEquals(LocalDate.of(2026, 9, 28), calendarDates(month, DayOfWeek.MONDAY).first())
    }

    @Test
    fun theSameDayNumberInAnotherMonthIsNotToday() {
        val today = LocalDate.of(2026, 10, 29)
        val selected = LocalDate.of(2026, 11, 29)
        val days = calendarDays(YearMonth.from(selected), selected, today, DayOfWeek.MONDAY, emptyMap())
        assertEquals(today, days.single { it.isToday }.date)
        assertFalse(days.single { it.date == selected }.isToday)
        assertTrue(days.single { it.date == selected }.isSelected)
    }

    @Test
    fun theSameMonthAndDayInAnotherYearIsNotToday() {
        val selected = LocalDate.of(2025, 10, 29)
        val days = calendarDays(YearMonth.from(selected), selected, LocalDate.of(2026, 10, 29), DayOfWeek.SUNDAY, emptyMap())
        assertTrue(days.none { it.isToday })
    }

    @Test
    fun movingMonthsClampsTheSelectedDayAndHandlesYearBoundaries() {
        assertEquals(LocalDate.of(2027, 2, 28), adjacentMonthDate(LocalDate.of(2027, 1, 31), 1))
        assertEquals(LocalDate.of(2028, 2, 29), adjacentMonthDate(LocalDate.of(2028, 1, 31), 1))
        assertEquals(LocalDate.of(2027, 1, 31), adjacentMonthDate(LocalDate.of(2026, 12, 31), 1))
        assertEquals(LocalDate.of(2025, 12, 15), adjacentMonthDate(LocalDate.of(2026, 1, 15), -1))
    }
}
