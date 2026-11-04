package com.FreeRave.remindme.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

private val signOutPreviewState = SignOutUiState(SignOutAccount("Kareem", "user@example.com"))
@Preview(name = "Sign out · Light", widthDp = 393, heightDp = 852)
@Composable private fun SignOutLightPreview() = SignOutPreview(false)
@Preview(name = "Sign out · Dark", widthDp = 393, heightDp = 852)
@Composable private fun SignOutDarkPreview() = SignOutPreview(true)
@Preview(name = "Sign out · Compact / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable private fun SignOutCompactPreview() = SignOutPreview(false)
@Preview(name = "Sign out · Loading", widthDp = 393, heightDp = 852)
@Composable private fun SignOutLoadingPreview() = SignOutPreview(true, signOutPreviewState.copy(status = SignOutStatus.BUSY))
@Preview(name = "Sign out · Error", widthDp = 393, heightDp = 852)
@Composable private fun SignOutErrorPreview() = SignOutPreview(true, signOutPreviewState.copy(status = SignOutStatus.ERROR))
@Preview(name = "Sign out · Complete", widthDp = 393, heightDp = 852)
@Composable private fun SignOutCompletePreview() = SignOutPreview(false, SignOutUiState(status = SignOutStatus.COMPLETE))

@Composable
private fun SignOutPreview(dark: Boolean, state: SignOutUiState = signOutPreviewState) {
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme { SignOutScreenContent(state, onConfirm = {}, onCancel = {}) }
    }
}
