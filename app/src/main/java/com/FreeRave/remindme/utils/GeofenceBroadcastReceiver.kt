package com.FreeRave.remindme.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent
import com.FreeRave.remindme.service.GeofenceEventService

private const val TAG = "GeofenceBroadcastReceiver"

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Log.e(TAG, "GeofencingEvent error: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent?.geofenceTransition ?: return

        if (geofencingEvent.triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "No triggering geofences")
            return
        }

        // Start service to handle the event
        val serviceIntent = Intent(context, GeofenceEventService::class.java).apply {
            putExtras(intent) // Pass the original intent containing geofence data
        }
        context.startService(serviceIntent)

        Log.d(TAG, "Started GeofenceEventService for geofence transition: $transition")
    }
}
