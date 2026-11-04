package com.FreeRave.remindme.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.settings.components.*
import com.FreeRave.remindme.service.BiometricAvailability
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground
import com.FreeRave.remindme.security.mfa.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSecurityScreenContent(
    state: AccountSecurityUiState, snackbar: SnackbarHostState,
    onBack: () -> Unit, onPassword: () -> Unit, onBiometric: (Boolean) -> Unit,
    onDevices: () -> Unit, onMfaAdd: () -> Unit, onVerifyEmail: () -> Unit,
    onMfaRemove: (MfaFactor) -> Unit = {}, onMfaRefresh: () -> Unit = {}, onMfaSignIn: () -> Unit = {},
    onPrivacy: () -> Unit = {}, onDeleteAccount: () -> Unit = {}, onContactSupport: () -> Unit = {}
) {
    GlassBackground {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(), title = { Text(stringResource(R.string.security_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reminder_back)) }
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    item(key = "intro") {
                        GlassSurface(tint = MaterialTheme.colorScheme.primaryContainer) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                Text(stringResource(R.string.security_intro_title), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(R.string.security_intro), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item(key = "sign-in") {
                        SettingsSection(stringResource(R.string.security_sign_in)) {
                            SettingItem(stringResource(R.string.security_password),
                                stringResource(if (state.passwordBusy) R.string.security_updating_password else R.string.security_password_hint),
                                onPassword, icon = Icons.Outlined.LockReset, enabled = !state.passwordBusy, busy = state.passwordBusy)
                            SettingsDivider()
                            SettingItem(stringResource(R.string.email_verification_title),
                                stringResource(R.string.email_verification_entry_hint), onVerifyEmail, icon = Icons.Outlined.MarkEmailRead)
                            SettingsDivider()
                            SettingItemWithSwitch(stringResource(R.string.app_lock_setting), state.biometricEnabled, onBiometric,
                                enabled = !state.biometricBusy && (state.biometricEnabled || state.biometricAvailability == BiometricAvailability.AVAILABLE),
                                subtitle = biometricHint(state), icon = Icons.Outlined.Fingerprint)
                        }
                    }
                    item(key = "devices") {
                        SettingsSection(stringResource(R.string.security_devices)) {
                            SettingItem(stringResource(R.string.security_review_devices), stringResource(R.string.security_devices_hint),
                                onDevices, icon = Icons.Outlined.DevicesOther)
                        }
                    }
                    item(key = "two-step") {
                        SettingsSection(stringResource(R.string.security_two_step)) {
                            MfaSettingsSection(state.mfa, onMfaAdd, onMfaRemove, onMfaRefresh, onVerifyEmail, onMfaSignIn)
                        }
                    }
                    item(key = "privacy") {
                        SettingsSection(stringResource(R.string.privacy_section)) {
                            SettingItem(stringResource(R.string.privacy_policy), stringResource(R.string.privacy_policy_hint),
                                onPrivacy, icon = Icons.Outlined.PrivacyTip)
                            SettingsDivider()
                            SettingItem(stringResource(R.string.privacy_contact_support), stringResource(R.string.privacy_support_email),
                                onContactSupport, icon = Icons.Outlined.Email)
                            SettingsDivider()
                            SettingItem(stringResource(R.string.deletion_title), stringResource(R.string.deletion_hint),
                                onDeleteAccount, icon = Icons.Outlined.DeleteOutline, destructive = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun biometricHint(state: AccountSecurityUiState): String = stringResource(
    if (state.biometricBusy) R.string.security_biometric_busy else when (state.biometricAvailability) {
        BiometricAvailability.AVAILABLE -> R.string.app_lock_setting_hint
        BiometricAvailability.NO_HARDWARE -> R.string.security_biometric_no_hardware
        BiometricAvailability.HARDWARE_UNAVAILABLE -> R.string.security_biometric_unavailable
        BiometricAvailability.NONE_ENROLLED -> R.string.security_biometric_not_enrolled
        BiometricAvailability.UNSUPPORTED -> R.string.security_biometric_unsupported
    }
)
