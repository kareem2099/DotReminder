package com.FreeRave.remindme.screens.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.settings.components.ChangePasswordDialog
import com.FreeRave.remindme.service.BiometricAvailability
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Preview(name = "Security · Light", widthDp = 393, heightDp = 852)
@Composable
private fun SecurityLightPreview() = SecurityPreview(false)

@Preview(name = "Security · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun SecurityDarkPreview() = SecurityPreview(true)

@Preview(name = "Security · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun SecurityCompactPreview() = SecurityPreview(false)

@Preview(name = "Security · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun SecurityTabletPreview() = SecurityPreview(false)

@Preview(name = "Security · No biometrics", widthDp = 393, heightDp = 852)
@Composable
private fun SecurityUnavailablePreview() = SecurityPreview(false, available = false)

@Preview(name = "Security · Password", widthDp = 393, heightDp = 852)
@Composable
private fun SecurityPasswordPreview() = SecurityPreview(true, password = true)

@Preview(name = "Security · Password loading", widthDp = 393, heightDp = 852)
@Composable
private fun SecurityPasswordBusyPreview() = SecurityPreview(true, password = true, busy = true)

@Composable
private fun SecurityPreview(dark: Boolean, available: Boolean = true, password: Boolean = false, busy: Boolean = false) {
    val theme = if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT
    val snackbar = remember { SnackbarHostState() }
    RemindMeAppTheme(theme, useDarkTheme = dark) {
        GlassTheme {
            AccountSecurityScreenContent(AccountSecurityUiState(biometricEnabled = false,
                biometricAvailability = if (available) BiometricAvailability.AVAILABLE else BiometricAvailability.NONE_ENROLLED,
                passwordBusy = busy), snackbar, onBack = {}, onPassword = {}, onBiometric = {}, onDevices = {}, onMfaAdd = {}, onVerifyEmail = {})
            if (password) ChangePasswordDialog(onConfirm = { _, _ -> }, onDismiss = {}, busy = busy,
                error = null, onInputChanged = {})
        }
    }
}
