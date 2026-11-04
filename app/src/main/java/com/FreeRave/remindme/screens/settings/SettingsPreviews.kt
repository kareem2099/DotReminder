package com.FreeRave.remindme.screens.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Preview(name = "Settings · Light", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsLightPreview() = SettingsPreview(false)

@Preview(name = "Settings · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsDarkPreview() = SettingsPreview(true)

@Preview(name = "Settings · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun SettingsCompactPreview() = SettingsPreview(false)

@Preview(name = "Settings · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun SettingsTabletPreview() = SettingsPreview(false)

@Composable
private fun SettingsPreview(dark: Boolean) {
    val theme = if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT
    val snackbar = remember { SnackbarHostState() }
    RemindMeAppTheme(theme, useDarkTheme = dark) {
        GlassTheme {
            SettingsScreenContent(SettingsUiState(theme = theme, defaultHour = 9, defaultMinute = 30),
                "Alarm Sound 1", snackbar, onBack = {}, onPanel = {}, onVibrationChanged = {}, onClearDefaultTime = {},
                onSecurity = {}, onSignOut = {}, onSupport = {}, onAppOpenAdsChanged = {})
        }
    }
}
