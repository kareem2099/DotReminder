package com.FreeRave.remindme.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.FreeRave.remindme.MainActivity
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.ReminderService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class UpcomingRemindersWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var reminderService: ReminderService

    @Inject
    lateinit var authService: FirebaseAuthServiceInterface

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all instances of the widget
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when first widget instance is created
    }

    override fun onDisabled(context: Context) {
        // Called when last widget instance is removed
        coroutineScope.cancel()
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_upcoming_reminders)

        // Set up the header click to open main app
        val headerIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val headerPendingIntent = PendingIntent.getActivity(
            context,
            0,
            headerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header, headerPendingIntent)

        // Load upcoming reminders
        coroutineScope.launch {
            try {
                val userId = authService.getCurrentUserId()
                if (userId.isNotBlank()) {
                    val reminders = reminderService.getRemindersOnce(userId)
                    val upcomingReminders = reminders
                        .filter { it.reminderTime > System.currentTimeMillis() }
                        .sortedBy { it.reminderTime }
                        .take(3) // Show only next 3 reminders

                    withContext(Dispatchers.Main) {
                        updateWidgetWithReminders(context, views, upcomingReminders, appWidgetManager, appWidgetId)
                    }
                } else {
                    // No user logged in, show login prompt
                    views.setTextViewText(R.id.widget_title, "Please log in")
                    views.setTextViewText(R.id.reminder_1_title, "")
                    views.setTextViewText(R.id.reminder_1_time, "")
                    views.setTextViewText(R.id.reminder_2_title, "")
                    views.setTextViewText(R.id.reminder_2_time, "")
                    views.setTextViewText(R.id.reminder_3_title, "")
                    views.setTextViewText(R.id.reminder_3_time, "")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                // Show error state
                views.setTextViewText(R.id.widget_title, "Error loading reminders")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun updateWidgetWithReminders(
        context: Context,
        views: RemoteViews,
        reminders: List<Reminder>,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

        // Update reminder 1
        if (reminders.isNotEmpty()) {
            val reminder1 = reminders[0]
            views.setTextViewText(R.id.reminder_1_title, reminder1.title)
            views.setTextViewText(R.id.reminder_1_time, dateFormat.format(Date(reminder1.reminderTime)))

            // Set click intent for reminder 1
            val intent1 = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("navigate_to", "edit_reminder")
                putExtra("reminder_id", reminder1.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent1 = PendingIntent.getActivity(
                context,
                reminder1.id.hashCode(),
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.reminder_1_container, pendingIntent1)
        } else {
            views.setTextViewText(R.id.reminder_1_title, "No upcoming reminders")
            views.setTextViewText(R.id.reminder_1_time, "")
        }

        // Update reminder 2
        if (reminders.size > 1) {
            val reminder2 = reminders[1]
            views.setTextViewText(R.id.reminder_2_title, reminder2.title)
            views.setTextViewText(R.id.reminder_2_time, dateFormat.format(Date(reminder2.reminderTime)))

            val intent2 = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("navigate_to", "edit_reminder")
                putExtra("reminder_id", reminder2.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent2 = PendingIntent.getActivity(
                context,
                reminder2.id.hashCode(),
                intent2,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.reminder_2_container, pendingIntent2)
        } else {
            views.setTextViewText(R.id.reminder_2_title, "")
            views.setTextViewText(R.id.reminder_2_time, "")
        }

        // Update reminder 3
        if (reminders.size > 2) {
            val reminder3 = reminders[2]
            views.setTextViewText(R.id.reminder_3_title, reminder3.title)
            views.setTextViewText(R.id.reminder_3_time, dateFormat.format(Date(reminder3.reminderTime)))

            val intent3 = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("navigate_to", "edit_reminder")
                putExtra("reminder_id", reminder3.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent3 = PendingIntent.getActivity(
                context,
                reminder3.id.hashCode(),
                intent3,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.reminder_3_container, pendingIntent3)
        } else {
            views.setTextViewText(R.id.reminder_3_title, "")
            views.setTextViewText(R.id.reminder_3_time, "")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
