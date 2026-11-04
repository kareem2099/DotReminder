package com.FreeRave.remindme.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.FreeRave.remindme.utils.NotificationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/** Boot broadcasts cannot start ordinary background services on current Android releases. */
@AndroidEntryPoint
class BootEventService : JobService() {
    @Inject lateinit var reminderService: ReminderService
    @Inject lateinit var geofenceService: GeofenceService
    @Inject lateinit var authService: FirebaseAuthServiceInterface
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        work = scope.launch {
            try {
                val uid = authService.getCurrentUserId()
                if (uid.isNotBlank()) {
                    val reminders = reminderService.getRemindersOnce(uid)
                    if (authService.getCurrentUserId() == uid) {
                        // An unavailable server can return no data; device-local alarms remain intact.
                        if (reminders.isNotEmpty()) NotificationUtils.reconcileNotifications(applicationContext, reminders, uid)
                        geofenceService.registerGeofences(reminders.filter {
                            it.isLocationBased && it.latitude != null && it.longitude != null
                        })
                    }
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { Log.e("BootEventService", "Could not restore reminder registrations", error) }
            finally { if (currentCoroutineContext().isActive) jobFinished(params, false) }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { work?.cancel(); return true }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        fun enqueue(context: Context) {
            val info = JobInfo.Builder(312005, ComponentName(context, BootEventService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build()
            context.getSystemService(JobScheduler::class.java).schedule(info)
        }
    }
}
