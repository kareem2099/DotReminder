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

    val biometricLoginEnabled: StateFlow<Boolean> =
        userPreferencesRepository.biometricLoginEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.DEFAULT_BIOMETRIC_LOGIN_ENABLED
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

    // --- Two-Factor Authentication ---
    val twoFactorAuthEnabled: StateFlow<Boolean> =
        userPreferencesRepository.twoFactorAuthEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.DEFAULT_TWO_FACTOR_AUTH_ENABLED
        )
    
    // This would hold the temporary secret during setup, cleared afterwards from local prefs
    // For long-term storage, it should be encrypted or managed by a backend.
    val currentTwoFactorAuthSecret = MutableStateFlow<String?>(null) // Holds the raw secret for setup
    val currentTwoFactorAuthOtpAuthUri = MutableStateFlow<String?>(null) // Holds the otpauth:// URI for QR code
    val generatedBackupCodes = MutableStateFlow<List<String>>(emptyList()) // Holds generated backup codes


    fun getBiometricAvailability(): BiometricAvailability {
        return biometricAuthManager.getBiometricAvailability(application.applicationContext)
    }

    fun setBiometricLoginEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveBiometricLoginEnabled(isEnabled)
        }
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

    // --- Mock 2FA Logic ---
    private fun generateMock2FASecret(): String {
        // In a real app, use a proper library like dev.samstevens.totp or a similar Android-compatible one.
        // This mock generates a 16-character string using Base32-compatible characters.
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toList() // Base32 characters
        return (1..16)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun generateMockBackupCodes(count: Int = 10): List<String> {
        // Generates simple mock backup codes, e.g., 8-digit numbers formatted with a hyphen
        return List(count) {
            val part1 = (1000..9999).random()
            val part2 = (1000..9999).random()
            "$part1-$part2"
        }
    }

    private fun verifyMockTOTP(secret: String?, code: String): Boolean {
        // In a real app, use a library to verify the TOTP against the secret.
        // For improved mock testing, accept any 6-digit code if a secret is present.
        return !secret.isNullOrEmpty() && code.length == 6 && code.all { it.isDigit() }
    }

    fun startTwoFactorAuthSetup(): Pair<String?, String?> { // Returns (secret, otpAuthUri)
        val newSecret = generateMock2FASecret()
        currentTwoFactorAuthSecret.value = newSecret

        // Use user ID for the otpauth URI, default if blank
        val userId = authService.getCurrentUserId()
        val userIdentifier = if (userId.isNotBlank()) userId else "unknown_user"
        val issuer = "RemindMeApp" // Replace with your app's name

        // Format: otpauth://totp/Issuer:userIdentifier?secret=SECRETKEY&issuer=Issuer
        // Ensure the userIdentifier is URL encoded if it contains special characters.
        // For simplicity, direct concatenation is used here.
        val otpAuthUri = "otpauth://totp/${issuer}:${userIdentifier}?secret=${newSecret}&issuer=${issuer}"
        currentTwoFactorAuthOtpAuthUri.value = otpAuthUri
        
        Log.d("SettingsVM", "Generated OTP Auth URI: $otpAuthUri")
        return Pair(newSecret, otpAuthUri)
    }

    fun cancelTwoFactorAuthSetup() {
        currentTwoFactorAuthSecret.value = null // Clear temporary secret
        currentTwoFactorAuthOtpAuthUri.value = null // Clear URI
    }

    fun verifyAndEnableTwoFactorAuth(userInputCode: String, onResult: (success: Boolean, message: String?, backupCodes: List<String>) -> Unit) {
        val secret = currentTwoFactorAuthSecret.value
        if (secret == null) {
            onResult(false, "Setup process not started or secret missing.", emptyList())
            return
        }

        if (verifyMockTOTP(secret, userInputCode)) {
            viewModelScope.launch {
                userPreferencesRepository.saveTwoFactorAuthEnabled(true)
                userPreferencesRepository.saveTwoFactorAuthSecret(secret) // Persist locally (ideally encrypted)
                
                val backupCodes = generateMockBackupCodes()
                generatedBackupCodes.value = backupCodes 
                // In a real app, these codes (or their hashes) would be stored securely on the backend.

                // Simulate updating Firestore
                val userId = authService.getCurrentUserId()
                if (userId.isNotBlank()) {
                    // authService.updateUser2FAStatusInFirestore(userId, true, secret, backupCodes) // Placeholder
                    Log.d("SettingsVM", "Simulating Firestore update for 2FA: User $userId, Enabled: true, Secret: $secret, BackupCodes: $backupCodes")
                }
                // Don't clear currentTwoFactorAuthSecret and URI yet, UI might need them briefly for the backup codes screen.
                // They will be cleared when the backup codes dialog is dismissed.
                onResult(true, "2FA enabled successfully. Please save your backup codes.", backupCodes)
            }
        } else {
            onResult(false, "Invalid verification code.", emptyList())
        }
    }

    fun disableTwoFactorAuth(userInputCode: String, onResult: (success: Boolean, message: String?) -> Unit) {
        viewModelScope.launch {
            val storedSecret = userPreferencesRepository.twoFactorAuthSecret.first() // Get the stored secret
            if (storedSecret == null) {
                onResult(false, "2FA is not properly set up or secret is missing.")
                return@launch
            }

            if (verifyMockTOTP(storedSecret, userInputCode)) {
                userPreferencesRepository.saveTwoFactorAuthEnabled(false)
                // Secret is cleared automatically by saveTwoFactorAuthEnabled(false) in UserPreferencesRepository

                // Simulate updating Firestore
                val userId = authService.getCurrentUserId()
                if (userId.isNotBlank()) {
                    // authService.updateUser2FAStatusInFirestore(userId, false, null) // Placeholder
                    Log.d("SettingsVM", "Simulating Firestore update for 2FA: User $userId, Enabled: false")
                }
                onResult(true, "2FA disabled successfully.")
            } else {
                onResult(false, "Invalid verification code. 2FA not disabled.")
            }
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
