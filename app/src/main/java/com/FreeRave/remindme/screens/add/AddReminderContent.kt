package com.FreeRave.remindme.screens.add

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.ui.reminder.ReminderDraft
import com.FreeRave.remindme.ui.reminder.ReminderFormContent
import com.FreeRave.remindme.ui.reminder.ReminderFormError
import com.FreeRave.remindme.ui.reminder.ReminderFormEvent
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Composable
internal fun AddReminderContent(
    draft: ReminderDraft,
    categories: List<Category>,
    errors: Set<ReminderFormError>,
    validationRequest: Int,
    isSaving: Boolean,
    isParsing: Boolean,
    snackbar: SnackbarHostState,
    onEvent: (ReminderFormEvent) -> Unit
) = ReminderFormContent(draft, categories, errors, validationRequest, isSaving, isParsing, snackbar, onEvent)

@Preview(name = "Add · Light", widthDp = 393, heightDp = 852)
@Composable
private fun AddReminderLightPreview() = AddReminderPreview(false)

@Preview(name = "Add · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun AddReminderDarkPreview() = AddReminderPreview(true)

@Preview(name = "Add · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun AddReminderCompactPreview() = AddReminderPreview(false)

@Preview(name = "Add · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun AddReminderTabletPreview() = AddReminderPreview(false)

@Composable
private fun AddReminderPreview(dark: Boolean) {
    RemindMeAppTheme(themeSetting = if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            AddReminderContent(ReminderDraft(reminderTimeMillis = 1_800_000_000_000L), emptyList(), emptySet(),
                validationRequest = 0, isSaving = false, isParsing = false, snackbar = SnackbarHostState(), onEvent = {})
        }
    }
}
