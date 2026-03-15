package com.FreeRave.remindme.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.FreeRave.remindme.MainActivity
import com.FreeRave.remindme.data.UserPreferencesRepository
// Ensure R is imported from your package, not android.R
import com.FreeRave.remindme.RemindMeApp // Import Application class
import com.FreeRave.remindme.model.Reminder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object NotificationUtils {

    const val CHANNEL_ID = "reminder_alarm_channel" // Changed for clarity
    const val CHANNEL_NAME = "Reminder Alarms"
    const val NOTIFICATION_ID_KEY = "notification_id_key"
    const val REMINDER_ID_STRING_KEY = "reminder_id_string_key" // For actual Reminder ID
    const val REMINDER_TITLE_KEY = "reminder_title_key"
    const val REMINDER_MESSAGE_KEY = "reminder_message_key"
    const val REMINDER_ORIGINAL_TIME_KEY = "reminder_original_time_key"
    const val SNOOZE_DURATION_MINUTES_KEY = "snooze_duration_minutes_key"
    const val SOUND_RESOURCE_NAME_KEY = "sound_resource_name_key" // Key for sound


    const val ACTION_SHOW_ALARM = "com.FreeRave.remindmeapp.ACTION_SHOW_ALARM"
    const val ACTION_STOP_ALARM = "com.FreeRave.remindmeapp.ACTION_STOP_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.FreeRave.remindmeapp.ACTION_SNOOZE_ALARM"

    // Updated to accept sound resource name and vibration preference. Empty string for sound means system default.
    fun createNotificationChannel(context: Context, soundResourceName: String, vibrationEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("NotificationUtils", "Creating/Updating channel. Sound requested: '$soundResourceName', Vibration: $vibrationEnabled")

            val alarmSoundUri: Uri = if (soundResourceName.isNotEmpty()) {
                val soundResourceId = context.resources.getIdentifier(soundResourceName, "raw", context.packageName)
                Log.d("NotificationUtils", "Resource ID for '$soundResourceName': $soundResourceId")
                if (soundResourceId != 0) {
                    val customUri = Uri.parse("android.resource://${context.packageName}/$soundResourceId")
                    Log.d("NotificationUtils", "Using custom sound URI: $customUri")
                    customUri
                } else {
                    // Fallback to default system alarm if custom sound resource name is invalid
                    Log.w("NotificationUtils", "Custom sound resource '$soundResourceName' not found. Falling back to default.")
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
            } else {
                // Use default system alarm sound if soundResourceName is empty
                Log.d("NotificationUtils", "Using default system alarm sound.")
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            // Get notification manager once at the start
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Delete any existing channel first to ensure changes take effect
            notificationManager.deleteNotificationChannel(CHANNEL_ID)

            // Create new channel with current settings
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for reminder alarms"
                setSound(alarmSoundUri, android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build())
                enableVibration(vibrationEnabled)
                if (vibrationEnabled) {
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                }
            }

            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationUtils", "Channel '$CHANNEL_ID' created/updated with sound: $alarmSoundUri, vibration: $vibrationEnabled")
        }
    }

    // Updated to accept snooze duration and sound resource name
    fun scheduleNotification(context: Context, reminder: Reminder, snoozeMinutes: Int, soundResourceName: String) {
        Log.d("NotificationUtils", "Scheduling notification for reminder ID ${reminder.id}. Snooze: $snoozeMinutes min, Sound: '$soundResourceName'")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SHOW_ALARM // Specify the action for the receiver
            putExtra(NOTIFICATION_ID_KEY, reminder.id.hashCode())
            putExtra(REMINDER_ID_STRING_KEY, reminder.id) // Pass the actual string ID
            putExtra(REMINDER_TITLE_KEY, reminder.title)
            putExtra(REMINDER_MESSAGE_KEY, reminder.description)
            putExtra(REMINDER_ORIGINAL_TIME_KEY, reminder.reminderTime)
            putExtra(SNOOZE_DURATION_MINUTES_KEY, snoozeMinutes) // Pass snooze duration
            putExtra(SOUND_RESOURCE_NAME_KEY, soundResourceName) // Pass sound resource name
        }

        // Revert to using reminder.id.hashCode() as the request code
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(), // Use original request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Keep IMMUTABLE for now
        )

        val currentTimeMillis = System.currentTimeMillis()
        if (reminder.reminderTime > currentTimeMillis) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.reminderTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.reminderTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.reminderTime,
                        pendingIntent
                    )
                }
            } catch (se: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.reminderTime,
                    pendingIntent
                )
            }
        }
    }

    fun cancelNotification(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = reminderId.hashCode()

        // Cancel the main alarm
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SHOW_ALARM // Ensure action matches the one used for scheduling
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        // Also cancel any displayed notification
        notificationManager.cancel(notificationId)

        // No need to explicitly cancel snooze PI if it uses the same request code as the main alarm PI
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Go fetch preferences and recreate channel right before showing notification
        // This requires accessing the repository, potentially blocking. Use a coroutine.
        // NOTE: Accessing repository directly in Receiver is not ideal.
        // Passing all relevant prefs (sound, vibration) via Intent is cleaner.
        // However, let's try this forceful recreation first.
        val pendingResult = goAsync() // Allow async work in receiver
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefsRepo = RemindMeApp.instance.userPreferencesRepository
                val vibrationEnabled = prefsRepo.isVibrationEnabled.first()

                // Get sound name from intent first, fall back to preferences if missing
                val soundResourceName = intent.getStringExtra(NotificationUtils.SOUND_RESOURCE_NAME_KEY)
                    ?: prefsRepo.selectedNotificationSound.first()
                    ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME

                // Recreate channel with the correct sound settings
                NotificationUtils.createNotificationChannel(context, soundResourceName, vibrationEnabled)

                // Now proceed with handling the intent *after* channel is updated
                handleIntent(context, intent, notificationManager, vibrationEnabled)

            } finally {
                pendingResult.finish() // Finish async work
            }
        }
    }

    // Extracted intent handling logic to be called from coroutine
    private fun handleIntent(context: Context, intent: Intent, notificationManager: NotificationManager, vibrationEnabled: Boolean) {
        val action = intent.action
        // Extract only the notification ID initially, as it's needed for all actions
        val notificationId = intent.getIntExtra(NotificationUtils.NOTIFICATION_ID_KEY, System.currentTimeMillis().toInt())
        Log.d("NotificationReceiver", "Handling intent action: $action for notificationId: $notificationId")


        when (action) {
            NotificationUtils.ACTION_SHOW_ALARM -> {
                // Extract details needed only for showing the alarm
                val reminderIdString = intent.getStringExtra(NotificationUtils.REMINDER_ID_STRING_KEY)
                val title = intent.getStringExtra(NotificationUtils.REMINDER_TITLE_KEY) ?: "Reminder"
                val message = intent.getStringExtra(NotificationUtils.REMINDER_MESSAGE_KEY) ?: "You have an alarm."
                val originalTime = intent.getLongExtra(NotificationUtils.REMINDER_ORIGINAL_TIME_KEY, 0L)
                val snoozeMinutes = intent.getIntExtra(
                    NotificationUtils.SNOOZE_DURATION_MINUTES_KEY,
                    UserPreferencesRepository.DEFAULT_SNOOZE_MINUTES_VALUE
                )
                val soundResourceName = intent.getStringExtra(NotificationUtils.SOUND_RESOURCE_NAME_KEY)
                    ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME
                Log.d("NotificationReceiver", "SHOW_ALARM Details - SoundName: $soundResourceName, SnoozeMinutes: $snoozeMinutes")

                // Intent for when notification is tapped
                val activityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    // Optionally pass reminderIdString to navigate to the specific reminder
                    // putExtra("reminder_id_to_open", reminderIdString)
                }
                val activityPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 1, // Unique request code
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Stop Action
                val stopIntent = Intent(context, NotificationReceiver::class.java).apply {
                    this.action = NotificationUtils.ACTION_STOP_ALARM
                    putExtra(NotificationUtils.NOTIFICATION_ID_KEY, notificationId)
                }
                val stopPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 2, // Unique request code
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Snooze Action
                val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                    this.action = NotificationUtils.ACTION_SNOOZE_ALARM
                    putExtra(NotificationUtils.NOTIFICATION_ID_KEY, notificationId)
                    putExtra(NotificationUtils.REMINDER_ID_STRING_KEY, reminderIdString)
                    putExtra(NotificationUtils.REMINDER_TITLE_KEY, title)
                    putExtra(NotificationUtils.REMINDER_MESSAGE_KEY, message)
                    putExtra(NotificationUtils.REMINDER_ORIGINAL_TIME_KEY, originalTime)
                    putExtra(NotificationUtils.SNOOZE_DURATION_MINUTES_KEY, snoozeMinutes) // Pass current snooze duration
                    putExtra(NotificationUtils.SOUND_RESOURCE_NAME_KEY, soundResourceName) // Pass current sound name
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 3, // Unique request code
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Using system alarm icon
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    // Sound is controlled by the notification channel settings
                    .setAutoCancel(false) // Make it non-cancellable by tapping body
                    .setContentIntent(activityPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop Alarm", stopPendingIntent) // Using system pause icon for stop
                    // Use the actual snooze duration in the button text
                    .addAction(
                        android.R.drawable.ic_popup_reminder, // Using system reminder icon for snooze
                        "Snooze $snoozeMinutes min",
                        snoozePendingIntent
                    )
                    .setOngoing(true) // Make it ongoing (persistent)

                // Explicitly set vibration pattern on the notification itself for better compatibility
                if (vibrationEnabled) {
                    notification.setVibrate(longArrayOf(0, 1000, 500, 1000))
                }

                notificationManager.notify(notificationId, notification.build())
            }
            NotificationUtils.ACTION_STOP_ALARM -> {
                notificationManager.cancel(notificationId)
                // If using a foreground service to play sound, stop it here.
            }
            NotificationUtils.ACTION_SNOOZE_ALARM -> {
                notificationManager.cancel(notificationId) // Dismiss current notification

                // Extract details needed for snoozing from the snooze intent itself
                val reminderIdStringSnooze = intent.getStringExtra(NotificationUtils.REMINDER_ID_STRING_KEY)
                val titleSnooze = intent.getStringExtra(NotificationUtils.REMINDER_TITLE_KEY) ?: "Reminder"
                val messageSnooze = intent.getStringExtra(NotificationUtils.REMINDER_MESSAGE_KEY) ?: "You have an alarm."
                val originalTimeSnooze = intent.getLongExtra(NotificationUtils.REMINDER_ORIGINAL_TIME_KEY, 0L)
                val snoozeMinutesSnooze = intent.getIntExtra(
                    NotificationUtils.SNOOZE_DURATION_MINUTES_KEY,
                    UserPreferencesRepository.DEFAULT_SNOOZE_MINUTES_VALUE
                )
                val soundResourceNameSnooze = intent.getStringExtra(NotificationUtils.SOUND_RESOURCE_NAME_KEY)
                    ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME

                if (reminderIdStringSnooze != null) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    // Calculate snooze time using the duration from the snooze intent
                    val snoozeDurationMs = TimeUnit.MINUTES.toMillis(snoozeMinutesSnooze.toLong())
                    val snoozeTime = System.currentTimeMillis() + snoozeDurationMs

                    // Display a Toast message
                    val toastMessage = "Reminder snoozed for $snoozeMinutesSnooze minutes."
                    android.widget.Toast.makeText(context, toastMessage, android.widget.Toast.LENGTH_SHORT).show()

                    // Re-create the intent for showing the alarm, but for the snoozed time
                    val rescheduledIntent = Intent(context, NotificationReceiver::class.java).apply {
                        this.action = NotificationUtils.ACTION_SHOW_ALARM
                        putExtra(NotificationUtils.NOTIFICATION_ID_KEY, notificationId)
                        putExtra(NotificationUtils.REMINDER_ID_STRING_KEY, reminderIdStringSnooze)
                        putExtra(NotificationUtils.REMINDER_TITLE_KEY, titleSnooze)
                        putExtra(NotificationUtils.REMINDER_MESSAGE_KEY, messageSnooze)
                        putExtra(NotificationUtils.REMINDER_ORIGINAL_TIME_KEY, originalTimeSnooze)
                        putExtra(NotificationUtils.SNOOZE_DURATION_MINUTES_KEY, snoozeMinutesSnooze)
                        putExtra(NotificationUtils.SOUND_RESOURCE_NAME_KEY, soundResourceNameSnooze)
                    }

                    val rescheduledPendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId, // IMPORTANT: Use the SAME request code as the original alarm
                                        // to ensure AlarmManager updates it.
                        rescheduledIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Keep IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    snoozeTime,
                                    rescheduledPendingIntent
                                )
                            } else {
                                alarmManager.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    snoozeTime,
                                    rescheduledPendingIntent
                                )
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                snoozeTime,
                                rescheduledPendingIntent
                            )
                        }
                    } catch (se: SecurityException) {
                         alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTime,
                            rescheduledPendingIntent
                        )
                    }
                }
            }
        }
    }

    // Helper function moved inside the receiver class
    private fun getSoundUri(context: Context, soundResourceName: String): Uri {
        return if (soundResourceName.isNotEmpty()) {
            val soundResourceId = context.resources.getIdentifier(soundResourceName, "raw", context.packageName)
            if (soundResourceId != 0) {
                Uri.parse("android.resource://${context.packageName}/$soundResourceId")
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) // Fallback
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) // Default
        }
    }
}
