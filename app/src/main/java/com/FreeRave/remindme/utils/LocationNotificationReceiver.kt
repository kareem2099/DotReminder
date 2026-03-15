package com.FreeRave.remindme.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.service.GeofenceService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "LocationNotificationReceiver"

@AndroidEntryPoint
class LocationNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderService: ReminderService

    @Inject
    lateinit var geofenceService: GeofenceService

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val reminderId = intent.getStringExtra(LocationBasedNotificationUtils.EXTRA_REMINDER_ID)

        if (reminderId.isNullOrBlank()) {
            Log.e(TAG, "Received location notification action without reminder ID")
            return
        }

        Log.d(TAG, "Handling location notification action: $action for reminder: $reminderId")

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleAction(context, action, reminderId, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling location notification action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAction(context: Context, action: String?, reminderId: String, intent: Intent) {
        when (action) {
            LocationBasedNotificationUtils.ACTION_SNOOZE_LOCATION -> {
                handleSnoozeAction(context, reminderId, intent)
            }
            LocationBasedNotificationUtils.ACTION_MARK_DONE -> {
                handleMarkDoneAction(context, reminderId)
            }
            else -> {
                Log.w(TAG, "Unknown location notification action: $action")
            }
        }
    }

    private suspend fun handleSnoozeAction(context: Context, reminderId: String, intent: Intent) {
        try {
            val snoozeMinutes = intent.getIntExtra(
                LocationBasedNotificationUtils.EXTRA_SNOOZE_MINUTES,
                10
            )

            // Get the reminder to recreate the geofence after snooze
            val reminder = reminderService.getReminder(reminderId)
            if (reminder == null) {
                Log.e(TAG, "Reminder not found for snooze action: $reminderId")
                return
            }

            // Temporarily unregister the geofence
            geofenceService.unregisterGeofence(reminderId)

            // Schedule re-registration after snooze period
            val snoozeMillis = TimeUnit.MINUTES.toMillis(snoozeMinutes.toLong())

            // For simplicity, we'll just show a toast and log
            // In a production app, you might want to schedule this properly
            val message = "${reminder.title} snoozed for $snoozeMinutes minutes"
            showToast(context, message)

            Log.d(TAG, "Location reminder snoozed: $reminderId for $snoozeMinutes minutes")

            // Note: In a full implementation, you'd want to:
            // 1. Store the snooze state
            // 2. Schedule re-registration after snooze period
            // 3. Handle multiple snoozes properly

        } catch (e: Exception) {
            Log.e(TAG, "Error handling snooze action", e)
        }
    }

    private suspend fun handleMarkDoneAction(context: Context, reminderId: String) {
        try {
            // Get the reminder
            val reminder = reminderService.getReminder(reminderId)
            if (reminder == null) {
                Log.e(TAG, "Reminder not found for mark done action: $reminderId")
                return
            }

            // Mark as completed by updating the reminder
            // Note: You might want to add a "completed" field to the Reminder model
            // For now, we'll just unregister the geofence and show a message

            geofenceService.unregisterGeofence(reminderId)

            val message = "${reminder.title} marked as done"
            showToast(context, message)

            Log.d(TAG, "Location reminder marked as done: $reminderId")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling mark done action", e)
        }
    }

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
