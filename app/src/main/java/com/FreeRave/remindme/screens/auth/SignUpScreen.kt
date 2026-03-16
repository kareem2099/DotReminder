package com.FreeRave.remindme.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.service.FirebaseService
import com.FreeRave.remindme.model.User
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.components.* // Import all components
import com.FreeRave.remindme.utils.* // Import all from utils, including LocationUtils
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants // Added for IterateForever
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.FreeRave.remindme.service.VerificationService // Add import
import kotlinx.coroutines.flow.collectLatest
import com.FreeRave.remindme.MainViewModel
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.layout.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity // For activity access
import com.FreeRave.remindme.ui.components.PhoneFieldWithOTP
import com.FreeRave.remindme.ui.theme.OceanBlueBackground

// Phonenumber import might be unused now, will be removed by formatter if so

@Composable
fun SignUpScreen(
    navController: NavController,
    authService: FirebaseAuthServiceInterface,
    verificationService: VerificationService
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+20") } // Default to Egypt
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var showWelcomeLottie by remember { mutableStateOf(true) } // New state for welcome Lottie
    var showOtpVerification by remember { mutableStateOf(false) } // New state for OTP step
    var otpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf(false) }
    var otpId by remember { mutableStateOf<String?>(null) } // Store the OTP session ID
    var passwordStrength by remember { mutableStateOf(PasswordStrength.NONE) }
    var isLoadingOTP by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val density = LocalDensity.current.density // For cameraDistance in graphicsLayer
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    // Get current theme for background
    val mainViewModel: MainViewModel = hiltViewModel()
    val currentTheme by mainViewModel.currentTheme.collectAsStateWithLifecycle()

    // Try to auto-detect country code if empty or just +
    // Call RequestLocationAndUpdateCountryCode directly from Composable context if needed
    if (countryCode.isEmpty() || countryCode == "+") {
        RequestLocationAndUpdateCountryCode(
            context = context, // Pass context explicitly
            snackbarHostState = snackbarHostState,
            onCountryCodeDetected = { detectedCode ->
                detectedCode?.let { countryCode = it }
            }
        )
    }

    // Page flip animation logic
    LaunchedEffect(key1 = showSuccessAnimation) {
        if (!showSuccessAnimation) { // Only run this if not in success state
            showWelcomeLottie = false // Trigger the flip immediately
        }
    }

    val animationProgress by animateFloatAsState(
        targetValue = if (showWelcomeLottie) 0f else 1f, // 0f for welcome, 1f for signup form
        animationSpec = tween(durationMillis = 1000), // Duration of the flip animation
        label = "pageFlipProgress"
    )

    val isSystemDarkTheme = isSystemInDarkTheme()
    val resolvedUseDarkTheme = remember(currentTheme, isSystemDarkTheme) {
        when (currentTheme) {
            ThemeSetting.LIGHT -> false
            ThemeSetting.DARK -> true
            else -> isSystemDarkTheme // For SYSTEM and all color themes, respect system setting
        }
    }

    OceanBlueBackground(
        themeSetting = currentTheme,
        useDarkTheme = resolvedUseDarkTheme
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->

            AdaptiveContainer { windowSizeClass, containerModifier ->
                Column(
                    modifier = containerModifier
                        .padding(paddingValues)
                        .graphicsLayer {
                            rotationY = -90f * (1f - animationProgress)
                            cameraDistance = 12 * density
                            this.alpha = animationProgress
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showSuccessAnimation) {
                        val successComposition by rememberLottieComposition(
                            LottieCompositionSpec.Asset(
                                "lottie/checkmark.json"
                            )
                        )
                        LottieAnimation(
                            composition = successComposition,
                            iterations = 1, // Play once for success
                            modifier = Modifier.size(200.dp)
                        )
                        Text(
                            text = "Account Created Successfully!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    } else {
                        // Box to contain the animated Welcome Lottie and the SignUp Content (Branding + Form)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(
                                    1f,
                                    fill = false
                                ), // Allow content to define its size, centered by parent Column
                            contentAlignment = Alignment.Center
                        ) {
                            // Signup Content (Branding Card + Form + Buttons) (Back of the page)
                            if (animationProgress > 0.01f) { // Only compose if at least partially visible
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState)
                                        .graphicsLayer {
                                            rotationY =
                                                -90f * (1f - animationProgress) // From -90 (invisible) to 0 (flat)
                                            cameraDistance = 12 * density
                                            this.alpha = animationProgress // Fades in
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    SignUpBranding() // Simplified: no props needed here

                                    AdaptiveSpacer(getResponsiveSpacing(16.dp))

                                    SignUpForm(
                                        name = name,
                                        onNameChange = {
                                            name = it
                                            nameError = it.isEmpty()
                                        },
                                        nameError = nameError,
                                        nameFocusRequester = nameFocusRequester,
                                        email = email,
                                        onEmailChange = {
                                            email = it
                                            emailError = !ValidationUtils.isValidEmail(it)
                                        },
                                        emailError = emailError,
                                        emailFocusRequester = emailFocusRequester,
                                        phone = phone,
                                        onPhoneChange = {
                                            phone = it
                                            val phoneUtil = PhoneNumberUtil.getInstance()
                                            val phoneNumberStr = "$countryCode$it"
                                            try {
                                                val parsedNumber =
                                                    phoneUtil.parse(phoneNumberStr, null)
                                                phoneError = !phoneUtil.isValidNumber(parsedNumber)
                                            } catch (e: Exception) {
                                                phoneError = true
                                            }
                                        },
                                        countryCode = countryCode,
                                        onCountryCodeChange = { countryCode = it },
                                        phoneError = phoneError,
                                        phoneFocusRequester = phoneFocusRequester,
                                        password = password,
                                        onPasswordChange = {
                                            password = it
                                            passwordStrength =
                                                ValidationUtils.calculatePasswordStrength(it)
                                            confirmPasswordError =
                                                confirmPassword.isNotEmpty() && it != confirmPassword
                                        },
                                        passwordVisible = passwordVisible,
                                        onPasswordVisibilityChange = { passwordVisible = it },
                                        passwordError = passwordError,
                                        passwordStrength = passwordStrength,
                                        confirmPassword = confirmPassword,
                                        onConfirmPasswordChange = {
                                            confirmPassword = it
                                            confirmPasswordError = it != password
                                        },
                                        confirmPasswordVisible = confirmPasswordVisible,
                                        onConfirmPasswordVisibilityChange = {
                                            confirmPasswordVisible = it
                                        },
                                        confirmPasswordError = confirmPasswordError,
                                        passwordFocusRequester = passwordFocusRequester,
                                        confirmPasswordFocusRequester = confirmPasswordFocusRequester,
                                        focusManager = focusManager,
                                        onSendOTP = {
                                            val phoneUtil = PhoneNumberUtil.getInstance()
                                            val phoneNumberStr = "$countryCode$phone"
                                            val isValidNumber = try {
                                                val parsedNumber =
                                                    phoneUtil.parse(phoneNumberStr, null)
                                                phoneUtil.isValidNumber(parsedNumber)
                                            } catch (e: Exception) {
                                                false
                                            }

                                            if (isValidNumber) {
                                                isLoadingOTP = true
                                                coroutineScope.launch {
                                                    val recipient = "$countryCode$phone"
                                                    verificationService.sendOTP(
                                                        recipient,
                                                        VerificationService.VerificationType.PHONE,
                                                        context as ComponentActivity
                                                    )
                                                        .collectLatest { result ->
                                                            if (result.success) {
                                                                otpId = result.otpId
                                                                otpSent = true
                                                                showOtpVerification = true
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar("OTP sent to $recipient")
                                                                }
                                                            } else {
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar(
                                                                        result.message
                                                                    )
                                                                }
                                                            }
                                                            isLoadingOTP = false
                                                        }
                                                }
                                            } else {
                                                phoneError = true
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Please enter a valid phone number, including country code.")
                                                }
                                            }
                                        },
                                        isLoadingOTP = isLoadingOTP,
                                        otpSent = otpSent
                                    )

                                    AdaptiveSpacer(getResponsiveSpacing(16.dp))

                                    AnimatedVisibility(
                                        visible = isLoading,
                                        enter = fadeIn(animationSpec = tween(200)),
                                        exit = fadeOut(animationSpec = tween(200))
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (showOtpVerification) {
                                        TextFieldInput(
                                            value = otpCode,
                                            onValueChange = {
                                                otpCode = it
                                                otpError = false // Clear error when typing
                                            },
                                            label = "OTP Code",
                                            placeholder = "Enter 6-digit code",
                                            isError = otpError,
                                            supportingText = {
                                                if (otpError) {
                                                    Text(
                                                        "Invalid OTP code",
                                                        color = MaterialTheme.colorScheme.error,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                } else {
                                                    Text(
                                                        "OTP sent to $countryCode$phone",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            },
                                            modifier = Modifier,
                                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        TextButton(
                                            onClick = { showOtpVerification = false },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Back to form")
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    PrimaryButton(
                                        text = if (isLoading) {
                                            if (showOtpVerification) "Signing Up..." else "Sending OTP..."
                                        } else {
                                            if (showOtpVerification) "Sign Up" else "Send OTP"
                                        },
                                        onClick = {
                                            if (showOtpVerification) {
                                                // Verify OTP
                                                isLoading = true
                                                coroutineScope.launch {
                                                    val result =
                                                        verificationService.verifyOTP(
                                                            otpId,
                                                            otpCode
                                                        )
                                                    isLoading = false
                                                    if (result.success) {
                                                        // OTP verified, now proceed with Firebase sign up
                                                        authService.signUpWithEmail(
                                                            email = email,
                                                            password = password,
                                                            name = name,
                                                            onSuccess = {
                                                                val e164Phone = try {
                                                                    val phoneUtil =
                                                                        PhoneNumberUtil.getInstance()
                                                                    val regionCode = try {
                                                                        phoneUtil.getRegionCodeForCountryCode(
                                                                            countryCode.substring(1)
                                                                                .toInt()
                                                                        )
                                                                    } catch (e: Exception) {
                                                                        "US"
                                                                    }
                                                                    val phoneNumber =
                                                                        phoneUtil.parse(
                                                                            phone,
                                                                            regionCode
                                                                        )
                                                                    if (phoneUtil.isValidNumber(
                                                                            phoneNumber
                                                                        )
                                                                    ) {
                                                                        phoneUtil.format(
                                                                            phoneNumber,
                                                                            PhoneNumberUtil.PhoneNumberFormat.E164
                                                                        )
                                                                    } else {
                                                                        "$countryCode$phone"
                                                                    }
                                                                } catch (e: Exception) {
                                                                    "$countryCode$phone"
                                                                }

                                                                // Since phone OTP verified, set isEmailVerified to true (representing account verified)
                                                                val user = User(
                                                                    id = authService.getCurrentUserId(),
                                                                    name = name,
                                                                    email = email,
                                                                    phone = e164Phone,
                                                                    isEmailVerified = true // OTP verification replaces email verification
                                                                )
                                                                FirebaseService().saveUser(user) { success ->
                                                                    if (success) {
                                                                        coroutineScope.launch {
                                                                            showSuccessAnimation =
                                                                                true
                                                                            delay(3000L)
                                                                            // After OTP verification, navigate directly to home (no need for email verification screen)
                                                                            navController.navigate(
                                                                                Screen.Home.route
                                                                            ) {
                                                                                popUpTo(Screen.SignUp.route) {
                                                                                    inclusive = true
                                                                                }
                                                                            }
                                                                        }
                                                                    } else {
                                                                        coroutineScope.launch {
                                                                            snackbarHostState.showSnackbar(
                                                                                "Failed to save user profile"
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onFailure = { e ->
                                                                val errorMessage = when (e) {
                                                                    is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters"
                                                                    is FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                                                                    is FirebaseAuthUserCollisionException -> "Email already in use"
                                                                    else -> "Sign up failed: ${e.localizedMessage}"
                                                                }
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar(
                                                                        errorMessage
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    } else {
                                                        otpError = true
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(result.message)
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Send OTP
                                                nameError = name.isEmpty()
                                                emailError = !ValidationUtils.isValidEmail(email)
                                                phoneError = phone.isEmpty() // Basic validation
                                                passwordError =
                                                    !ValidationUtils.isValidPassword(password)
                                                confirmPasswordError =
                                                    !ValidationUtils.doPasswordsMatch(
                                                        password,
                                                        confirmPassword
                                                    )

                                                if (!nameError && !emailError && !phoneError && !passwordError && !confirmPasswordError) {
                                                    isLoading = true
                                                    coroutineScope.launch {
                                                        val recipient = "$countryCode$phone"
                                                        verificationService.sendOTP(
                                                            recipient,
                                                            VerificationService.VerificationType.PHONE,
                                                            context as ComponentActivity
                                                        )
                                                            .collectLatest { result ->
                                                                if (result.success) {
                                                                    otpId = result.otpId
                                                                    showOtpVerification = true
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar(
                                                                            "OTP sent to $recipient"
                                                                        )
                                                                    }
                                                                } else {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar(
                                                                            result.message
                                                                        )
                                                                    }
                                                                }
                                                                isLoading = false
                                                            }
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isLoading
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(
                                        onClick = { navController.navigate(Screen.Login.route) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            buildString {
                                                append("Already have an account? ")
                                                append("Login")
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
