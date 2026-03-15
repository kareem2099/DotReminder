package com.FreeRave.remindme.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.service.GeofenceService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BootEventService"

@AndroidEntryPoint
class BootEventService : Service() {

    @Inject
    lateinit var reminderService: ReminderService

    @Inject
    lateinit var geofenceService: GeofenceService

    @Inject
    lateinit var authService: FirebaseAuthServiceInterface

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        serviceScope.launch {
            try {
                performBootTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error performing boot tasks", e)
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun performBootTasks() {
        try {
            val userId = authService.getCurrentUserId()
            if (userId.isNotBlank()) {
                val reminders = reminderService.getRemindersOnce(userId)
                val locationReminders = reminders.filter { reminder ->
                    reminder.isLocationBased &&
                            reminder.latitude != null &&
                            reminder.longitude != null
                }
                if (locationReminders.isNotEmpty()) {
                    Log.d(TAG, "Re-registering ${locationReminders.size} location-based geofences after boot")
                    geofenceService.registerGeofences(locationReminders)
                } else {
                    Log.d(TAG, "No location-based reminders found to re-register")
                }
            } else {
                Log.w(TAG, "User not authenticated, cannot re-register geofences")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error re-registering geofences on boot", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
