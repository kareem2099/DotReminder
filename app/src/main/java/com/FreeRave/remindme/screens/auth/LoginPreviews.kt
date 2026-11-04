package com.FreeRave.remindme.screens.auth

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.auth.components.PasswordResetDialog
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Preview(name = "Login · Light", widthDp = 393, heightDp = 852)
@Composable
private fun LoginLightPreview() = LoginPreview(false)
@Preview(name = "Login · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun LoginDarkPreview() = LoginPreview(true)
@Preview(name = "Login · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun LoginCompactPreview() = LoginPreview(false)
@Preview(name = "Login · Tablet", widthDp = 840, heightDp = 1100)
@Composable
private fun LoginTabletPreview() = LoginPreview(false)
@Preview(name = "Login · Error", widthDp = 393, heightDp = 852)
@Composable
private fun LoginErrorPreview() = LoginPreview(true, LoginUiState(email = "user@example.com", error = "Check your connection and try again."))
@Preview(name = "Login · Google loading", widthDp = 393, heightDp = 852)
@Composable
private fun LoginLoadingPreview() = LoginPreview(true, LoginUiState(busy = true, method = LoginMethod.GOOGLE))
@Preview(name = "Login · Reset password", widthDp = 393, heightDp = 852)
@Composable
private fun LoginResetPreview() = LoginPreview(true, reset = true)

@Composable
private fun LoginPreview(dark: Boolean, state: LoginUiState = LoginUiState(), reset: Boolean = false) {
    val snackbar = remember { SnackbarHostState() }
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            LoginScreenContent(state, snackbar, onEmail = {}, onPassword = {}, onRemember = {}, onSubmit = {},
                onForgot = {}, onGoogle = {}, onFacebook = {}, onBiometric = {}, onSignUp = {})
            if (reset) PasswordResetDialog("", PasswordResetState.Idle, onSend = {}, onDismiss = {}, onEdit = {})
        }
    }
}
