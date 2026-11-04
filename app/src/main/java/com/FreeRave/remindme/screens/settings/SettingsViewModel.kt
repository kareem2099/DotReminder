package com.FreeRave.remindme.screens.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.service.BiometricAuthManager
import com.FreeRave.remindme.service.BiometricAvailability
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.utils.NotificationUtils
import com.FreeRave.remindme.RemindMeApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SoundOption(
    val displayName: String,
    val resourceName: String
)

data class ThemeOption(
    val displayName: String,
    val setting: ThemeSetting
)

data class SnoozeOption(
    val displayName: String,
    val minutes: Int
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderService: ReminderService, // Inject ReminderService
    private val authService: FirebaseAuthServiceInterface, // Inject AuthService
    private val biometricAuthManager: BiometricAuthManager // Inject BiometricAuthManager
) : ViewModel() {

    // Keep track of deletion state
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    val availableSounds: List<SoundOption> = listOf(
        SoundOption("Alarm Sound 1", "alarm_sound_1"),
    )

    val availableThemes: List<ThemeOption> = listOf(
        ThemeOption("Light", ThemeSetting.LIGHT),
        ThemeOption("Dark", ThemeSetting.DARK),
        ThemeOption("Follow System", ThemeSetting.SYSTEM),
        ThemeOption("Ocean Blue ★", ThemeSetting.OCEAN_BLUE),
        ThemeOption("Forest Green ★", ThemeSetting.FOREST_GREEN),
        ThemeOption("Sunset Orange ★", ThemeSetting.SUNSET_ORANGE),
        ThemeOption("Midnight Purple ★", ThemeSetting.MIDNIGHT_PURPLE),
        ThemeOption("Candy ★", ThemeSetting.CANDY)
    )

    val availableSnoozeDurations: List<SnoozeOption> = listOf(
        SnoozeOption("5 minutes", 5),
        SnoozeOption("10 minutes", 10),
        SnoozeOption("15 minutes", 15),
        SnoozeOption("30 minutes", 30)
    )

    val selectedSoundResourceName: StateFlow<String> =
        userPreferencesRepository.selectedNotificationSound.map { savedSound ->
            savedSound ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME
        )

    val selectedTheme: StateFlow<ThemeSetting> =
        userPreferencesRepository.selectedTheme.map { themeValue ->
            ThemeSetting.fromValue(themeValue)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.DEFAULT_THEME_SETTING
        )

    val selectedSnoozeMinutes: StateFlow<Int> =
        userPreferencesRepository.defaultSnoozeMinutes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.DEFAULT_SNOOZE_MINUTES_VALUE
        )

    val isVibrationEnabled: StateFlow<Boolean> =
        userPreferencesRepository.isVibrationEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.DEFAULT_VIBRATION_ENABLED
        )

    val defaultReminderHour: StateFlow<Int> =
        userPreferencesRepository.defaultReminderHour.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.NO_DEFAULT_TIME_SET
        )

    val defaultReminderMinute: StateFlow<Int> =
        userPreferencesRepository.defaultReminderMinute.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.NO_DEFAULT_TIME_SET
        )

    val appOpenAdsEnabled: StateFlow<Boolean> =
        userPreferencesRepository.appOpenAdsEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.DEFAULT_APP_OPEN_ADS_ENABLED
        )

    val oceanBlueAdsWatched: StateFlow<Int> =
        userPreferencesRepository.oceanBlueAdsWatched.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    val forestGreenAdsWatched: StateFlow<Int> =
        userPreferencesRepository.forestGreenAdsWatched.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    val sunsetOrangeAdsWatched: StateFlow<Int> =
        userPreferencesRepository.sunsetOrangeAdsWatched.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    val midnightPurpleAdsWatched: StateFlow<Int> =
        userPreferencesRepository.midnightPurpleAdsWatched.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    val candyAdsWatched: StateFlow<Int> =
        userPreferencesRepository.candyAdsWatched.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    fun getBiometricAvailability(): BiometricAvailability {
        return biometricAuthManager.getBiometricAvailability(application.applicationContext)
    }

    fun setAppOpenAdsEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveAppOpenAdsEnabled(isEnabled)
        }
        // Update AdManager immediately
        RemindMeApp.instance.setAppOpenAdsEnabled(isEnabled)
    }

    fun selectSound(soundResourceName: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveSelectedNotificationSound(soundResourceName)
            // Re-create the notification channel with the new sound and current vibration setting
            val vibration = isVibrationEnabled.first() // Get current vibration setting
            NotificationUtils.createNotificationChannel(
                context = application.applicationContext,
                soundResourceName = soundResourceName,
                vibrationEnabled = vibration
            )
        }
    }

    fun selectTheme(themeSetting: ThemeSetting) {
        viewModelScope.launch {
            userPreferencesRepository.saveSelectedTheme(themeSetting.value)
            // The app theme will be applied based on this preference in MainActivity/App composable
        }
    }

    fun selectSnoozeDuration(minutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveDefaultSnoozeMinutes(minutes)
            // Update the notification action text dynamically (requires more complex notification update logic)
            // Or, simpler: the NotificationReceiver will now read this value when snooze is triggered.
            // We might need to update the snooze button text in the notification itself if possible,
            // but for now, the duration used when snoozing will be correct.
            // No need to recreate channel here as snooze duration doesn't affect the channel itself.
        }
    }

    fun setVibrationEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveVibrationEnabled(isEnabled)
            // Re-create the notification channel with the new vibration setting and current sound
            val sound = selectedSoundResourceName.first() // Get current sound setting
            NotificationUtils.createNotificationChannel(
                context = application.applicationContext,
                soundResourceName = sound,
                vibrationEnabled = isEnabled
            )
        }
    }

    fun setDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveDefaultReminderTime(hour, minute)
        }
    }

    fun clearDefaultReminderTime() {
        viewModelScope.launch {
            userPreferencesRepository.clearDefaultReminderTime()
        }
    }

    fun clearAllReminders(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { // Use IO dispatcher for potentially long operation
            _isDeleting.value = true
            val userId = authService.getCurrentUserId()
            if (userId.isBlank()) {
                _isDeleting.value = false
                onComplete(false) // Cannot delete without user ID
                return@launch
            }

            // Fetch all reminder IDs for the user to cancel notifications
            // Note: This might be inefficient for a very large number of reminders.
            // A more robust solution might involve a Cloud Function or server-side logic.
            val reminderIds = mutableListOf<String>()
            try {
                 reminderService.getReminders(userId).first().forEach { reminderIds.add(it.id) }
            } catch (e: Exception) {
                 // Ignore error fetching IDs for cancellation, proceed with deletion attempt
                 Log.e("SettingsVM", "Error fetching reminder IDs for cancellation", e)
            }


            val success = reminderService.deleteAllRemindersForUser(userId)

            if (success) {
                // Cancel all notifications for the deleted reminders
                reminderIds.forEach { reminderId ->
                    NotificationUtils.cancelNotification(application.applicationContext, reminderId)
                }
            }
             _isDeleting.value = false
            onComplete(success)
        }
    }

    fun incrementOceanBlueAdsWatched() {
        viewModelScope.launch {
            userPreferencesRepository.incrementOceanBlueAdsWatched()
        }
    }

    fun incrementForestGreenAdsWatched() {
        viewModelScope.launch {
            userPreferencesRepository.incrementForestGreenAdsWatched()
        }
    }

    fun incrementSunsetOrangeAdsWatched() {
        viewModelScope.launch {
            userPreferencesRepository.incrementSunsetOrangeAdsWatched()
        }
    }

    fun incrementMidnightPurpleAdsWatched() {
        viewModelScope.launch {
            userPreferencesRepository.incrementMidnightPurpleAdsWatched()
        }
    }

    fun incrementCandyAdsWatched() {
        viewModelScope.launch {
            userPreferencesRepository.incrementCandyAdsWatched()
        }
    }
}
