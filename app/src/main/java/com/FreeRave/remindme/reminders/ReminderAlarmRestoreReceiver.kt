package com.FreeRave.remindme.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.FreeRave.remindme.utils.NotificationUtils
import kotlinx.coroutines.*

class ReminderAlarmRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { NotificationUtils.restoreNotifications(context.applicationContext) }
            catch (error: Exception) { Log.e("ReminderAlarm", "Could not restore scheduled reminders", error) }
            finally { pending.finish() }
        }
    }
}
