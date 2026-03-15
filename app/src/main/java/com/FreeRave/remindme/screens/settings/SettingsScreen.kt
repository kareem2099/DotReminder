package com.FreeRave.remindme.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Changed for AutoMirrored
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.FreeRave.remindme.screens.settings.components.AppearanceSettingsSection
import com.FreeRave.remindme.screens.settings.components.ClearRemindersConfirmationDialog
import com.FreeRave.remindme.screens.settings.components.DataManagementSettingsSection
import com.FreeRave.remindme.screens.settings.components.DefaultTimePickerDialog
import com.FreeRave.remindme.screens.settings.components.NotificationSettingsSection
import com.FreeRave.remindme.screens.settings.components.ReminderSettingsSection
import com.FreeRave.remindme.screens.settings.components.SecuritySettingsSection
import com.FreeRave.remindme.screens.settings.components.SupportDeveloperSection
import com.FreeRave.remindme.screens.settings.components.AdPreferencesSection
// import androidx.compose.material.icons.filled.Fingerprint // Removed unused import
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.FreeRave.remindme.screens.auth.AuthViewModel // Added import
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.components.PrimaryButton
import com.FreeRave.remindme.ui.components.SnoozeBottomSheet
import com.FreeRave.remindme.ui.components.ThemeBottomSheet
import com.FreeRave.remindme.ui.theme.AppTypography
import com.FreeRave.remindme.ui.theme.ScreenBackground
import com.FreeRave.remindme.ui.theme.ScreenType
import com.FreeRave.remindme.MainViewModel
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel() // Added AuthViewModel
) {
    // Sound state
    val availableSounds = viewModel.availableSounds
    val selectedSoundResourceName by viewModel.selectedSoundResourceName.collectAsState()
    var showSoundDropdown by remember { mutableStateOf(false) }

    // Theme state
    val availableThemes = viewModel.availableThemes
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    var showThemeBottomSheet by remember { mutableStateOf(false) }

    // Snooze state
    val availableSnoozeDurations = viewModel.availableSnoozeDurations
    val selectedSnoozeMinutes by viewModel.selectedSnoozeMinutes.collectAsState()
    var showSnoozeBottomSheet by remember { mutableStateOf(false) }

    // Vibration state
    val vibrationEnabled by viewModel.isVibrationEnabled.collectAsState()

    // Default Time state
    val defaultHour by viewModel.defaultReminderHour.collectAsState()
    val defaultMinute by viewModel.defaultReminderMinute.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current // Re-initialize context here for general use

    // Ad preferences state
    val appOpenAdsEnabled by viewModel.appOpenAdsEnabled.collectAsState()
    val oceanBlueAdsWatched by viewModel.oceanBlueAdsWatched.collectAsState()
    val forestGreenAdsWatched by viewModel.forestGreenAdsWatched.collectAsState()
    val sunsetOrangeAdsWatched by viewModel.sunsetOrangeAdsWatched.collectAsState()
    val midnightPurpleAdsWatched by viewModel.midnightPurpleAdsWatched.collectAsState()
    val candyAdsWatched by viewModel.candyAdsWatched.collectAsState()

    // Biometric and 2FA states are now managed in AccountSecurityScreen or not directly used here.
    // We can remove these state collections from SettingsScreen if they are not used by any direct UI element here.
    // val biometricLoginEnabledState by viewModel.biometricLoginEnabled.collectAsState() 
    // val biometricAvailability = remember { viewModel.getBiometricAvailability() } 
    // val twoFactorAuthEnabledState by viewModel.twoFactorAuthEnabled.collectAsState()
    // var showTwoFactorAuthSetupDialog by remember { mutableStateOf(false) }
    // val current2FASecret by viewModel.currentTwoFactorAuthSecret.collectAsState()


    // Clear All state
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    val isDeleting by viewModel.isDeleting.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Get current theme for background
    val mainViewModel: MainViewModel = hiltViewModel()
    val currentTheme by mainViewModel.currentTheme.collectAsStateWithLifecycle()


    ScreenBackground(
        screenType = ScreenType.SETTINGS,
        themeSetting = currentTheme,
        useDarkTheme = false // Settings screen uses light theme
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }, // Add SnackbarHost
            topBar = {
                TopAppBar(
                    title = { Text("Settings", style = AppTypography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            AdaptiveContainer { windowSizeClass, containerModifier ->
                Column(
                    modifier = containerModifier

                        .padding(paddingValues)

                        .verticalScroll(rememberScrollState()), // Added scrollability
                ) {
                    AdaptiveSpacer(getResponsiveSpacing(16.dp))

                    // --- Appearance Settings ---
                    AppearanceSettingsSection(
                        selectedTheme = selectedTheme,
                        onThemeSettingClicked = { showThemeBottomSheet = true }
                    )

                    // --- Notification Settings ---
                    NotificationSettingsSection(
                        availableSounds = availableSounds,
                        selectedSoundResourceName = selectedSoundResourceName,
                        onSoundSelected = { viewModel.selectSound(it) },
                        showSoundDropdown = showSoundDropdown,
                        onSoundDropdownExpandedChange = { showSoundDropdown = it },
                        context = context,
                        selectedSnoozeMinutes = selectedSnoozeMinutes,
                        onSnoozeSettingClicked = { showSnoozeBottomSheet = true },
                        vibrationEnabled = vibrationEnabled,
                        onVibrationChanged = { viewModel.setVibrationEnabled(it) }
                    )

                    // --- Reminder Settings ---
                    ReminderSettingsSection(
                        defaultHour = defaultHour,
                        defaultMinute = defaultMinute,
                        onDefaultTimeSettingClicked = { showTimePicker = true },
                        onClearDefaultTimeClicked = { viewModel.clearDefaultReminderTime() }
                    )

                    // --- Security Settings ---
                    SecuritySettingsSection(
                        onAccountSecurityClicked = {
                            navController.navigate(Screen.AccountSecurity.route)
                        }
                    )


                    // --- Support Developer ---
                    SupportDeveloperSection(
                        onSupportClicked = {
                            navController.navigate(Screen.Support.route)
                        }
                    )

                    // --- Ad Preferences ---
                    AdPreferencesSection(
                        appOpenAdsEnabled = appOpenAdsEnabled,
                        onAppOpenAdsToggle = { viewModel.setAppOpenAdsEnabled(it) }
                    )

                    // --- Data Management Settings ---
                    DataManagementSettingsSection(
                        isDeleting = isDeleting,
                        onClearAllRemindersClicked = { showClearConfirmationDialog = true }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    PrimaryButton(
                        text = "Sign Out",
                        onClick = { navController.navigate(Screen.SignOut.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = getResponsivePadding(16.dp))
                    )
                }
            }

            // Theme Bottom Sheet
            ThemeBottomSheet(
                showSheet = showThemeBottomSheet,
                availableThemes = availableThemes,
                selectedTheme = selectedTheme,
                oceanBlueAdsWatched = oceanBlueAdsWatched,
                forestGreenAdsWatched = forestGreenAdsWatched,
                sunsetOrangeAdsWatched = sunsetOrangeAdsWatched,
                midnightPurpleAdsWatched = midnightPurpleAdsWatched,
                candyAdsWatched = candyAdsWatched,
                onThemeSelected = { viewModel.selectTheme(it) },
                onOceanBlueAdWatched = {
                    // Increment the ad watch count in the repository
                    kotlinx.coroutines.GlobalScope.launch {
                        viewModel.incrementOceanBlueAdsWatched()
                    }
                },
                onForestGreenAdWatched = {
                    // Increment the ad watch count in the repository
                    kotlinx.coroutines.GlobalScope.launch {
                        viewModel.incrementForestGreenAdsWatched()
                    }
                },
                onSunsetOrangeAdWatched = {
                    // Increment the ad watch count in the repository
                    kotlinx.coroutines.GlobalScope.launch {
                        viewModel.incrementSunsetOrangeAdsWatched()
                    }
                },
                onMidnightPurpleAdWatched = {
                    // Increment the ad watch count in the repository
                    kotlinx.coroutines.GlobalScope.launch {
                        viewModel.incrementMidnightPurpleAdsWatched()
                    }
                },
                onCandyAdWatched = {
                    // Increment the ad watch count in the repository
                    kotlinx.coroutines.GlobalScope.launch {
                        viewModel.incrementCandyAdsWatched()
                    }
                },
                onDismiss = { showThemeBottomSheet = false }
            )

            // Snooze Bottom Sheet
            SnoozeBottomSheet(
                showSheet = showSnoozeBottomSheet,
                availableDurations = availableSnoozeDurations,
                selectedMinutes = selectedSnoozeMinutes,
                onDurationSelected = { viewModel.selectSnoozeDuration(it) },
                onDismiss = { showSnoozeBottomSheet = false }
            )

            // Time Picker Dialog for Default Time
            DefaultTimePickerDialog(
                showDialog = showTimePicker,
                defaultHour = defaultHour,
                defaultMinute = defaultMinute,
                onTimeSelected = { hour, minute ->
                    viewModel.setDefaultReminderTime(hour, minute)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )

            // Confirmation Dialog for Clearing Reminders
            ClearRemindersConfirmationDialog(
                showDialog = showClearConfirmationDialog,
                onConfirm = {
                    showClearConfirmationDialog = false
                    viewModel.clearAllReminders { success ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (success) "All reminders cleared." else "Failed to clear reminders.",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                onDismiss = { showClearConfirmationDialog = false }
            )

            // Two-Factor Auth Setup Dialog is now managed by AccountSecurityScreen
        } // Closes ScreenBackground
    }
}
