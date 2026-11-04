package com.FreeRave.remindme.ui

import androidx.compose.ui.graphics.Color
import com.FreeRave.remindme.ui.location.LocationDraft
import com.FreeRave.remindme.ui.location.locationNumber
import com.FreeRave.remindme.ui.reminder.*
import com.FreeRave.remindme.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class PolishRulesTest {
    @Test fun timeInputAcceptsArabicAndPersianDigits() {
        assertEquals(23, selectedTimeHour("٢٣", true, false))
        assertEquals(45, selectedTimeMinute("۴۵"))
    }
    @Test fun twelveHourMidnightAndNoonAreDifferent() {
        assertEquals(0, selectedTimeHour("12", false, false))
        assertEquals(12, selectedTimeHour("12", false, true))
        assertEquals(23, selectedTimeHour("11", false, true))
    }
    @Test fun timeInputRejectsHoursOutsideTheSelectedClockMode() {
        assertNull(selectedTimeHour("24", true, false))
        assertNull(selectedTimeHour("0", false, false))
        assertNull(selectedTimeHour("13", false, false))
    }
    @Test fun minuteInputRejectsInvalidAndMissingValues() {
        listOf("", "60", "-1", "1.5", "NaN").forEach { assertNull(selectedTimeMinute(it)) }
    }
    @Test fun locationAcceptsZeroAndBoundaryCoordinates() {
        assertTrue(LocationDraft("0", "0", "200").valid)
        assertTrue(LocationDraft("-90", "180", "1").valid)
    }
    @Test fun locationRejectsOutOfRangeCoordinates() {
        assertFalse(LocationDraft("91", "30", "200").valid)
        assertFalse(LocationDraft("30", "181", "200").valid)
    }
    @Test fun locationRejectsNonFiniteCoordinatesAndRadius() {
        listOf("NaN", "Infinity", "-Infinity", "1e400").forEach {
            assertFalse(LocationDraft(it, "30", "200").valid)
            assertFalse(LocationDraft("30", "30", it).valid)
        }
    }
    @Test fun radiusMustBePositiveAndRepresentableAsAFloat() {
        listOf("0", "-200", "", "1e39").forEach { assertFalse(LocationDraft("30", "30", it).valid) }
    }
    @Test fun locationSupportsLocalDecimalSeparatorsAndArabicDigits() {
        assertEquals(30.25, locationNumber("٣٠٫٢٥")!!, 0.0001)
        assertEquals(-31.5, locationNumber("−31,5")!!, 0.0001)
    }
    @Test fun lightAccentsKeepWhiteButtonTextReadable() {
        listOf(Color.Yellow, Color.Cyan, Color(0xFFFFAA66), Color(0xFFF18CB8), Color.White).forEach {
            assertTrue(colorContrast(glassAccent(it, false), Color.White) >= 4.99f)
        }
    }
    @Test fun darkAccentsKeepTheirHueAndChooseContrastingText() {
        val accent = Color(0xFF8CE1CE)
        assertEquals(accent, glassAccent(accent, true))
        assertTrue(colorContrast(accent, textOnAccent(accent)) >= 4.5f)
    }
    @Test fun blackAndWhiteHaveTheExpectedContrastRange() {
        assertEquals(21f, colorContrast(Color.Black, Color.White), 0.01f)
        assertEquals(1f, colorContrast(Color.White, Color.White), 0.01f)
    }
}
