package com.FreeRave.remindme.screens.settings

import androidx.annotation.StringRes
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting

@get:StringRes
internal val ThemeSetting.titleResource: Int get() = when (this) {
    ThemeSetting.LIGHT -> R.string.settings_theme_light
    ThemeSetting.DARK -> R.string.settings_theme_dark
    ThemeSetting.SYSTEM -> R.string.settings_theme_system
    ThemeSetting.OCEAN_BLUE -> R.string.settings_theme_ocean
    ThemeSetting.FOREST_GREEN -> R.string.settings_theme_forest
    ThemeSetting.SUNSET_ORANGE -> R.string.settings_theme_sunset
    ThemeSetting.MIDNIGHT_PURPLE -> R.string.settings_theme_midnight
    ThemeSetting.CANDY -> R.string.settings_theme_candy
}
