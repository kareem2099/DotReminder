package com.FreeRave.remindme.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.core.app.NotificationCompat
import com.FreeRave.remindme.MainActivity
import com.FreeRave.remindme.model.Reminder
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class LocationBasedNotificationUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val LOCATION_CHANNEL_ID = "location_reminder_channel"
        const val LOCATION_CHANNEL_NAME = "Location Reminders"

        const val ACTION_SNOOZE_LOCATION = "com.FreeRave.remindme.ACTION_SNOOZE_LOCATION"
        const val ACTION_MARK_DONE = "com.FreeRave.remindme.ACTION_MARK_DONE"
        const val ACTION_VIEW_LOCATION = "com.FreeRave.remindme.ACTION_VIEW_LOCATION"

        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }

    init {
        createLocationNotificationChannel()
    }

    private fun createLocationNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Delete existing channel to ensure updates take effect
            notificationManager.deleteNotificationChannel(LOCATION_CHANNEL_ID)

            val channel = android.app.NotificationChannel(
                LOCATION_CHANNEL_ID,
                LOCATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for location-based reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showLocationBasedNotification(
        reminder: Reminder,
        triggerType: String,
        currentLocation: LatLng? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationId = reminder.id.hashCode()

            // Build rich notification content
            val (title, message, bigText) = buildNotificationContent(reminder, triggerType, currentLocation)

            // Create pending intents for actions
            val mainIntent = createMainIntent(reminder.id)
            val snoozeIntent = createSnoozeIntent(reminder.id)
            val doneIntent = createDoneIntent(reminder.id)
            val locationIntent = createLocationIntent(reminder)

            val notification = NotificationCompat.Builder(context, LOCATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(mainIntent)
                .setColor(0xFF4285F4.toInt()) // Google Blue
                .setColorized(true)
                .addAction(
                    android.R.drawable.ic_popup_reminder,
                    "Snooze 10min",
                    snoozeIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Mark Done",
                    doneIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_mylocation,
                    "View Location",
                    locationIntent
                )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification.build())
        }
    }

    private suspend fun buildNotificationContent(
        reminder: Reminder,
        triggerType: String,
        currentLocation: LatLng?
    ): Triple<String, String, String> {
        val locationUtils = LocationUtils(context)

        // Get address for the reminder location
        val reminderAddress = reminder.latitude?.let { lat ->
            reminder.longitude?.let { lng ->
                locationUtils.reverseGeocode(lat, lng)
            }
        } ?: "Unknown location"

        // Calculate distance if current location available
        val distanceText = if (currentLocation != null && reminder.latitude != null && reminder.longitude != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                reminder.latitude, reminder.longitude,
                results
            )
            val distanceKm = results[0] / 1000f
            if (distanceKm < 1f) {
                "${(distanceKm * 1000).roundToInt()}m away"
            } else {
                "${distanceKm.roundToInt()}km away"
            }
        } else ""

        val title = "${reminder.title} 📍"

        val shortMessage = when (triggerType.lowercase()) {
            "enter" -> "You entered the reminder area"
            "exit" -> "You left the reminder area"
            "dwell" -> "You've been in the area for a while"
            else -> "Location reminder triggered"
        }

        val bigText = buildString {
            append(shortMessage)
            if (distanceText.isNotEmpty()) {
                append(" • $distanceText")
            }
            append("\n\nLocation: $reminderAddress")
            if (reminder.description.isNotBlank()) {
                append("\n\n${reminder.description}")
            }
            append("\n\nTap to view details")
        }

        return Triple(title, shortMessage, bigText)
    }

    private fun createMainIntent(reminderId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_reminder", reminderId)
        }
        return PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createSnoozeIntent(reminderId: String): PendingIntent {
        val intent = Intent(context, LocationNotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE_LOCATION
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_SNOOZE_MINUTES, 10)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDoneIntent(reminderId: String): PendingIntent {
        val intent = Intent(context, LocationNotificationReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createLocationIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_reminder", reminder.id)
            putExtra("show_location", true)
            putExtra("location_lat", reminder.latitude)
            putExtra("location_lng", reminder.longitude)
        }
        return PendingIntent.getActivity(
            context,
            reminder.id.hashCode() + 3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


}
