package com.FreeRave.remindme.screens.settings

import com.FreeRave.remindme.data.ThemeSetting
import org.junit.Assert.*
import org.junit.Test

class SettingsRulesTest {
    @Test fun `standard themes need no rewarded ads`() {
        val progress = ThemeUnlockProgress()
        listOf(ThemeSetting.LIGHT, ThemeSetting.DARK, ThemeSetting.SYSTEM).forEach {
            assertTrue(progress.unlocked(it))
            assertFalse(it.isPremiumTheme)
        }
    }

    @Test fun `every premium theme requires four complete ads`() {
        val below = ThemeUnlockProgress(3, 3, 3, 3, 3)
        val complete = ThemeUnlockProgress(4, 4, 4, 4, 4)
        ThemeSetting.entries.filter { it.isPremiumTheme }.forEach {
            assertFalse(below.unlocked(it))
            assertTrue(complete.unlocked(it))
        }
    }

    @Test fun `unlock progress is independent for each theme`() {
        val progress = ThemeUnlockProgress(ocean = 4, forest = 1, sunset = 2, midnight = 3)
        assertTrue(progress.unlocked(ThemeSetting.OCEAN_BLUE))
        assertEquals(1, progress.watched(ThemeSetting.FOREST_GREEN))
        assertEquals(2, progress.watched(ThemeSetting.SUNSET_ORANGE))
        assertEquals(3, progress.watched(ThemeSetting.MIDNIGHT_PURPLE))
        assertEquals(0, progress.watched(ThemeSetting.CANDY))
        ThemeSetting.entries.filter { it.isPremiumTheme && it != ThemeSetting.OCEAN_BLUE }.forEach {
            assertFalse(progress.unlocked(it))
        }
    }

    @Test fun `invalid stored ad counts remain bounded`() {
        val progress = ThemeUnlockProgress(ocean = -20, forest = Int.MAX_VALUE)
        assertEquals(0, progress.watched(ThemeSetting.OCEAN_BLUE))
        assertFalse(progress.unlocked(ThemeSetting.OCEAN_BLUE))
        assertEquals(4, progress.watched(ThemeSetting.FOREST_GREEN))
        assertTrue(progress.unlocked(ThemeSetting.FOREST_GREEN))
    }

    @Test fun `midnight and end of day are valid default times`() {
        assertTrue(hasDefaultReminderTime(0, 0))
        assertTrue(hasDefaultReminderTime(23, 59))
    }

    @Test fun `an absent or partially set default time stays unset`() {
        listOf(-1 to -1, -1 to 30, 8 to -1).forEach { (hour, minute) ->
            assertFalse(hasDefaultReminderTime(hour, minute))
        }
    }

    @Test fun `corrupt time preferences cannot reach the picker`() {
        listOf(24 to 0, 8 to 60, -2 to 0, 1 to -2, Int.MAX_VALUE to 30).forEach { (hour, minute) ->
            assertFalse(hasDefaultReminderTime(hour, minute))
        }
    }
}
