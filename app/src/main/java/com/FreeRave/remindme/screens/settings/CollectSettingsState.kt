package com.FreeRave.remindme.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SettingsViewModel.collectSettingsState(): SettingsUiState {
    val theme by selectedTheme.collectAsStateWithLifecycle()
    val sound by selectedSoundResourceName.collectAsStateWithLifecycle()
    val snooze by selectedSnoozeMinutes.collectAsStateWithLifecycle()
    val vibration by isVibrationEnabled.collectAsStateWithLifecycle()
    val hour by defaultReminderHour.collectAsStateWithLifecycle()
    val minute by defaultReminderMinute.collectAsStateWithLifecycle()
    val ads by appOpenAdsEnabled.collectAsStateWithLifecycle()
    val deleting by isDeleting.collectAsStateWithLifecycle()
    val ocean by oceanBlueAdsWatched.collectAsStateWithLifecycle()
    val forest by forestGreenAdsWatched.collectAsStateWithLifecycle()
    val sunset by sunsetOrangeAdsWatched.collectAsStateWithLifecycle()
    val midnight by midnightPurpleAdsWatched.collectAsStateWithLifecycle()
    val candy by candyAdsWatched.collectAsStateWithLifecycle()
    return SettingsUiState(theme, sound, snooze, vibration, hour, minute, ads, deleting,
        ThemeUnlockProgress(ocean, forest, sunset, midnight, candy))
}
