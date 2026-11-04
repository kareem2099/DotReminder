package com.FreeRave.remindme.reminders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.utils.NotificationUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal enum class DeliveryRequirement { NOTIFICATIONS, CHANNEL, EXACT_ALARM, READY }
internal fun deliveryRequirement(notifications: Boolean, channel: Boolean, precise: Boolean) = when {
    !notifications -> DeliveryRequirement.NOTIFICATIONS
    !channel -> DeliveryRequirement.CHANNEL
    !precise -> DeliveryRequirement.EXACT_ALARM
    else -> DeliveryRequirement.READY
}

/** Saves only after the owner has explicitly allowed the capabilities needed to deliver their reminder. */
@Composable
internal fun rememberReminderDeliveryAccess(onReady: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val latestReady by rememberUpdatedState(onReady)
    var pending by rememberSaveable { mutableStateOf(false) }
    var waitingForSettings by rememberSaveable { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var requirement by rememberSaveable { mutableStateOf<DeliveryRequirement?>(null) }
    var blockedChannel by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsError by remember { mutableStateOf(false) }

    fun check() {
        scope.launch {
            try {
                val prefs = UserPreferencesRepository(context.applicationContext)
                val sound = prefs.selectedNotificationSound.first() ?: UserPreferencesRepository.DEFAULT_SOUND_RESOURCE_NAME
                val vibration = prefs.isVibrationEnabled.first()
                NotificationUtils.createNotificationChannel(context, sound, vibration)
                blockedChannel = NotificationUtils.blockedChannel(context, sound, vibration)
                val next = deliveryRequirement(NotificationUtils.notificationsAllowed(context), blockedChannel == null,
                    NotificationUtils.canSchedulePrecisely(context))
                if (!pending) return@launch
                if (next == DeliveryRequirement.READY) {
                    pending = false; requirement = null
                    latestReady()
                } else requirement = next
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { settingsError = true; requirement = DeliveryRequirement.NOTIFICATIONS }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) check() else requirement = DeliveryRequirement.NOTIFICATIONS
    }
    val latestCheck by rememberUpdatedState(::check)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pending && waitingForSettings) {
                waitingForSettings = false; latestCheck()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    requirement?.let { current ->
        val canRequest = current == DeliveryRequirement.NOTIFICATIONS && Build.VERSION.SDK_INT >= 33 && !asked &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val cancel = { pending = false; waitingForSettings = false; requirement = null; settingsError = false }
        AlertDialog(onDismissRequest = cancel,
            title = { Text(stringResource(if (current == DeliveryRequirement.EXACT_ALARM) R.string.reminder_alert_exact_title else R.string.reminder_alert_access_title)) },
            text = { Text(stringResource(when {
                settingsError -> R.string.reminder_alert_settings_error
                current == DeliveryRequirement.EXACT_ALARM -> R.string.reminder_alert_exact_intro
                current == DeliveryRequirement.CHANNEL -> R.string.reminder_alert_channel_intro
                else -> R.string.reminder_alert_access_intro
            })) },
            confirmButton = {
                TextButton(onClick = {
                    settingsError = false
                    if (canRequest) { asked = true; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    else {
                        val intent = when (current) {
                            DeliveryRequirement.EXACT_ALARM -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                            DeliveryRequirement.CHANNEL -> Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, blockedChannel)
                            else -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        try { waitingForSettings = true; context.startActivity(intent) }
                        catch (_: RuntimeException) { waitingForSettings = false; settingsError = true }
                    }
                }) { Text(stringResource(if (canRequest) R.string.reminder_alert_allow else R.string.reminder_alert_settings)) }
            },
            dismissButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.reminder_alert_cancel)) } })
    }
    return { if (!pending) { pending = true; check() } }
}
