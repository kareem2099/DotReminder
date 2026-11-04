package com.FreeRave.remindme.screens.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.screens.settings.components.*
import com.FreeRave.remindme.ui.components.SnoozeBottomSheet
import com.FreeRave.remindme.ui.components.ThemeBottomSheet
import com.FreeRave.remindme.ui.theme.GlassTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val state = viewModel.collectSettingsState()
    var panel by rememberSaveable { mutableStateOf<SettingsPanel?>(null) }
    val dismiss = { panel = null }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val soundLabel = viewModel.availableSounds.firstOrNull { it.resourceName == state.soundResource }?.displayName
        ?: stringResource(R.string.settings_default_sound)

    GlassTheme {
        SettingsScreenContent(
            state, soundLabel, snackbar,
            onBack = { navController.popBackStack() }, onPanel = { panel = it },
            onVibrationChanged = viewModel::setVibrationEnabled,
            onClearDefaultTime = viewModel::clearDefaultReminderTime,
            onSecurity = { navController.navigate(Screen.AccountSecurity.route) },
            onSignOut = { navController.navigate(Screen.SignOut.route) },
            onSupport = { navController.navigate(Screen.Support.route) },
            onAppOpenAdsChanged = viewModel::setAppOpenAdsEnabled
        )
        ThemeBottomSheet(
            showSheet = panel == SettingsPanel.THEME,
            availableThemes = viewModel.availableThemes, selectedTheme = state.theme, progress = state.themeProgress,
            onThemeSelected = viewModel::selectTheme,
            onReward = { theme ->
                when (theme) {
                    ThemeSetting.OCEAN_BLUE -> viewModel.incrementOceanBlueAdsWatched()
                    ThemeSetting.FOREST_GREEN -> viewModel.incrementForestGreenAdsWatched()
                    ThemeSetting.SUNSET_ORANGE -> viewModel.incrementSunsetOrangeAdsWatched()
                    ThemeSetting.MIDNIGHT_PURPLE -> viewModel.incrementMidnightPurpleAdsWatched()
                    ThemeSetting.CANDY -> viewModel.incrementCandyAdsWatched()
                    else -> Unit
                }
            }, onDismiss = dismiss
        )
        if (panel == SettingsPanel.SOUND) {
            SoundBottomSheet(viewModel.availableSounds, state.soundResource, viewModel::selectSound, dismiss)
        }
        SnoozeBottomSheet(panel == SettingsPanel.SNOOZE, viewModel.availableSnoozeDurations,
            state.snoozeMinutes, viewModel::selectSnoozeDuration, dismiss)
        DefaultTimePickerDialog(panel == SettingsPanel.DEFAULT_TIME, state.defaultHour, state.defaultMinute,
            onTimeSelected = { hour, minute -> viewModel.setDefaultReminderTime(hour, minute); dismiss() }, onDismiss = dismiss)
        ClearRemindersConfirmationDialog(panel == SettingsPanel.CLEAR_REMINDERS,
            onConfirm = {
                dismiss()
                if (!state.isDeleting) viewModel.clearAllReminders { success ->
                    scope.launch {
                        snackbar.showSnackbar(resources.getString(if (success) R.string.settings_clear_success else R.string.settings_clear_error))
                    }
                }
            }, onDismiss = dismiss)
    }
}
