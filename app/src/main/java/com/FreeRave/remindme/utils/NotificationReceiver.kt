package com.FreeRave.remindme.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.FreeRave.remindme.MainActivity
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.reminders.ReminderAlarm
import com.FreeRave.remindme.reminders.ReminderAlarmStore
import com.FreeRave.remindme.reminders.nextReminderOccurrence
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(NotificationUtils.REMINDER_ID_STRING_KEY) ?: return
        if (intent.action == NotificationUtils.ACTION_STOP_ALARM) { NotificationUtils.dismiss(context, id); return }
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8_000) { handle(context.applicationContext, intent, id) } }
            catch (error: Exception) { Log.e("ReminderAlarm", "Could not deliver reminder alert", error) }
            finally { pending.finish() }
        }
    }

    private suspend fun handle(context: Context, intent: Intent, id: String) {
        var sound: String? = null
        var vibration = UserPreferencesRepository.DEFAULT_VIBRATION_ENABLED
        if (intent.action == NotificationUtils.ACTION_SHOW_ALARM) {
            try {
                withTimeoutOrNull(1_500) {
                    val preferences = UserPreferencesRepository(context)
                    sound = preferences.selectedNotificationSound.first()
                    vibration = preferences.isVibrationEnabled.first()
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { /* Use the saved alarm's sound if preferences cannot be read. */ }
        }
        synchronized(NotificationUtils.alarmLock) { handleLocked(context, intent, id, sound, vibration) }
    }

    private fun handleLocked(context: Context, intent: Intent, id: String, sound: String?, vibration: Boolean) {
        val store = ReminderAlarmStore(context)
        val record = store.get(id) ?: return
        if (record.reminder.userId != NotificationUtils.currentUserId()) {
            NotificationUtils.cancelPendingAlarm(context, id)
            NotificationUtils.dismiss(context, id)
            return
        }
        val now = System.currentTimeMillis()
        when (intent.action) {
            NotificationUtils.ACTION_SNOOZE_ALARM -> {
                NotificationUtils.dismiss(context, id)
                val snoozed = record.copy(triggerAt = now + record.snoozeMinutes * 60_000L, delivered = false)
                store.put(snoozed)
                NotificationUtils.arm(context, snoozed)
            }
            NotificationUtils.ACTION_SHOW_ALARM -> {
                if (record.delivered || record.triggerAt > now + 1_000 ||
                    intent.getLongExtra(NotificationUtils.TRIGGER_KEY, -1) != record.triggerAt) return
                val channel = NotificationUtils.createNotificationChannel(context, sound ?: record.sound, vibration)
                val posted = NotificationUtils.notificationsAllowed(context) &&
                    context.getSystemService(NotificationManager::class.java).getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE
                if (posted) show(context, record, channel)
                val deliveryDelay = if (posted) System.currentTimeMillis() - record.triggerAt else record.lastDeliveryDelayMillis
                val next = if (record.reminder.isRecurring) nextReminderOccurrence(record.reminder.reminderTime,
                    record.reminder.recurrenceIntervalMillis, record.reminder.recurrenceEndTime, maxOf(now, record.triggerAt)) else null
                if (next != null) record.copy(triggerAt = next, delivered = false, lastDeliveryDelayMillis = deliveryDelay)
                    .let { store.put(it); NotificationUtils.arm(context, it) }
                else store.put(record.copy(delivered = posted, lastDeliveryDelayMillis = deliveryDelay))
            }
        }
    }

    private fun show(context: Context, record: ReminderAlarm, channel: String) {
        val id = record.reminder.id
        fun action(action: String) = PendingIntent.getBroadcast(context, 0, NotificationUtils.alarmIntent(context, id, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(record.reminder.title)
            .setContentText(record.reminder.description.ifBlank { context.getString(R.string.reminder_alert_due) })
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(content).setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_pause, context.getString(R.string.reminder_alert_dismiss), action(NotificationUtils.ACTION_STOP_ALARM))
            .addAction(android.R.drawable.ic_popup_reminder, context.getString(R.string.reminder_alert_snooze, record.snoozeMinutes), action(NotificationUtils.ACTION_SNOOZE_ALARM))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NotificationUtils.notificationTag(id), 0, notification)
    }
}
