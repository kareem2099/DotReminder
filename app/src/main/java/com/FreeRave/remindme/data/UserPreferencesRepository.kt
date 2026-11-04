package com.FreeRave.remindme.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.* // Use wildcard to include intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class BiometricLockPreference(val enabled: Boolean = false, val ownerUid: String? = null) {
    // Old enabled preferences have no owner: keep the session locked until the user authenticates.
    fun protects(uid: String?) = !uid.isNullOrBlank() && enabled && (ownerUid == null || ownerUid == uid)
}

// Define the DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_NOTIFICATION_SOUND = stringPreferencesKey("selected_notification_sound")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour") // Added key
        val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute") // Added key
        val REMEMBER_ME_ENABLED = booleanPreferencesKey("remember_me_enabled") // Added key
        val REMEMBERED_EMAIL = stringPreferencesKey("remembered_email") // Added key
        val BIOMETRIC_LOGIN_ENABLED = booleanPreferencesKey("biometric_login_enabled") // Added key
        val BIOMETRIC_OWNER_UID = stringPreferencesKey("biometric_owner_uid")
        val TWO_FACTOR_AUTH_ENABLED = booleanPreferencesKey("two_factor_auth_enabled")
        val TWO_FACTOR_AUTH_SECRET = stringPreferencesKey("two_factor_auth_secret")
        val APP_OPEN_ADS_ENABLED = booleanPreferencesKey("app_open_ads_enabled")
        val OCEAN_BLUE_ADS_WATCHED = intPreferencesKey("ocean_blue_ads_watched")
        val FOREST_GREEN_ADS_WATCHED = intPreferencesKey("forest_green_ads_watched")
        val SUNSET_ORANGE_ADS_WATCHED = intPreferencesKey("sunset_orange_ads_watched")
        val MIDNIGHT_PURPLE_ADS_WATCHED = intPreferencesKey("midnight_purple_ads_watched")
        val CANDY_ADS_WATCHED = intPreferencesKey("candy_ads_watched")
        val CURRENT_SESSION_ID = stringPreferencesKey("current_session_id")
    }

    // --- Notification Sound ---
    val selectedNotificationSound: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_NOTIFICATION_SOUND]
        }

    suspend fun saveSelectedNotificationSound(soundResourceName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_NOTIFICATION_SOUND] = soundResourceName
        }
    }

    val selectedTheme: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME] ?: ThemeSetting.SYSTEM.value
        }

    suspend fun saveSelectedTheme(themeValue: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME] = themeValue
        }
    }

    // --- Snooze Duration ---
    val defaultSnoozeMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_SNOOZE_MINUTES] ?: DEFAULT_SNOOZE_MINUTES_VALUE
        }

    suspend fun saveDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SNOOZE_MINUTES] = minutes
        }
    }

    // --- Vibration ---
    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] ?: DEFAULT_VIBRATION_ENABLED
        }

    suspend fun saveVibrationEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = isEnabled
        }
    }

    // --- Default Reminder Time ---
    // Store hour and minute separately, use -1 to indicate not set
    val defaultReminderHour: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_REMINDER_HOUR] ?: NO_DEFAULT_TIME_SET
        }

    val defaultReminderMinute: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_REMINDER_MINUTE] ?: NO_DEFAULT_TIME_SET
        }

    suspend fun saveDefaultReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.DEFAULT_REMINDER_MINUTE] = minute
        }
    }

    suspend fun clearDefaultReminderTime() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.DEFAULT_REMINDER_HOUR)
            preferences.remove(PreferencesKeys.DEFAULT_REMINDER_MINUTE)
        }
    }

    // --- Remember Me ---
    val rememberMeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.REMEMBER_ME_ENABLED] ?: DEFAULT_REMEMBER_ME_ENABLED
        }

    val rememberedEmail: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.REMEMBERED_EMAIL]
        }

    suspend fun saveRememberMeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMEMBER_ME_ENABLED] = isEnabled
            if (!isEnabled) {
                preferences.remove(PreferencesKeys.REMEMBERED_EMAIL) // Clear email if "Remember Me" is disabled
            }
        }
    }

    suspend fun saveRememberedEmail(email: String?) {
        context.dataStore.edit { preferences ->
            // This function assumes that if an email is being saved,
            // the REMEMBER_ME_ENABLED flag is already true or will be set true by the caller.
            // If email is null, it's cleared.
            if (email != null) {
                preferences[PreferencesKeys.REMEMBERED_EMAIL] = email
            } else {
                preferences.remove(PreferencesKeys.REMEMBERED_EMAIL)
            }
        }
    }

    // --- Biometric Login ---
    val biometricLockPreference: Flow<BiometricLockPreference> = context.dataStore.data.map { preferences ->
        BiometricLockPreference(preferences[PreferencesKeys.BIOMETRIC_LOGIN_ENABLED] ?: false,
            preferences[PreferencesKeys.BIOMETRIC_OWNER_UID])
    }

    suspend fun saveBiometricLock(uid: String, isEnabled: Boolean) {
        require(uid.isNotBlank())
        context.dataStore.edit { preferences ->
            val owner = preferences[PreferencesKeys.BIOMETRIC_OWNER_UID]
            if (!isEnabled && owner != null && owner != uid) return@edit
            preferences[PreferencesKeys.BIOMETRIC_LOGIN_ENABLED] = isEnabled
            if (isEnabled) preferences[PreferencesKeys.BIOMETRIC_OWNER_UID] = uid
            else preferences.remove(PreferencesKeys.BIOMETRIC_OWNER_UID)
        }
    }

    /** Removes the obsolete local demo flag and secret; real factors live only in Firebase. */
    suspend fun clearLegacyTwoFactorDemo() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.TWO_FACTOR_AUTH_ENABLED)
            preferences.remove(PreferencesKeys.TWO_FACTOR_AUTH_SECRET)
        }
    }

    // --- App Open Ads ---
    val appOpenAdsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_OPEN_ADS_ENABLED] ?: DEFAULT_APP_OPEN_ADS_ENABLED
        }

    suspend fun saveAppOpenAdsEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_OPEN_ADS_ENABLED] = isEnabled
        }
    }

    // --- Ocean Blue Theme Ads Watched ---
    val oceanBlueAdsWatched: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.OCEAN_BLUE_ADS_WATCHED] ?: 0
        }

    suspend fun incrementOceanBlueAdsWatched() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[PreferencesKeys.OCEAN_BLUE_ADS_WATCHED] ?: 0
            preferences[PreferencesKeys.OCEAN_BLUE_ADS_WATCHED] = currentCount + 1
        }
    }

    suspend fun resetOceanBlueAdsWatched() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OCEAN_BLUE_ADS_WATCHED] = 0
        }
    }

    // --- Forest Green Theme Ads Watched ---
    val forestGreenAdsWatched: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FOREST_GREEN_ADS_WATCHED] ?: 0
        }

    suspend fun incrementForestGreenAdsWatched() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[PreferencesKeys.FOREST_GREEN_ADS_WATCHED] ?: 0
            preferences[PreferencesKeys.FOREST_GREEN_ADS_WATCHED] = currentCount + 1
        }
    }

    suspend fun resetForestGreenAdsWatched() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOREST_GREEN_ADS_WATCHED] = 0
        }
    }

    // --- Sunset Orange Theme Ads Watched ---
    val sunsetOrangeAdsWatched: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SUNSET_ORANGE_ADS_WATCHED] ?: 0
        }

    suspend fun incrementSunsetOrangeAdsWatched() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[PreferencesKeys.SUNSET_ORANGE_ADS_WATCHED] ?: 0
            preferences[PreferencesKeys.SUNSET_ORANGE_ADS_WATCHED] = currentCount + 1
        }
    }

    suspend fun resetSunsetOrangeAdsWatched() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUNSET_ORANGE_ADS_WATCHED] = 0
        }
    }

    // --- Midnight Purple Theme Ads Watched ---
    val midnightPurpleAdsWatched: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.MIDNIGHT_PURPLE_ADS_WATCHED] ?: 0
        }

    suspend fun incrementMidnightPurpleAdsWatched() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[PreferencesKeys.MIDNIGHT_PURPLE_ADS_WATCHED] ?: 0
            preferences[PreferencesKeys.MIDNIGHT_PURPLE_ADS_WATCHED] = currentCount + 1
        }
    }

    suspend fun resetMidnightPurpleAdsWatched() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MIDNIGHT_PURPLE_ADS_WATCHED] = 0
        }
    }

    // --- Candy Theme Ads Watched ---
    val candyAdsWatched: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CANDY_ADS_WATCHED] ?: 0
        }

    suspend fun incrementCandyAdsWatched() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[PreferencesKeys.CANDY_ADS_WATCHED] ?: 0
            preferences[PreferencesKeys.CANDY_ADS_WATCHED] = currentCount + 1
        }
    }

    suspend fun resetCandyAdsWatched() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CANDY_ADS_WATCHED] = 0
        }
    }

    // --- Current Session ID ---
    val currentSessionId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CURRENT_SESSION_ID]
        }

    suspend fun saveCurrentSessionId(sessionId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_SESSION_ID] = sessionId
        }
    }

    suspend fun clearCurrentSessionId() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CURRENT_SESSION_ID)
        }
    }

    companion object {
        const val DEFAULT_SOUND_RESOURCE_NAME = "alarm_sound_1"
        val DEFAULT_THEME_SETTING = ThemeSetting.SYSTEM
        const val DEFAULT_SNOOZE_MINUTES_VALUE = 5 // Default snooze time in minutes
        const val DEFAULT_VIBRATION_ENABLED = true // Default vibration state
        const val NO_DEFAULT_TIME_SET = -1 // Sentinel value for no default time
        const val DEFAULT_REMEMBER_ME_ENABLED = false // Added default
        const val DEFAULT_BIOMETRIC_LOGIN_ENABLED = false // Added default
        const val DEFAULT_APP_OPEN_ADS_ENABLED = true
        const val DEFAULT_SESSION_ID = ""
    }
}

enum class ThemeSetting(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system"),
    OCEAN_BLUE("ocean_blue"),
    FOREST_GREEN("forest_green"),
    SUNSET_ORANGE("sunset_orange"),
    MIDNIGHT_PURPLE("midnight_purple"),
    CANDY("candy");

    companion object {
        fun fromValue(value: String?): ThemeSetting =
            entries.find { it.value == value } ?: ThemeSetting.SYSTEM
    }
}
