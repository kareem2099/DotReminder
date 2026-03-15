package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.utils.PasswordStrength

@Composable
fun SignUpForm(
    // Name State
    name: String,
    onNameChange: (String) -> Unit,
    nameError: Boolean,
    nameFocusRequester: FocusRequester,
    // Email State
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: Boolean,
    emailFocusRequester: FocusRequester,
    // Phone State
    phone: String,
    onPhoneChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    phoneError: Boolean,
    phoneFocusRequester: FocusRequester,
    onSendOTP: () -> Unit,
    isLoadingOTP: Boolean,
    otpSent: Boolean,
    // Password State (passed down to PasswordInputSection)
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    passwordError: Boolean,
    passwordStrength: PasswordStrength,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibilityChange: (Boolean) -> Unit,
    confirmPasswordError: Boolean,
    passwordFocusRequester: FocusRequester,
    confirmPasswordFocusRequester: FocusRequester,
    // Focus Management
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextFieldInput(
            modifier = Modifier.focusRequester(nameFocusRequester),
            value = name,
            onValueChange = onNameChange, // Pass lambda directly
            label = "Full Name",
            placeholder = "Enter your full name",
            isError = nameError,
            supportingText = {
                if (nameError) {
                    Text("Name cannot be empty")
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() })
        )

        Spacer(modifier = Modifier.height(0.dp))

        TextFieldInput(
            modifier = Modifier.focusRequester(emailFocusRequester),
            value = email,
            onValueChange = onEmailChange, // Pass lambda directly
            label = "Email",
            placeholder = "Enter your email",
            isError = emailError,
            supportingText = {
                if (emailError) {
                    Text("Please enter a valid email")
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            // Navigate to phone code field first, then phone number
            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }) // Updated later if needed
        )

        Spacer(modifier = Modifier.height(0.dp))

        PhoneFieldWithOTP(
            phone = phone,
            onPhoneChange = onPhoneChange,
            countryCode = countryCode,
            onCountryCodeChange = onCountryCodeChange,
            phoneError = phoneError,
            phoneFocusRequester = phoneFocusRequester,
            onSendOTP = onSendOTP,
            isLoading = isLoadingOTP,
            otpSent = otpSent
        )

        Spacer(modifier = Modifier.height(0.dp))

        PasswordInputSection(
            password = password,
            onPasswordChange = onPasswordChange,
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = onPasswordVisibilityChange,
            passwordError = passwordError,
            passwordStrength = passwordStrength,
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = onConfirmPasswordChange,
            confirmPasswordVisible = confirmPasswordVisible,
            onConfirmPasswordVisibilityChange = onConfirmPasswordVisibilityChange,
            confirmPasswordError = confirmPasswordError,
            passwordFocusRequester = passwordFocusRequester,
            confirmPasswordFocusRequester = confirmPasswordFocusRequester,
            focusManager = focusManager
        )
    }
}
