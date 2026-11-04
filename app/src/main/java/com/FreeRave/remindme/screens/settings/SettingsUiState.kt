package com.FreeRave.remindme.screens.settings

import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.data.UserPreferencesRepository

internal data class SettingsUiState(
    val theme: ThemeSetting = ThemeSetting.SYSTEM,
    val soundResource: String = UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME,
    val snoozeMinutes: Int = UserPreferencesRepository.DEFAULT_SNOOZE_MINUTES_VALUE,
    val vibrationEnabled: Boolean = UserPreferencesRepository.DEFAULT_VIBRATION_ENABLED,
    val defaultHour: Int = UserPreferencesRepository.NO_DEFAULT_TIME_SET,
    val defaultMinute: Int = UserPreferencesRepository.NO_DEFAULT_TIME_SET,
    val appOpenAdsEnabled: Boolean = UserPreferencesRepository.DEFAULT_APP_OPEN_ADS_ENABLED,
    val isDeleting: Boolean = false,
    val themeProgress: ThemeUnlockProgress = ThemeUnlockProgress()
)

internal enum class SettingsPanel { THEME, SOUND, SNOOZE, DEFAULT_TIME, CLEAR_REMINDERS }

internal data class ThemeUnlockProgress(
    val ocean: Int = 0, val forest: Int = 0, val sunset: Int = 0, val midnight: Int = 0, val candy: Int = 0
) {
    fun watched(theme: ThemeSetting): Int = when (theme) {
        ThemeSetting.OCEAN_BLUE -> ocean
        ThemeSetting.FOREST_GREEN -> forest
        ThemeSetting.SUNSET_ORANGE -> sunset
        ThemeSetting.MIDNIGHT_PURPLE -> midnight
        ThemeSetting.CANDY -> candy
        else -> 0
    }.coerceIn(0, REQUIRED_THEME_ADS)

    fun unlocked(theme: ThemeSetting): Boolean = !theme.isPremiumTheme || watched(theme) >= REQUIRED_THEME_ADS
}

internal const val REQUIRED_THEME_ADS = 4
internal val ThemeSetting.isPremiumTheme: Boolean get() = when (this) {
    ThemeSetting.LIGHT, ThemeSetting.DARK, ThemeSetting.SYSTEM -> false
    else -> true
}

internal fun hasDefaultReminderTime(hour: Int, minute: Int): Boolean = hour in 0..23 && minute in 0..59
