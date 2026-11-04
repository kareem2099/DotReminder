package com.FreeRave.remindme.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

private val verificationPreviewState = EmailVerificationUiState(phase = EmailVerificationPhase.READY,
    account = VerificationAccount("preview", "user@example.com", false))
@Preview(name = "Email · Light", widthDp = 393, heightDp = 852)
@Composable private fun EmailLightPreview() = EmailPreview(false)
@Preview(name = "Email · Dark", widthDp = 393, heightDp = 852)
@Composable private fun EmailDarkPreview() = EmailPreview(true)
@Preview(name = "Email · Compact / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable private fun EmailCompactPreview() = EmailPreview(false)
@Preview(name = "Email · Sent", widthDp = 393, heightDp = 852)
@Composable private fun EmailSentPreview() = EmailPreview(true, verificationPreviewState.copy(
    hasSentLink = true, notice = EmailVerificationNotice.SENT, resendSeconds = 42))
@Preview(name = "Email · Verified", widthDp = 393, heightDp = 852)
@Composable private fun EmailVerifiedPreview() = EmailPreview(true, verificationPreviewState.copy(phase = EmailVerificationPhase.VERIFIED))
@Preview(name = "Email · Loading", widthDp = 393, heightDp = 852)
@Composable private fun EmailLoadingPreview() = EmailPreview(false, EmailVerificationUiState())
@Preview(name = "Email · Error", widthDp = 393, heightDp = 852)
@Composable private fun EmailErrorPreview() = EmailPreview(true, verificationPreviewState.copy(error = EmailVerificationError.NETWORK))

@Composable
private fun EmailPreview(dark: Boolean, state: EmailVerificationUiState = verificationPreviewState) {
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            VerifyEmailScreenContent(state, onBack = {}, onCheck = {}, onSend = {}, onRetry = {}, onRetrySync = {}, onContinue = {}, onLogin = {})
        }
    }
}
