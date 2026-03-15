package com.FreeRave.remindme.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Changed for AutoMirrored
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity // Added import
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.FreeRave.remindme.navigation.Screen // Added import for Screen
import com.FreeRave.remindme.screens.auth.AuthViewModel
import com.FreeRave.remindme.screens.settings.components.ChangePasswordDialog // Import new dialog
import com.FreeRave.remindme.screens.settings.components.ConfirmDisable2FADialog
import com.FreeRave.remindme.screens.settings.components.DisplayBackupCodesDialog // Import new dialog
import com.FreeRave.remindme.screens.settings.components.SettingItemWithSwitch
import com.FreeRave.remindme.screens.settings.components.TwoFactorAuthSetupDialog
import com.FreeRave.remindme.ui.theme.AppTypography
import kotlinx.coroutines.launch
import android.util.Log
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecurityScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel() // If needed for change password or user context
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 2FA State
    val twoFactorAuthEnabledState by settingsViewModel.twoFactorAuthEnabled.collectAsState()
    var showTwoFactorAuthSetupDialog by remember { mutableStateOf(false) }
    var showConfirmDisable2FADialog by remember { mutableStateOf(false) }
    var showBackupCodesDialog by remember { mutableStateOf(false) } // State for backup codes dialog
    val current2FASecret by settingsViewModel.currentTwoFactorAuthSecret.collectAsState()
    val current2FAOtpAuthUri by settingsViewModel.currentTwoFactorAuthOtpAuthUri.collectAsState()
    val generatedBackupCodes by settingsViewModel.generatedBackupCodes.collectAsState()

    // Change Password State
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val changePasswordState by authViewModel.changePasswordState.collectAsState()

    // Biometric State
    val biometricLoginEnabledState by authViewModel.biometricLoginEnabled.collectAsState() // From AuthViewModel
    val biometricAvailability by remember { // From SettingsViewModel, as it has app context
        mutableStateOf(settingsViewModel.getBiometricAvailability())
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Account Security", style = AppTypography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContainer { windowSizeClass, containerModifier ->
            AdaptiveColumn(
                modifier = containerModifier.padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                AdaptiveFormContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing(16.dp))
                    ) {
                        // --- Two-Factor Authentication Section ---
                        Text(
                            "Two-Factor Authentication (2FA)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = getResponsiveSpacing(4.dp))
                        )
                        SettingItemWithSwitch(
                            title = "Enable 2FA",
                            isChecked = twoFactorAuthEnabledState,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    settingsViewModel.startTwoFactorAuthSetup()
                                    showTwoFactorAuthSetupDialog = true
                                } else {
                                    // Show confirmation dialog before disabling
                                    showConfirmDisable2FADialog = true
                                }
                            }
                        )
                        Text(
                            "Protect your account with an extra layer of security. When enabled, you'll need to provide a code from your authenticator app during login.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = getResponsiveSpacing(2.dp), bottom = getResponsiveSpacing(8.dp))
                        )

                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))

                        // --- Biometric Login Section ---
                        Text(
                            "Biometric Login",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = getResponsiveSpacing(4.dp))
                        )
                        SettingItemWithSwitch(
                            title = "Enable Biometric Login",
                            isChecked = biometricLoginEnabledState,
                            enabled = biometricAvailability == com.FreeRave.remindme.service.BiometricAvailability.AVAILABLE,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    var unwrappedContext: android.content.Context = context
                                    var fragmentActivity: FragmentActivity? = null

                                    // First, check if the initial context is already a FragmentActivity
                                    if (unwrappedContext is FragmentActivity) {
                                        fragmentActivity = unwrappedContext
                                    } else {
                                        // If not, iterate up the ContextWrapper chain
                                        while (unwrappedContext is android.content.ContextWrapper) {
                                            if (unwrappedContext is FragmentActivity) {
                                                fragmentActivity = unwrappedContext
                                                break
                                            }
                                            unwrappedContext = unwrappedContext.baseContext ?: break
                                        }
                                    }

                                    if (fragmentActivity != null) {
                                        authViewModel.attemptToEnableBiometricLogin(fragmentActivity) { success, message ->
                                            coroutineScope.launch {
                                                message?.let { snackbarHostState.showSnackbar(it) }
                                            }
                                            // Note: biometricLoginEnabledState will update via collection from AuthViewModel
                                        }
                                    } else {
                                        Log.w("AccountSecurityScreen", "Could not find FragmentActivity. Initial LocalContext.current was: ${context::class.java.name}")
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Cannot perform biometric setup: Unable to find valid Activity.")
                                        }
                                    }
                                } else {
                                    // User wants to disable biometric login
                                    authViewModel.updateBiometricPreferenceDirectly(false)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Biometric login disabled.")
                                    }
                                }
                            }
                        )
                        if (biometricAvailability != com.FreeRave.remindme.service.BiometricAvailability.AVAILABLE) {
                            Text(
                                text = when (biometricAvailability) {
                                    com.FreeRave.remindme.service.BiometricAvailability.NO_HARDWARE -> "No biometric hardware detected."
                                    com.FreeRave.remindme.service.BiometricAvailability.HARDWARE_UNAVAILABLE -> "Biometric hardware currently unavailable."
                                    com.FreeRave.remindme.service.BiometricAvailability.NONE_ENROLLED -> "No biometrics enrolled. Please set up in device settings."
                                    else -> "Biometric login is not supported on this device."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = getResponsiveSpacing(8.dp), top = getResponsiveSpacing(2.dp), bottom = getResponsiveSpacing(8.dp))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
                        }


                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))

                        // --- Signed-in Devices Section (Placeholder) ---
                        Text(
                            "Signed-in Devices",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = getResponsiveSpacing(4.dp))
                        )
                        Text(
                            "Review and manage devices where your account is currently active.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = getResponsiveSpacing(4.dp))
                        )
                        Button(
                            onClick = {
                                navController.navigate(Screen.ActiveSessions.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Review Devices")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = getResponsiveSpacing(8.dp)))
                        // Spacer(modifier = Modifier.height(16.dp)) // Already handled by divider padding

                        // --- Change Password Section ---
                        Text(
                            "Change Password",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = getResponsiveSpacing(4.dp))
                        )
                        // Placeholder for Change Password UI/Navigation
                        Button(
                            onClick = {
                                showChangePasswordDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Password")
                        }
                        Spacer(modifier = Modifier.height(getResponsiveSpacing(8.dp)))
                    }
                }
            }
        }
    }

    // Handle ChangePasswordState
    LaunchedEffect(changePasswordState) {
        when (val state = changePasswordState) {
            is AuthViewModel.ChangePasswordState.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
                showChangePasswordDialog = false
                authViewModel.resetChangePasswordState() // Reset state in ViewModel
            }
            is AuthViewModel.ChangePasswordState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
                // Dialog remains open for user to correct, or they can cancel
                // Optionally reset state if error is meant to be shown once per attempt
                // authViewModel.resetChangePasswordState()
            }
            else -> {}
        }
    }


    if (showTwoFactorAuthSetupDialog && current2FASecret != null && current2FAOtpAuthUri != null) {
        TwoFactorAuthSetupDialog(
            secretKey = current2FASecret!!,
            otpAuthUri = current2FAOtpAuthUri!!, // Pass the URI
            onVerify = { code ->
                settingsViewModel.verifyAndEnableTwoFactorAuth(code) { success, message, backupCodes ->
                    coroutineScope.launch {
                        message?.let { snackbarHostState.showSnackbar(it) }
                    }
                    if (success) {
                        showTwoFactorAuthSetupDialog = false
                        if (backupCodes.isNotEmpty()) {
                            showBackupCodesDialog = true // Show backup codes dialog
                        }
                    }
                }
            },
            onDismiss = {
                settingsViewModel.cancelTwoFactorAuthSetup()
                showTwoFactorAuthSetupDialog = false
            },
            onSecretCopied = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    if (showConfirmDisable2FADialog) {
        ConfirmDisable2FADialog(
            onConfirm = { code ->
                settingsViewModel.disableTwoFactorAuth(code) { success, message ->
                    coroutineScope.launch {
                        message?.let { snackbarHostState.showSnackbar(message) }
                    }
                    if (success) {
                        showConfirmDisable2FADialog = false
                    }
                    // If not successful, dialog remains open for another attempt or cancellation
                }
            },
            onDismiss = {
                showConfirmDisable2FADialog = false
            }
        )
    }

    if (showBackupCodesDialog) {
        DisplayBackupCodesDialog(
            backupCodes = generatedBackupCodes,
            onDismiss = {
                showBackupCodesDialog = false
                settingsViewModel.cancelTwoFactorAuthSetup() // Clear all temporary 2FA setup state
            },
            onCopied = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { currentPassword, newPassword ->
                authViewModel.changePassword(currentPassword, newPassword)
                // Dialog dismissal and snackbar messages are handled by LaunchedEffect(changePasswordState)
            },
            onDismiss = {
                showChangePasswordDialog = false
                authViewModel.resetChangePasswordState() // Reset if dialog is dismissed manually
            }
        )
    }
}
