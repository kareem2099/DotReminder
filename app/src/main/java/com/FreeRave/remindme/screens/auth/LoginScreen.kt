package com.FreeRave.remindme.screens.auth

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalResources
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.R
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.screens.auth.components.PasswordResetDialog
import com.FreeRave.remindme.service.BiometricAvailability
import com.FreeRave.remindme.ui.theme.GlassTheme
import kotlinx.coroutines.launch
import com.FreeRave.remindme.security.mfa.MfaChallengeDialog
import com.FreeRave.remindme.ui.components.SecureWindowEffect

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {
    val resources = LocalResources.current
    val activity = LocalActivity.current
    val login by viewModel.loginState.collectAsStateWithLifecycle()
    val secondFactor by viewModel.secondFactorState.collectAsStateWithLifecycle()
    val method by viewModel.activeLoginMethod.collectAsStateWithLifecycle()
    val reset by viewModel.passwordResetState.collectAsStateWithLifecycle()
    val storedEmail by viewModel.rememberedEmail.collectAsStateWithLifecycle()
    val rememberEmail by viewModel.rememberMeEnabled.collectAsStateWithLifecycle()
    val biometricPreference by viewModel.biometricLoginEnabled.collectAsStateWithLifecycle()
    val biometric by viewModel.biometricAuthState.collectAsStateWithLifecycle()
    val preferenceError by viewModel.preferenceError.collectAsStateWithLifecycle()
    var availability by remember { mutableStateOf(viewModel.getBiometricAvailability()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) availability = viewModel.getBiometricAvailability()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); viewModel.clearSecondFactor() }
    }

    var email by rememberSaveable { mutableStateOf("") }
    var emailEdited by rememberSaveable { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    var activityError by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val busy = login is AuthViewModel.LoginState.Loading || login is AuthViewModel.LoginState.SecondFactor || reset is PasswordResetState.Loading
    SecureWindowEffect(password.isNotEmpty() || secondFactor != null)
    val validation = validateLogin(email, password)
    LaunchedEffect(storedEmail, rememberEmail) {
        email = rememberedEmailForForm(email, emailEdited, rememberEmail, storedEmail)
    }
    LaunchedEffect(login) {
        if (login is AuthViewModel.LoginState.SecondFactor) password = ""
        if (login is AuthViewModel.LoginState.Success && navController.currentBackStackEntry?.destination?.route == Screen.Login.route) {
            password = ""
            navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true }; launchSingleTop = true }
        }
    }
    LaunchedEffect(reset) {
        if (reset is PasswordResetState.Sent) {
            showReset = false
            viewModel.resetPasswordResetState()
            scope.launch { snackbar.showSnackbar(resources.getString(R.string.login_reset_sent)) }
        }
    }
    val state = LoginUiState(email, password, rememberEmail, submitted && validation.emailInvalid,
        submitted && validation.passwordMissing, busy, method,
        (login as? AuthViewModel.LoginState.Error)?.message ?: (biometric as? AuthViewModel.BiometricAuthState.Error)?.message ?: activityError ?: preferenceError,
        showBiometrics = biometricPreference && rememberEmail && !storedEmail.isNullOrBlank() && availability == BiometricAvailability.AVAILABLE,
        biometricReady = viewModel.hasBiometricSession())
    val noActivity = { activityError = resources.getString(R.string.login_activity_unavailable) }
    GlassTheme {
        LoginScreenContent(state, snackbar,
            onEmail = { email = it; emailEdited = true; activityError = null; viewModel.clearLoginError() },
            onPassword = { password = it; activityError = null; viewModel.clearLoginError() },
            onRemember = viewModel::userToggledRememberMe,
            onSubmit = { if (!busy) { submitted = true; if (validation.valid) viewModel.loginWithEmail(email, password) } },
            onForgot = { if (!busy) { viewModel.resetPasswordResetState(); showReset = true } },
            onGoogle = { if (activity != null) viewModel.signInWithGoogleCredentialManager(activity) else noActivity() },
            onFacebook = { (activity as? ComponentActivity)?.let(viewModel::signInWithFacebook) ?: noActivity() },
            onBiometric = { (activity as? FragmentActivity)?.let(viewModel::triggerBiometricAuth) ?: noActivity() },
            onSignUp = { if (!busy) navController.navigate(Screen.SignUp.route) { launchSingleTop = true } })
        if (showReset) PasswordResetDialog(email.trim(), reset, onSend = viewModel::sendPasswordResetEmail,
            onDismiss = { if (reset !is PasswordResetState.Loading) { showReset = false; viewModel.resetPasswordResetState() } },
            onEdit = viewModel::resetPasswordResetState)
        secondFactor?.let { challenge ->
            MfaChallengeDialog(challenge, viewModel::verifySecondFactor, viewModel::selectSecondFactor,
                viewModel::editSecondFactor, viewModel::cancelSecondFactor)
        }
    }
}
