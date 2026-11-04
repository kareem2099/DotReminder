package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.settings.titleResource

@Composable
fun AppearanceSettingsSection(selectedTheme: ThemeSetting, onThemeSettingClicked: () -> Unit) {
    SettingsSection(stringResource(R.string.settings_appearance)) {
        SettingItem(stringResource(R.string.settings_theme), stringResource(selectedTheme.titleResource),
            onThemeSettingClicked, icon = Icons.Outlined.Palette)
    }
}
