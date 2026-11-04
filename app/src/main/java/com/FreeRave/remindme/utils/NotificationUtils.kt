package com.FreeRave.remindme.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.FreeRave.remindme.MainActivity
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.reminders.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

object NotificationUtils {
    const val CHANNEL_ID = "reminder_alarm_channel" // Retained for existing system channel preferences.
    const val CHANNEL_NAME = "Reminder Alarms"
    const val NOTIFICATION_ID_KEY = "notification_id_key"
    const val REMINDER_ID_STRING_KEY = "reminder_id_string_key"
    const val REMINDER_TITLE_KEY = "reminder_title_key"
    const val REMINDER_MESSAGE_KEY = "reminder_message_key"
    const val REMINDER_ORIGINAL_TIME_KEY = "reminder_original_time_key"
    const val SNOOZE_DURATION_MINUTES_KEY = "snooze_duration_minutes_key"
    const val SOUND_RESOURCE_NAME_KEY = "sound_resource_name_key"
    const val ACTION_SHOW_ALARM = "com.FreeRave.remindmeapp.ACTION_SHOW_ALARM"
    const val ACTION_STOP_ALARM = "com.FreeRave.remindmeapp.ACTION_STOP_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.FreeRave.remindmeapp.ACTION_SNOOZE_ALARM"
    internal const val TRIGGER_KEY = "scheduled_trigger"
    internal val alarmLock = Any()

    fun createNotificationChannel(context: Context, soundResourceName: String, vibrationEnabled: Boolean): String {
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = normalizedSound(context, soundResourceName)
        val id = reminderChannelId(sound, vibrationEnabled)
        // Preserve a user's explicit block of the old reminder channel during migration.
        if (manager.getNotificationChannel(id) == null && manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE)
            return CHANNEL_ID
        val channel = NotificationChannel(id, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Scheduled reminders"
            setSound(soundUri(context, sound), AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_ALARM).build())
            enableVibration(vibrationEnabled)
            if (vibrationEnabled) vibrationPattern = longArrayOf(0, 500, 250, 500)
        }
        // Recreating the same ID leaves system/user choices intact. Never delete channels to reset them.
        manager.createNotificationChannel(channel)
        return id
    }

    fun canSchedulePrecisely(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun blockedChannel(context: Context, sound: String, vibration: Boolean): String? {
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = reminderChannelId(normalizedSound(context, sound), vibration)
        val channel = manager.getNotificationChannel(id) ?: manager.getNotificationChannel(CHANNEL_ID)
        return channel?.takeIf { it.importance == NotificationManager.IMPORTANCE_NONE }?.id
    }

    fun notificationsAllowed(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun scheduleNotification(context: Context, reminder: Reminder, snoozeMinutes: Int, soundResourceName: String,
                             allowDueNow: Boolean = false) = synchronized(alarmLock) {
        if (reminder.id.isBlank() || reminder.userId.isBlank()) return@synchronized
        val store = ReminderAlarmStore(context)
        val previous = store.get(reminder.id)
        val now = System.currentTimeMillis()
        val retained = previous?.takeIf { it.sameTiming(reminder) }
        val recentUndelivered = retained?.let { !it.delivered && now - it.triggerAt in 0..60_000 } == true
        val preserved = retained?.takeIf { it.triggerAt > now || it.delivered && !reminder.isRecurring || recentUndelivered }
        val trigger = preserved?.triggerAt ?: initialReminderTrigger(reminder.reminderTime,
            reminder.recurrenceIntervalMillis.takeIf { reminder.isRecurring }, reminder.recurrenceEndTime, now, allowDueNow)
            ?: reminder.reminderTime
        val record = ReminderAlarm(reminder, trigger, snoozeMinutes.coerceIn(1, 1440), normalizedSound(context, soundResourceName),
            delivered = retained?.delivered == true && preserved != null,
            lastDeliveryDelayMillis = retained?.lastDeliveryDelayMillis)
        store.put(record)
        if ((record.triggerAt > now || recentUndelivered) && !record.delivered) arm(context, record)
        else cancelPendingAlarm(context, reminder.id)
    }

    internal fun arm(context: Context, record: ReminderAlarm) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = alarmIntent(context, record.reminder.id, ACTION_SHOW_ALARM).putExtra(TRIGGER_KEY, record.triggerAt)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (canSchedulePrecisely(context)) {
                val show = PendingIntent.getActivity(context, 0,
                    Intent(context, MainActivity::class.java).apply {
                        data = Uri.Builder().scheme("dotreminder").authority("upcoming-alarm").appendPath(record.reminder.id).build()
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(record.triggerAt, show), pending)
            }
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, record.triggerAt, pending)
        } catch (error: SecurityException) {
            Log.w("ReminderAlarm", "Exact alarm access was rejected; using approximate delivery", error)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, record.triggerAt, pending)
        }
    }

    internal fun alarmIntent(context: Context, id: String, action: String) = Intent(context, NotificationReceiver::class.java).apply {
        this.action = action
        data = Uri.Builder().scheme("dotreminder").authority("reminder-alarm").appendPath(id).build()
        putExtra(REMINDER_ID_STRING_KEY, id)
    }
    internal fun notificationTag(id: String) = "reminder:$id"
    internal fun currentUserId(): String? = try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: IllegalStateException) { null }

    internal fun cancelPendingAlarm(context: Context, id: String) {
        val manager = context.getSystemService(AlarmManager::class.java)
        PendingIntent.getBroadcast(context, 0, alarmIntent(context, id, ACTION_SHOW_ALARM),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { manager.cancel(it); it.cancel() }
        // Clean up alarms scheduled by older builds using only hashCode request IDs.
        PendingIntent.getBroadcast(context, id.hashCode(), Intent(context, NotificationReceiver::class.java).setAction(ACTION_SHOW_ALARM),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { manager.cancel(it); it.cancel() }
    }

    fun cancelNotification(context: Context, reminderId: String) = synchronized(alarmLock) {
        cancelPendingAlarm(context, reminderId)
        ReminderAlarmStore(context).remove(reminderId)
        dismiss(context, reminderId)
    }
    internal fun dismiss(context: Context, id: String) {
        context.getSystemService(NotificationManager::class.java).apply {
            cancel(notificationTag(id), 0)
            cancel(id.hashCode()) // Legacy displayed notification.
        }
    }

    fun restoreNotifications(context: Context) = synchronized(alarmLock) {
        val uid = currentUserId()
        val store = ReminderAlarmStore(context)
        val now = System.currentTimeMillis()
        store.all().forEach { record ->
            if (record.reminder.userId != uid) { cancelPendingAlarm(context, record.reminder.id); dismiss(context, record.reminder.id) }
            else if (!record.delivered && (record.triggerAt > now || now - record.triggerAt in 0..60_000)) arm(context, record)
            else if (record.reminder.isRecurring) {
                val next = nextReminderOccurrence(record.reminder.reminderTime, record.reminder.recurrenceIntervalMillis,
                    record.reminder.recurrenceEndTime, now)
                if (next != null) record.copy(triggerAt = next, delivered = false).let { store.put(it); arm(context, it) }
            }
        }
    }

    suspend fun reconcileNotifications(context: Context, reminders: List<Reminder>, uid: String) {
        val preferences = UserPreferencesRepository(context.applicationContext)
        val snooze = preferences.defaultSnoozeMinutes.first()
        val sound = preferences.selectedNotificationSound.first() ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME
        synchronized(alarmLock) {
            if (currentUserId() != uid) return
            val ids = reminders.map { it.id }.toSet()
            ReminderAlarmStore(context).all().filter { it.reminder.userId == uid && it.reminder.id !in ids }
                .forEach { cancelNotification(context, it.reminder.id) }
            reminders.forEach { scheduleNotification(context, it, snooze, sound) }
        }
    }

    internal fun normalizedSound(context: Context, name: String): String =
        name.takeIf { it.matches(Regex("[a-z][a-z0-9_]{0,60}")) && context.resources.getIdentifier(it, "raw", context.packageName) != 0 }.orEmpty()
    internal fun soundUri(context: Context, name: String): Uri =
        if (name.isEmpty()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else Uri.parse("android.resource://${context.packageName}/raw/$name")
}
