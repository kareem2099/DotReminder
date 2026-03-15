package com.FreeRave.remindme.screens.settings.components

import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.FreeRave.remindme.data.UserPreferencesRepository
import java.util.Calendar

@Composable
fun DefaultTimePickerDialog(
    showDialog: Boolean,
    defaultHour: Int,
    defaultMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    if (showDialog) {
        val calendar = Calendar.getInstance()
        val initialHour = if (defaultHour != UserPreferencesRepository.NO_DEFAULT_TIME_SET) defaultHour else calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = if (defaultMinute != UserPreferencesRepository.NO_DEFAULT_TIME_SET) defaultMinute else calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                onTimeSelected(hour, minute)
            },
            initialHour,
            initialMinute,
            false // Use 12 or 24 hour format based on system settings? For now, false = AM/PM.
        ).apply {
            setOnDismissListener { onDismiss() }
            show()
        }
    }
}
