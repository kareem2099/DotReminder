package com.FreeRave.remindme.screens.templates

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.ReminderTemplate
import com.FreeRave.remindme.screens.templates.components.TemplateEditorSheet
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Preview(name = "Templates · Light", widthDp = 393, heightDp = 852)
@Composable
private fun TemplatesLightPreview() = TemplatesPreview(false)
@Preview(name = "Templates · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun TemplatesDarkPreview() = TemplatesPreview(true)
@Preview(name = "Templates · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun TemplatesCompactPreview() = TemplatesPreview(false)
@Preview(name = "Templates · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun TemplatesTabletPreview() = TemplatesPreview(false)
@Preview(name = "Templates · Empty", widthDp = 393, heightDp = 852)
@Composable
private fun TemplatesEmptyPreview() = TemplatesPreview(false, "empty")
@Preview(name = "Templates · Error", widthDp = 393, heightDp = 852)
@Composable
private fun TemplatesErrorPreview() = TemplatesPreview(false, "error")
@Preview(name = "Templates · Editor", widthDp = 393, heightDp = 852)
@Composable
private fun TemplatesEditorPreview() = TemplatesPreview(true, "editor")

@Composable
private fun TemplatesPreview(dark: Boolean, mode: String = "ready") {
    val items = listOf(
        ReminderTemplate(id = "one", userId = "preview", name = "Morning routine", title = "Water the plants",
            description = "A little care for the balcony garden", category = "Personal", isRecurring = true, recurrenceIntervalMillis = 86_400_000),
        ReminderTemplate(id = "two", userId = "preview", name = "Before leaving home", title = "Bring my notebook", isLocationBased = true))
    val state = TemplatesUiState(templates = if (mode in listOf("empty", "error")) emptyList() else items,
        isLoading = false, hasError = mode == "error", isAuthenticated = true)
    val snackbar = remember { SnackbarHostState() }
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            TemplatesScreenContent(state, snackbar, onBack = {}, onCreate = {}, onUse = {}, onEdit = {}, onDelete = {},
                onQuery = {}, onFilter = {}, onRetry = {}, onReset = {})
            if (mode == "editor") TemplateEditorSheet(TemplateEditorState(original = items[0]), emptyList(), canSave = true,
                onChange = {}, onSave = {}, onDismiss = {})
        }
    }
}
