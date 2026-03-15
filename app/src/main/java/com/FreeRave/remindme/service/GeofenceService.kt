package com.FreeRave.remindme.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.utils.GeofenceBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeofenceService"
private const val PREFS_NAME = "geofence_prefs"
private const val KEY_REGISTERED_GEOFENCES = "registered_geofences"
private const val MAX_GEOFENCES = 100 // Android's limit per app

@Singleton
class GeofenceService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        // Restore geofences on service initialization
        CoroutineScope(Dispatchers.IO).launch {
            restoreGeofences()
        }
    }

    // Persistence methods
    private fun saveRegisteredGeofences(geofenceIds: Set<String>) {
        val json = gson.toJson(geofenceIds)
        prefs.edit().putString(KEY_REGISTERED_GEOFENCES, json).apply()
    }

    private fun getRegisteredGeofences(): Set<String> {
        val json = prefs.getString(KEY_REGISTERED_GEOFENCES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, object : TypeToken<Set<String>>() {}.type)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse registered geofences", e)
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    private fun addRegisteredGeofence(geofenceId: String) {
        val current = getRegisteredGeofences().toMutableSet()
        current.add(geofenceId)
        saveRegisteredGeofences(current)
    }

    private fun removeRegisteredGeofence(geofenceId: String) {
        val current = getRegisteredGeofences().toMutableSet()
        current.remove(geofenceId)
        saveRegisteredGeofences(current)
    }

    private fun clearRegisteredGeofences() {
        prefs.edit().remove(KEY_REGISTERED_GEOFENCES).apply()
    }

    // Geofence management methods
    fun getActiveGeofenceCount(): Int = getRegisteredGeofences().size

    fun canRegisterMoreGeofences(count: Int = 1): Boolean {
        return getActiveGeofenceCount() + count <= MAX_GEOFENCES
    }

    /**
     * Attempts to make room for new geofences by removing lower priority ones if needed
     * Returns true if space is available (either was available or made available)
     */
    suspend fun ensureGeofenceCapacity(reminders: List<Reminder>): Boolean {
        val currentCount = getActiveGeofenceCount()
        val neededSpace = reminders.size

        // If we have enough space, return true
        if (currentCount + neededSpace <= MAX_GEOFENCES) {
            return true
        }

        // Calculate how many we need to remove
        val excess = (currentCount + neededSpace) - MAX_GEOFENCES
        if (excess <= 0) return true

        // Get all registered geofence IDs and their priorities
        val registeredIds = getRegisteredGeofences()
        val remindersToRemove = mutableListOf<String>()

        // Sort by priority (lowest first) and remove excess lowest priority geofences
        val sortedByPriority = reminders.sortedBy { it.geofencePriority }
        var removedCount = 0

        for (reminder in sortedByPriority) {
            if (removedCount >= excess) break
            if (registeredIds.contains(reminder.id)) {
                remindersToRemove.add(reminder.id)
                removedCount++
            }
        }

        // Remove the lowest priority geofences
        for (reminderId in remindersToRemove) {
            try {
                unregisterGeofence(reminderId)
                Log.d(TAG, "Removed low priority geofence $reminderId to make room for new ones")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove geofence $reminderId", e)
            }
        }

        // Check if we now have enough space
        return getActiveGeofenceCount() + neededSpace <= MAX_GEOFENCES
    }

    suspend fun restoreGeofences() {
        val registeredIds = getRegisteredGeofences()
        if (registeredIds.isNotEmpty()) {
            Log.d(TAG, "Restoring ${registeredIds.size} geofences: $registeredIds")
            // Note: In a real implementation, you'd need to fetch the reminder data
            // from your data source and re-register them. For now, we'll just log.
            // This would typically be called with actual reminder data.
        }
    }

    suspend fun registerGeofence(reminder: Reminder) {
        registerGeofences(listOf(reminder))
    }

    suspend fun registerGeofences(reminders: List<Reminder>) {
        val locationReminders = reminders.filter { isLocationBased(it) }
        if (locationReminders.isEmpty()) {
            Log.d(TAG, "No location-based reminders to register")
            return
        }

        // Try to ensure capacity by removing lower priority geofences if needed
        if (!ensureGeofenceCapacity(locationReminders)) {
            Log.e(TAG, "Cannot register ${locationReminders.size} geofences. Even after cleanup, limit is $MAX_GEOFENCES, currently have ${getActiveGeofenceCount()}")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Fine location permission not granted, cannot register geofences")
            return
        }

        // Check if GPS is enabled - if not, log and return without registering
        if (!com.FreeRave.remindme.utils.LocationUtils.isGpsEnabled(context)) {
            Log.w(TAG, "GPS/Location services not enabled, cannot register geofences for location-based reminders")
            return
        }

        try {
            val geofences = locationReminders.map { buildGeofence(it) }
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()

            // Create a unique pending intent for the batch - use a hash of all reminder IDs
            val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                locationReminders.joinToString(",").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            geofencingClient.addGeofences(geofencingRequest, pendingIntent).await()

            // Persist registered geofence IDs
            locationReminders.forEach { addRegisteredGeofence(it.id) }

            Log.d(TAG, "Registered ${locationReminders.size} geofences for reminders: ${locationReminders.map { it.id }}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofences for reminders: ${locationReminders.map { it.id }}", e)
        }
    }

    /**
     * Check if GPS is enabled and return appropriate status
     */
    fun checkGpsStatus(): GpsStatus {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return GpsStatus.NO_PERMISSION
        }
        return if (com.FreeRave.remindme.utils.LocationUtils.isGpsEnabled(context)) {
            GpsStatus.ENABLED
        } else {
            GpsStatus.DISABLED
        }
    }

    enum class GpsStatus {
        ENABLED,
        DISABLED,
        NO_PERMISSION
    }

    suspend fun unregisterGeofence(reminderId: String) {
        try {
            // Try to remove using individual pending intent first
            var intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            var pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                geofencingClient.removeGeofences(pendingIntent).await()
                removeRegisteredGeofence(reminderId)
                Log.d(TAG, "Geofence unregistered for reminder $reminderId")
                return
            }

            // If individual pending intent not found, try to remove from batch
            // This is a simplified approach - in production, you'd need better tracking
            Log.w(TAG, "Individual geofence not found for $reminderId, may be part of a batch")
            removeRegisteredGeofence(reminderId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister geofence for reminder $reminderId", e)
        }
    }

    suspend fun unregisterAllGeofences() {
        try {
            val registeredIds = getRegisteredGeofences()
            if (registeredIds.isNotEmpty()) {
                geofencingClient.removeGeofences(registeredIds.toList()).await() // Remove all geofences by IDs
            }
            clearRegisteredGeofences()
            Log.d(TAG, "All geofences unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister all geofences", e)
        }
    }

    suspend fun restoreGeofencesWithData(reminders: List<Reminder>) {
        val registeredIds = getRegisteredGeofences()
        if (registeredIds.isNotEmpty()) {
            val remindersToRestore = reminders.filter { it.id in registeredIds && isLocationBased(it) }
            if (remindersToRestore.isNotEmpty()) {
                Log.d(TAG, "Restoring ${remindersToRestore.size} geofences from ${reminders.size} available reminders")
                registerGeofences(remindersToRestore)
            }
        }
    }

    // Note: Removing all geofences requires storing geofence IDs or pending intents separately

    private fun buildGeofence(reminder: Reminder): Geofence {
        val transitionTypes = when (reminder.geofenceTransitionType) {
            1 -> Geofence.GEOFENCE_TRANSITION_ENTER
            2 -> Geofence.GEOFENCE_TRANSITION_EXIT
            3 -> Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            else -> Geofence.GEOFENCE_TRANSITION_ENTER
        }

        val builder = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude ?: 0.0,
                reminder.longitude ?: 0.0,
                reminder.geofenceRadius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)

        // Add dwell time if specified (converts milliseconds to seconds for API)
        reminder.geofenceDwellTimeMillis?.let { dwellTime ->
            if (dwellTime > 0) {
                // Add dwell transition type if not already present
                val dwellTransitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_DWELL
                builder.setTransitionTypes(dwellTransitionTypes)
                builder.setLoiteringDelay((dwellTime / 1000).toInt()) // Convert to seconds
            }
        }

        return builder.build()
    }

    private fun isLocationBased(reminder: Reminder): Boolean {
        return reminder.isLocationBased &&
               reminder.latitude != null &&
               reminder.longitude != null
    }
}
