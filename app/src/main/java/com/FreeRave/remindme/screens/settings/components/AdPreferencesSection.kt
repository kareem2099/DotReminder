package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R

@Composable
fun AdPreferencesSection(appOpenAdsEnabled: Boolean, onAppOpenAdsToggle: (Boolean) -> Unit) {
    SettingsSection(stringResource(R.string.settings_ads)) {
        SettingItemWithSwitch(stringResource(R.string.settings_app_open_ads), appOpenAdsEnabled, onAppOpenAdsToggle,
            subtitle = stringResource(R.string.settings_app_open_ads_hint), icon = Icons.Outlined.PlayCircleOutline)
    }
}
