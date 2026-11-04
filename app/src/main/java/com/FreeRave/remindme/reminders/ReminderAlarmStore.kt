package com.FreeRave.remindme.reminders

import android.content.Context
import com.FreeRave.remindme.model.Reminder
import org.json.JSONObject

internal data class ReminderAlarm(
    val reminder: Reminder, val triggerAt: Long, val snoozeMinutes: Int,
    val sound: String, val delivered: Boolean = false, val lastDeliveryDelayMillis: Long? = null
) {
    fun sameTiming(other: Reminder) = reminder.userId == other.userId && reminder.reminderTime == other.reminderTime &&
        reminder.isRecurring == other.isRecurring && reminder.recurrenceIntervalMillis == other.recurrenceIntervalMillis &&
        reminder.recurrenceEndTime == other.recurrenceEndTime
}

/** Private, device-local alarm records allow boot recovery without waiting for Firestore or connectivity. */
internal class ReminderAlarmStore(context: Context) {
    private val preferences = context.getSharedPreferences("reminder_alarm_records", Context.MODE_PRIVATE)
    fun all(): List<ReminderAlarm> = synchronized(lock) { preferences.all.values.mapNotNull { decode(it as? String) } }
    fun get(id: String): ReminderAlarm? = synchronized(lock) { decode(preferences.getString(id, null)) }
    fun remove(id: String) = synchronized(lock) { preferences.edit().remove(id).apply() }
    fun put(alarm: ReminderAlarm) = synchronized(lock) {
        val r = alarm.reminder
        val json = JSONObject().apply {
            put("id", r.id); put("uid", r.userId); put("title", r.title); put("description", r.description)
            put("start", r.reminderTime); put("recurring", r.isRecurring)
            put("interval", r.recurrenceIntervalMillis); put("end", r.recurrenceEndTime)
            put("trigger", alarm.triggerAt); put("snooze", alarm.snoozeMinutes)
            put("sound", alarm.sound); put("delivered", alarm.delivered)
            put("lastDeliveryDelayMillis", alarm.lastDeliveryDelayMillis)
        }
        preferences.edit().putString(r.id, json.toString()).apply()
    }
    private fun decode(value: String?): ReminderAlarm? = try {
        value?.let {
            val json = JSONObject(it)
            val reminder = Reminder(id = json.getString("id"), userId = json.getString("uid"),
                title = json.getString("title"), description = json.optString("description"),
                reminderTime = json.getLong("start"), isRecurring = json.optBoolean("recurring"),
                recurrenceIntervalMillis = if (json.isNull("interval")) null else json.getLong("interval"),
                recurrenceEndTime = if (json.isNull("end")) null else json.getLong("end"))
            ReminderAlarm(reminder, json.getLong("trigger"), json.optInt("snooze", 5).coerceIn(1, 1440),
                json.optString("sound"), json.optBoolean("delivered"),
                if (json.isNull("lastDeliveryDelayMillis")) null else json.getLong("lastDeliveryDelayMillis"))
        }
    } catch (_: Exception) { null }

    companion object { private val lock = Any() }
}
