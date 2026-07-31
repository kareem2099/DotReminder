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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import com.FreeRave.remindme.ui.layout.*
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.theme.OceanBlueBackground
import com.FreeRave.remindme.utils.ValidationUtils
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

    val biometricAvailability = remember { viewModel.getBiometricAvailability() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var triggerShake by remember { mutableStateOf(false) }
    var resetRequested by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(rememberedEmailFromVM, rememberMeEnabledFromVM) {
        if (rememberMeEnabledFromVM && !rememberedEmailFromVM.isNullOrEmpty() && email.isEmpty()) {
            email = rememberedEmailFromVM!!
        }
    }

    LaunchedEffect(Unit) {
        if (rememberMeEnabledFromVM &&
            !rememberedEmailFromVM.isNullOrEmpty() &&
            biometricLoginEnabledUserPref &&
            biometricAvailability == BiometricAvailability.AVAILABLE
        ) {
            if (context is FragmentActivity) {
                viewModel.triggerBiometricAuth(context)
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Cannot initiate biometric auth: Invalid context.")
                }
            }
        }
    }

    LaunchedEffect(biometricAuthState) {
        when (val state = biometricAuthState) {
            is AuthViewModel.BiometricAuthState.Error ->
                snackbarHostState.showSnackbar(state.message)
            is AuthViewModel.BiometricAuthState.FailedAttempt ->
                if (state.remaining > 0)
                    snackbarHostState.showSnackbar("Biometric failed. ${state.remaining} attempts remaining.")
            else -> {}
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
    LaunchedEffect(Unit) { formAnim.targetState = true }

    val isSystemDarkTheme = isSystemInDarkTheme()
    val resolvedUseDarkTheme = remember(currentTheme, isSystemDarkTheme) {
        when (currentTheme) {
            ThemeSetting.LIGHT -> false
            ThemeSetting.DARK -> true
            else -> isSystemDarkTheme
        }
    }

    // Instead of ScreenBackground — OceanBlueBackground directly
    OceanBlueBackground(
        themeSetting = currentTheme,
        useDarkTheme = resolvedUseDarkTheme
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->

            AdaptiveContainer { _, containerModifier ->
                AnimatedVisibility(
                    visibleState = formAnim,
                    enter = fadeIn() + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(700)
                    ),
                    exit = fadeOut()
                ) {
                    AdaptiveColumn(
                        modifier = containerModifier.padding(paddingValues),
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
                                passwordError = it.length < 8
                            },
                            passwordError = passwordError,
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                            onForgotPasswordClick = {
                                if (ValidationUtils.isValidEmail(email)) {
                                    resetRequested = true
                                    coroutineScope.launch {
                                        viewModel.sendPasswordResetEmail(email)
                                        snackbarHostState.showSnackbar("Password reset email sent to $email")
                                        resetRequested = false
                                    }
                                } else {
                                    emailError = true
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Enter a valid email to reset password")
                                    }
                                }
                            },
                            rememberMeChecked = rememberMeEnabledFromVM,
                            onRememberMeChange = viewModel::userToggledRememberMe,
                            modifier = Modifier.fillMaxWidth()
                        )

                        AdaptiveSpacer(getResponsiveSpacing(24.dp))

                        LoginActions(
                            isLoading = loginState is AuthViewModel.LoginState.Loading,
                            isLoginButtonEnabled = loginState !is AuthViewModel.LoginState.Loading,
                            onLoginClick = {
                                emailError = !ValidationUtils.isValidEmail(email)
                                passwordError = password.length < 8
                                if (!emailError && !passwordError) {
                                    viewModel.loginWithEmail(email, password)
                                } else {
                                    triggerShake = true
                                }
                            },
                            onGoogleSignInClick = {
                                viewModel.signInWithGoogleCredentialManager(context)
                            },
                            onFacebookSignInClick = {
                                val activity = context as? ComponentActivity
                                if (activity != null) {
                                    viewModel.signInWithFacebook(activity)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Cannot start Facebook login.")
                                    }
                                }
                            },
                            onBiometricLoginClick = {
                                if (context is FragmentActivity) {
                                    viewModel.triggerBiometricAuth(context)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Cannot initiate biometric auth.")
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
}