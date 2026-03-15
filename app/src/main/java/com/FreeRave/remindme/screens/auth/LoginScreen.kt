package com.FreeRave.remindme.screens.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.MainViewModel
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.service.BiometricAvailability
import com.FreeRave.remindme.ui.components.*
import com.FreeRave.remindme.ui.theme.ScreenBackground
import com.FreeRave.remindme.ui.theme.ScreenType
import com.FreeRave.remindme.utils.ValidationUtils
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val rememberedEmailFromVM by viewModel.rememberedEmail.collectAsStateWithLifecycle()
    val rememberMeEnabledFromVM by viewModel.rememberMeEnabled.collectAsStateWithLifecycle()
    val biometricLoginEnabledUserPref by viewModel.biometricLoginEnabled.collectAsStateWithLifecycle()
    val biometricAuthState by viewModel.biometricAuthState.collectAsStateWithLifecycle()
    val currentTheme by mainViewModel.currentTheme.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Determine biometric availability once
    val biometricAvailability = remember { viewModel.getBiometricAvailability() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var triggerShake by remember { mutableStateOf(false) }
    var resetRequested by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    // Launcher for Google Sign In - reverting to StartActivityForResult based on compiler feedback
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // The existing handler in AuthViewModel expects ActivityResult and calls GoogleSignIn.getSignedInAccountFromIntent(result.data)
        // This is consistent with StartActivityForResult.
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                // AuthViewModel's handleGoogleSignInResult will update loginState
                // and trigger navigation or error messages via LaunchedEffect(loginState)
                viewModel.handleGoogleSignInResult(result) // Updated method name
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Google Sign-In cancelled or failed by user.")
            }
        }
    }

    // Removed: viewModel.initializeGoogleSignIn(context) as it's no longer in AuthViewModel
    // The new service gets client on demand.

    // Pre-fill email if remembered and current email state is empty
    LaunchedEffect(rememberedEmailFromVM, rememberMeEnabledFromVM) {
        if (rememberMeEnabledFromVM && !rememberedEmailFromVM.isNullOrEmpty() && email.isEmpty()) {
            email = rememberedEmailFromVM!!
        }
    }

    // Attempt biometric auth on screen launch if conditions are met
    LaunchedEffect(Unit) { // Runs once when the screen is composed
        if (rememberMeEnabledFromVM &&
            !rememberedEmailFromVM.isNullOrEmpty() &&
            biometricLoginEnabledUserPref &&
            biometricAvailability == BiometricAvailability.AVAILABLE
        ) {
            if (context is FragmentActivity) {
                viewModel.triggerBiometricAuth(context)
            } else {
                // Log or handle the case where context is not a FragmentActivity
                // This is unlikely in typical app scenarios for a screen Composable
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Cannot initiate biometric auth: Invalid context.")
                }
            }
        }
    }

    // Handle Biometric Auth State changes
    LaunchedEffect(biometricAuthState) {
        when (val state = biometricAuthState) {
            is AuthViewModel.BiometricAuthState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                // Optionally reset biometric auth state in ViewModel if it's a one-off message
                // viewModel.resetBiometricAuthState() 
            }
            is AuthViewModel.BiometricAuthState.FailedAttempt -> {
                if (state.remaining > 0) {
                    snackbarHostState.showSnackbar("Biometric failed. ${state.remaining} attempts remaining.")
                } else {
                    // This case is handled by Error state from ViewModel after max attempts
                }
            }
            is AuthViewModel.BiometricAuthState.Success -> {
                // LoginState.Success will be triggered by ViewModel, leading to navigation
                // snackbarHostState.showSnackbar("Biometric login successful!") // Optional success message
            }
            else -> { /* Idle, Authenticating - usually no action needed here as system prompt handles UI */ }
        }
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is AuthViewModel.LoginState.Success -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is AuthViewModel.LoginState.Error -> {
                triggerShake = true
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    LaunchedEffect(triggerShake) {
        if (triggerShake) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(0f, keyframes {
                durationMillis = 400
                -10f at 50; 10f at 100; -8f at 150; 8f at 200; -5f at 250; 5f at 300; 0f at 350
            })
            triggerShake = false
        }
    }

    val formAnim = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { // This one is for the animation
        formAnim.targetState = true
    }

    val isSystemDarkTheme = isSystemInDarkTheme()
    val resolvedUseDarkTheme = remember(currentTheme, isSystemDarkTheme) {
        when (currentTheme) {
            ThemeSetting.LIGHT -> false
            ThemeSetting.DARK -> true
            else -> isSystemDarkTheme // For SYSTEM and all color themes, respect system setting
        }
    }

    ScreenBackground(
        screenType = ScreenType.LOGIN,
        themeSetting = currentTheme,
        useDarkTheme = resolvedUseDarkTheme
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        AdaptiveContainer { windowSizeClass, containerModifier ->
            AnimatedVisibility(
                visibleState = formAnim,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(700)),
                exit = fadeOut()
            ) {
                AdaptiveColumn(
                    modifier = containerModifier,
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    scrollable = true
                ) {
                    LoginBranding(modifier = Modifier.offset(x = shakeOffset.value.dp))

                    AdaptiveSpacer(getResponsiveSpacing(32.dp))

                    LoginForm(
                        email = email,
                        onEmailChange = {
                            email = it
                            emailError = !ValidationUtils.isValidEmail(it)
                        },
                        emailError = emailError,
                        password = password,
                        onPasswordChange = {
                            password = it
                            passwordError = it.length < 8 // Basic validation, can be enhanced
                        },
                        passwordError = passwordError,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                        onForgotPasswordClick = {
                            if (ValidationUtils.isValidEmail(email)) {
                                resetRequested = true // Can be used for UI feedback if needed
                                coroutineScope.launch {
                                    viewModel.sendPasswordResetEmail(email)
                                    snackbarHostState.showSnackbar("Password reset email sent to $email")
                                    resetRequested = false
                                }
                            } else {
                                emailError = true // Show error on email field
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Enter a valid email to reset password")
                                }
                            }
                        },
                        rememberMeChecked = rememberMeEnabledFromVM, // Pass the collected state
                        onRememberMeChange = viewModel::userToggledRememberMe, // Pass the ViewModel's method
                        modifier = Modifier.fillMaxWidth()
                    )

                    AdaptiveSpacer(getResponsiveSpacing(24.dp))

                    LoginActions(
                        isLoading = loginState is AuthViewModel.LoginState.Loading,
                        isLoginButtonEnabled = loginState !is AuthViewModel.LoginState.Loading,
                        onLoginClick = {
                            emailError = !ValidationUtils.isValidEmail(email)
                            passwordError = password.length < 8 // Keep consistent validation

                            if (!emailError && !passwordError) {
                                viewModel.loginWithEmail(email, password)
                            } else {
                                triggerShake = true // Shake the branding card on error
                            }
                        },
                        onGoogleSignInClick = {
                            coroutineScope.launch {
                                try {
                                    val signInIntent: Intent = viewModel.getGoogleSignInIntent(context) // Updated method name
                                    googleSignInLauncher.launch(signInIntent)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Could not start Google Sign-In: ${e.localizedMessage}")
                                }
                            }
                        },
                        onFacebookSignInClick = {
                            val activity = context as? ComponentActivity
                            if (activity != null) {
                                viewModel.signInWithFacebook(activity)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Cannot start Facebook login: Context is not an Activity.")
                                }
                            }
                        },
                        onBiometricLoginClick = {
                            if (context is FragmentActivity) {
                                viewModel.triggerBiometricAuth(context)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Cannot initiate biometric auth: Invalid context.")
                                }
                            }
                        },
                        isBiometricAuthPossible = biometricLoginEnabledUserPref &&
                                rememberMeEnabledFromVM &&
                                !rememberedEmailFromVM.isNullOrEmpty() &&
                                biometricAvailability == BiometricAvailability.AVAILABLE,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AdaptiveSpacer(getResponsiveSpacing(12.dp))

                    SignUpNavigationLink(
                        onSignUpClick = { navController.navigate(Screen.SignUp.route) }
                    )
                }
            }
        }
    }
}