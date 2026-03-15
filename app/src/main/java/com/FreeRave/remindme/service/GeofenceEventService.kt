package com.FreeRave.remindme.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.maps.model.LatLng
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.ReminderService
import com.FreeRave.remindme.utils.LocationBasedNotificationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GeofenceEventService"

@AndroidEntryPoint
class GeofenceEventService : Service() {

    @Inject
    lateinit var authService: FirebaseAuthServiceInterface

    @Inject
    lateinit var reminderService: ReminderService

    @Inject
    lateinit var locationNotificationUtils: LocationBasedNotificationUtils

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with intent: $intent")

        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Log.e(TAG, "GeofencingEvent error: ${geofencingEvent.errorCode}")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val transition = geofencingEvent?.geofenceTransition ?: run {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                handleGeofenceTransition(geofencingEvent, transition)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition", e)
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun handleGeofenceTransition(geofencingEvent: GeofencingEvent, transition: Int) {
        for (triggeringGeofence in geofencingEvent.triggeringGeofences ?: emptyList()) {
            val geofenceId = triggeringGeofence.requestId

            // Geofence ID is the reminder ID
            val userId = authService.getCurrentUserId()
            if (userId.isBlank()) {
                Log.e(TAG, "No current user for geofence transition")
                continue
            }

            val reminder = reminderService.getReminder(geofenceId)
            if (reminder != null && reminder.isLocationBased) {
                // Check if transition matches the reminder's type
                val allowedTransition = when (reminder.geofenceTransitionType) {
                    1 -> Geofence.GEOFENCE_TRANSITION_ENTER
                    2 -> Geofence.GEOFENCE_TRANSITION_EXIT
                    3 -> TransitionType.BOTH.code
                    else -> null
                }

                val isAllowed = allowedTransition == transition ||
                        (reminder.geofenceTransitionType == TransitionType.BOTH.code && transition in listOf(
                            Geofence.GEOFENCE_TRANSITION_ENTER,
                            Geofence.GEOFENCE_TRANSITION_EXIT
                        ))

                if (isAllowed) {
                    // Get current location for distance calculation
                    val currentLocation = geofencingEvent.triggeringLocation?.let { location ->
                        LatLng(location.latitude, location.longitude)
                    }

                    val triggerType = when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> "enter"
                        Geofence.GEOFENCE_TRANSITION_EXIT -> "exit"
                        Geofence.GEOFENCE_TRANSITION_DWELL -> "dwell"
                        else -> "unknown"
                    }

                    // Use the enhanced notification system
                    locationNotificationUtils.showLocationBasedNotification(
                        reminder = reminder,
                        triggerType = triggerType,
                        currentLocation = currentLocation
                    )

                    Log.d(TAG, "Enhanced location notification sent for reminder: ${reminder.id}, trigger: $triggerType")
                } else {
                    Log.d(TAG, "Transition type not allowed for reminder ${reminder.id}")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private enum class TransitionType(val code: Int) {
        ENTER(1),
        EXIT(2),
        BOTH(3)
    }
}
