package com.FreeRave.remindme.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Preview(name = "Signup · Light", widthDp = 393, heightDp = 852)
@Composable private fun SignUpLightPreview() = SignUpPreview(false)
@Preview(name = "Signup · Dark", widthDp = 393, heightDp = 852)
@Composable private fun SignUpDarkPreview() = SignUpPreview(true)
@Preview(name = "Signup · Compact / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable private fun SignUpCompactPreview() = SignUpPreview(false)
@Preview(name = "Signup · Verify phone", widthDp = 393, heightDp = 852)
@Composable private fun SignUpCodePreview() = SignUpPreview(true, SignUpUiState(step = SignUpStep.CODE, recipient = "+201012345678"))
@Preview(name = "Signup · Profile retry", widthDp = 393, heightDp = 852)
@Composable private fun SignUpRetryPreview() = SignUpPreview(true, SignUpUiState(step = SignUpStep.PROFILE, error = "Retry saving your details."))
@Preview(name = "Signup · Complete", widthDp = 393, heightDp = 852)
@Composable private fun SignUpCompletePreview() = SignUpPreview(false, SignUpUiState(step = SignUpStep.COMPLETE))

@Composable
private fun SignUpPreview(dark: Boolean, state: SignUpUiState = SignUpUiState()) {
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            SignUpScreenContent(state, onEdit = { _, _ -> }, onSend = {}, onCreate = {}, onEditDetails = {},
                onRetry = {}, onLogin = {}, onContinue = {})
        }
    }
}
