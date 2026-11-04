package com.FreeRave.remindme.screens.settings

import com.FreeRave.remindme.ui.components.SecureWindowEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import com.FreeRave.remindme.security.mfa.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.FreeRave.remindme.ui.components.PrivacyLinks
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.R
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.screens.auth.AuthViewModel
import com.FreeRave.remindme.screens.settings.components.*
import com.FreeRave.remindme.ui.theme.GlassTheme
import kotlinx.coroutines.launch

@Composable
fun AccountSecurityScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val resources = LocalResources.current
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language
    val activity = LocalActivity.current as? FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val notify: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) }; Unit }

    val mfaViewModel: MfaSettingsViewModel = hiltViewModel()
    val mfa by mfaViewModel.state.collectAsStateWithLifecycle()
    val passwordMfa by authViewModel.secondFactorState.collectAsStateWithLifecycle()
    val biometricEnabled by authViewModel.biometricLoginEnabled.collectAsStateWithLifecycle()
    val biometricState by authViewModel.biometricAuthState.collectAsStateWithLifecycle()
    val passwordState by authViewModel.changePasswordState.collectAsStateWithLifecycle()
    val passwordBusy = passwordState is AuthViewModel.ChangePasswordState.Loading
    val biometricBusy = biometricState is AuthViewModel.BiometricAuthState.Authenticating ||
        biometricState is AuthViewModel.BiometricAuthState.FailedAttempt
    var availability by remember { mutableStateOf(settingsViewModel.getBiometricAvailability()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, settingsViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                availability = settingsViewModel.getBiometricAvailability()
                mfaViewModel.controller.refresh()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            authViewModel.cancelBiometricSetup()
            if (!mfaViewModel.hiddenByAppLock) { mfaViewModel.controller.clearSensitiveState(); authViewModel.clearSecondFactor() }
        }
    }

    // Credentials and pending enrollment are deliberately not saved across process death.
    var showPassword by remember { mutableStateOf(false) }
    var showDeletion by remember { mutableStateOf(false) }
    SecureWindowEffect(showPassword || passwordMfa != null || mfa.step != MfaSettingsStep.NONE)
    LaunchedEffect(mfa.notice) {
        mfa.notice?.let { notice ->
            notify(resources.getString(if (notice == MfaNotice.ADDED) R.string.mfa_added else R.string.mfa_removed))
            mfaViewModel.controller.consumeNotice()
        }
    }

    LaunchedEffect(passwordState) {
        when (val result = passwordState) {
            is AuthViewModel.ChangePasswordState.Success -> {
                showPassword = false
                authViewModel.resetChangePasswordState()
                notify(result.message)
            }
            is AuthViewModel.ChangePasswordState.Error -> if (!showPassword) notify(result.message)
            else -> Unit
        }
    }

    GlassTheme {
        AccountSecurityScreenContent(
            AccountSecurityUiState(biometricEnabled, availability, biometricBusy, passwordBusy, mfa), snackbar,
            onBack = { navController.popBackStack() },
            onPassword = { if (!passwordBusy) { authViewModel.resetChangePasswordState(); showPassword = true } },
            onBiometric = { enabled ->
                if (!biometricBusy) {
                    if (enabled) {
                        if (activity != null) authViewModel.attemptToEnableBiometricLogin(activity) { _, message -> message?.let(notify) }
                        else notify(resources.getString(R.string.security_biometric_no_activity))
                    } else {
                        authViewModel.updateBiometricPreferenceDirectly(false) { success ->
                            notify(resources.getString(if (success) R.string.security_biometric_disabled else R.string.app_lock_failed))
                        }
                    }
                }
            },
            onDevices = { navController.navigate(Screen.ActiveSessions.route) },
            onVerifyEmail = { navController.navigate(Screen.VerifyEmail.route) { launchSingleTop = true } },
            onMfaAdd = { mfaViewModel.controller.request(MfaAction.Add) },
            onMfaRemove = { mfaViewModel.controller.request(MfaAction.Remove(it)) },
            onMfaRefresh = mfaViewModel.controller::refresh,
            onMfaSignIn = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true }; launchSingleTop = true } },
            onPrivacy = { PrivacyLinks.open(context, language) },
            onContactSupport = { PrivacyLinks.contactSupport(context, language) },
            onDeleteAccount = { showDeletion = true }
        )
        if (showDeletion) DeleteAccountRequestDialog(
            onContinue = { showDeletion = false; PrivacyLinks.open(context, language, deletion = true) },
            onDismiss = { showDeletion = false }
        )
        if (showPassword && passwordMfa == null) ChangePasswordDialog(
            onConfirm = authViewModel::changePassword,
            onDismiss = { if (!passwordBusy) { showPassword = false; authViewModel.resetChangePasswordState() } },
            busy = passwordBusy, error = (passwordState as? AuthViewModel.ChangePasswordState.Error)?.message,
            onInputChanged = { if (passwordState is AuthViewModel.ChangePasswordState.Error) authViewModel.resetChangePasswordState() })
        passwordMfa?.let { challenge ->
            MfaChallengeDialog(challenge, authViewModel::verifySecondFactor, authViewModel::selectSecondFactor,
                authViewModel::editSecondFactor, authViewModel::cancelSecondFactor)
        }
        when (mfa.step) {
            MfaSettingsStep.REAUTHENTICATE -> MfaReauthenticationDialog(mfa,
                onConfirm = { provider, password ->
                    (activity as? ComponentActivity)?.let { mfaViewModel.reauthenticate(it, provider, password) }
                        ?: notify(resources.getString(R.string.security_biometric_no_activity))
                }, onEdit = mfaViewModel.controller::edit, onDismiss = mfaViewModel.controller::cancel)
            MfaSettingsStep.CHALLENGE -> mfa.challenge?.let { challenge ->
                MfaChallengeDialog(challenge.copy(busy = mfa.busy), mfaViewModel.controller::verifyChallenge,
                    mfaViewModel.controller::selectFactor, mfaViewModel.controller::edit, mfaViewModel.controller::cancel)
            }
            MfaSettingsStep.ENROLL -> mfa.setup?.let { setup ->
                TwoFactorAuthSetupDialog(setup.secret, setup.qrUri,
                    onVerify = mfaViewModel.controller::enroll, onDismiss = mfaViewModel.controller::cancel,
                    onSecretCopied = notify, busy = mfa.busy,
                    error = mfa.error?.let { resources.getString(mfaErrorResource(it)) }, onInputChanged = mfaViewModel.controller::edit,
                    shouldClearClipboard = { !mfaViewModel.hiddenByAppLock })
            }
            MfaSettingsStep.NONE -> Unit
        }
    }
}
