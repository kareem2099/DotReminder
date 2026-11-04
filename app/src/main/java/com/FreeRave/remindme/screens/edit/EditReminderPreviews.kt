package com.FreeRave.remindme.screens.edit

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.ui.reminder.DAY_MILLIS
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.ReminderFormContent
import com.FreeRave.remindme.ui.reminder.ReminderFormMode
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Preview(name = "Edit · Light", widthDp = 393, heightDp = 852)
@Composable
private fun EditReminderLightPreview() = EditReminderPreview(false)

@Preview(name = "Edit · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun EditReminderDarkPreview() = EditReminderPreview(true)

@Preview(name = "Edit · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun EditReminderCompactPreview() = EditReminderPreview(false)

@Preview(name = "Edit · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun EditReminderTabletPreview() = EditReminderPreview(false)

@Preview(name = "Edit · Loading", widthDp = 393, heightDp = 852)
@Composable
private fun EditReminderLoadingPreview() = EditReminderPreview(true, loading = true)

@Preview(name = "Edit · Load error", widthDp = 393, heightDp = 852)
@Composable
private fun EditReminderErrorPreview() = EditReminderPreview(false, failed = true)

@Composable
private fun EditReminderPreview(dark: Boolean, loading: Boolean = false, failed: Boolean = false) {
    val snackbar = remember { SnackbarHostState() }
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            if (loading || failed) EditReminderLoadingContent(loading, snackbar, onBack = {}, onRetry = {})
            else ReminderFormContent(
                draft = ReminderDraft(title = "A little time to reset", description = "Step outside. Clear your mind.",
                    category = "Personal", reminderTimeMillis = 1_900_000_000_000L,
                    isRecurring = true, recurrenceIntervalMillis = DAY_MILLIS),
                categories = emptyList(), errors = emptySet(), validationRequest = 0,
                isSaving = false, isParsing = false, snackbar = snackbar, onEvent = {},
                mode = ReminderFormMode.EDIT, referenceId = "a17b8c9d", saveEnabled = true
            )
        }
    }
}
