package com.FreeRave.remindme.screens.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.Session
import com.FreeRave.remindme.screens.settings.components.SessionRemovalDialog
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import com.google.firebase.Timestamp

@Preview(name = "Sessions · Light", widthDp = 393, heightDp = 852)
@Composable
private fun SessionsLightPreview() = SessionsPreview(false)

@Preview(name = "Sessions · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun SessionsDarkPreview() = SessionsPreview(true)

@Preview(name = "Sessions · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun SessionsCompactPreview() = SessionsPreview(false)

@Preview(name = "Sessions · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun SessionsTabletPreview() = SessionsPreview(false)

@Preview(name = "Sessions · Loading", widthDp = 393, heightDp = 852)
@Composable
private fun SessionsLoadingPreview() = SessionsPreview(false, status = "loading")

@Preview(name = "Sessions · Error", widthDp = 393, heightDp = 852)
@Composable
private fun SessionsErrorPreview() = SessionsPreview(false, status = "error")

@Preview(name = "Sessions · Empty", widthDp = 393, heightDp = 852)
@Composable
private fun SessionsEmptyPreview() = SessionsPreview(false, status = "empty")

@Preview(name = "Sessions · Identity missing", widthDp = 393, heightDp = 852)
@Composable
private fun SessionsMissingIdentityPreview() = SessionsPreview(false, status = "identity")

@Preview(name = "Sessions · Confirmation failure", widthDp = 393, heightDp = 852)
@Composable
private fun SessionsConfirmationPreview() = SessionsPreview(true, status = "confirmation")

@Composable
private fun SessionsPreview(dark: Boolean, status: String = "ready") {
    val time = Timestamp(1_790_000_000, 0)
    val records = listOf(
        Session("this", "OnePlus IN2025", "IN2025", time, time),
        Session("tablet", "Samsung Galaxy Tab · A long name that wraps naturally", "Galaxy Tab", time, time),
        Session("phone", "OnePlus IN2025", "IN2025", time, time))
    val state = ActiveSessionsUiState(
        sessions = if (status in listOf("loading", "empty", "error")) emptyList() else records,
        currentSessionId = if (status == "identity") null else "this", identityLoaded = true,
        isLoading = status == "loading", hasError = status == "error",
        removal = if (status == "confirmation") SessionRemoval.One(records[1]) else null, removalFailed = status == "confirmation")
    val snackbar = remember { SnackbarHostState() }
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            ActiveSessionsScreenContent(state, snackbar, onBack = {}, onRetry = {}, onRemove = {}, onClearOthers = {})
            state.removal?.let { SessionRemovalDialog(it, busy = false, failed = state.removalFailed,
                canConfirm = state.canConfirmRemoval(), onConfirm = {}, onDismiss = {}) }
        }
    }
}
