package com.FreeRave.remindme.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.FreeRave.remindme.service.BootEventService

private const val TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d(TAG, "Device boot completed, starting service to re-register geofences")

        // Start service to handle boot tasks
        BootEventService.enqueue(context)
    }
}
