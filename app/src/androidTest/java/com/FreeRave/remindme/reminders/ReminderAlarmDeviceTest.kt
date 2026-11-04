package com.FreeRave.remindme.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioAttributes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.screens.auth.LoginUiReviewRunner
import com.FreeRave.remindme.utils.NotificationUtils
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Local synthetic alarms only. No Firebase writes, real reminder edits or permission changes. */
@RunWith(AndroidJUnit4::class)
class ReminderAlarmDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun isolatedRunnerOnly() {
        assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner)
        ReminderAlarmStore(context).all().filter { it.reminder.id.startsWith("review-") }
            .forEach { NotificationUtils.cancelNotification(context, it.reminder.id) }
    }

    @Test fun deviceLocalRecordsPreserveRepeatAndSnoozeDetails() {
        val id = "review-store-${UUID.randomUUID()}"
        val store = ReminderAlarmStore(context)
        val reminder = Reminder(id = id, userId = "synthetic-owner", title = "Synthetic alarm",
            reminderTime = 1_800_000_000_000, isRecurring = true, recurrenceIntervalMillis = 60_000,
            recurrenceEndTime = 1_800_001_000_000)
        try {
            store.put(ReminderAlarm(reminder, 1_800_000_060_000, 10, "alarm_sound_1"))
            val restored = ReminderAlarmStore(context).get(id)!!
            assertEquals(reminder.id, restored.reminder.id)
            assertEquals(reminder.userId, restored.reminder.userId)
            assertEquals(reminder.recurrenceIntervalMillis, restored.reminder.recurrenceIntervalMillis)
            assertEquals(reminder.recurrenceEndTime, restored.reminder.recurrenceEndTime)
            assertEquals(1_800_000_060_000, restored.triggerAt)
            assertEquals(10, restored.snoozeMinutes)
        } finally { store.remove(id) }
    }

    @Test fun collidingStringHashesDoNotReplaceEachOthersPendingAlarms() {
        val suffix = UUID.randomUUID().toString()
        val first = "review-Aa-$suffix"
        val second = "review-BB-$suffix"
        assertEquals(first.hashCode(), second.hashCode())
        try {
            val a = PendingIntent.getBroadcast(context, 0, NotificationUtils.alarmIntent(context, first, NotificationUtils.ACTION_SHOW_ALARM), PendingIntent.FLAG_IMMUTABLE)
            val b = PendingIntent.getBroadcast(context, 0, NotificationUtils.alarmIntent(context, second, NotificationUtils.ACTION_SHOW_ALARM), PendingIntent.FLAG_IMMUTABLE)
            assertNotEquals(a, b)
        } finally {
            NotificationUtils.cancelNotification(context, first)
            NotificationUtils.cancelNotification(context, second)
        }
    }

    @Test fun realAlarmsSurviveLocalRestorationDeliverWithSoundAndSupportRepeatAndSnooze() {
        assertTrue("Device owner must allow reminder notifications first", NotificationUtils.notificationsAllowed(context))
        assertTrue("Device owner must allow precise reminders first", NotificationUtils.canSchedulePrecisely(context))
        val uid = requireNotNull(NotificationUtils.currentUserId()) { "Keep the existing account signed in" }
        val suffix = UUID.randomUUID().toString()
        val first = "review-Aa-$suffix"
        val second = "review-BB-$suffix"
        val store = ReminderAlarmStore(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val start = System.currentTimeMillis() + 15_000
        try {
            NotificationUtils.scheduleNotification(context,
                Reminder(id = first, userId = uid, title = "DotReminder · sound review", reminderTime = start,
                    isRecurring = true, recurrenceIntervalMillis = 60_000), 1, "alarm_sound_1")
            NotificationUtils.scheduleNotification(context,
                Reminder(id = second, userId = uid, title = "DotReminder · restore review", reminderTime = start), 1, "alarm_sound_1")
            // Simulate Android clearing its alarm registrations; keep the disk records, then restore them.
            NotificationUtils.cancelPendingAlarm(context, second)
            NotificationUtils.restoreNotifications(context)
            waitFor { listOf(first, second).all { id -> manager.activeNotifications.any { it.tag == NotificationUtils.notificationTag(id) } } }
            val posted = manager.activeNotifications.first { it.tag == NotificationUtils.notificationTag(first) }
            assertTrue("An exact reminder should not be batched seconds after its time", posted.postTime - start < 2_000)
            InstrumentationRegistry.getInstrumentation().sendStatus(0, android.os.Bundle().apply {
                putString("firstAlarmDelayMs", (posted.postTime - start).toString())
                putString("restoredAlarmDelayMs", (manager.activeNotifications.first {
                    it.tag == NotificationUtils.notificationTag(second)
                }.postTime - start).toString())
            })
            val channel = manager.getNotificationChannel(posted.notification.channelId)
            assertNotNull(channel.sound)
            assertEquals(AudioAttributes.USAGE_ALARM, channel.audioAttributes.usage)
            assertTrue(channel.importance >= NotificationManager.IMPORTANCE_DEFAULT)
            if (channel.sound.scheme == "android.resource") {
                context.contentResolver.openAssetFileDescriptor(channel.sound, "r").use { assertNotNull(it) }
                assertTrue(channel.sound.path.orEmpty().startsWith("/raw/"))
            }
            waitFor { store.get(first)?.triggerAt?.let { it > start } == true }
            assertEquals(start + 60_000, store.get(first)!!.triggerAt)
            // Exercise the actual notification action PendingIntent.
            posted.notification.actions[1].actionIntent.send()
            waitFor { manager.activeNotifications.none { it.tag == NotificationUtils.notificationTag(first) } }
            assertTrue(store.get(first)!!.triggerAt > System.currentTimeMillis() + 45_000)
        } finally {
            NotificationUtils.cancelNotification(context, first)
            NotificationUtils.cancelNotification(context, second)
        }
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 45_000_000_000L
        while (!condition()) {
            if (System.nanoTime() > deadline) fail("Alarm delivery did not complete within 45 seconds")
            Thread.sleep(100)
        }
    }
}
