package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.FreeRave.remindme.utils.PasswordStrength
import com.FreeRave.remindme.utils.ValidationUtils
import com.FreeRave.remindme.utils.displayText
import com.FreeRave.remindme.utils.getColor
import com.FreeRave.remindme.utils.PasswordRequirements

@Composable
fun PasswordInputSection(
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
    focusManager: FocusManager, // Pass FocusManager for the final 'Done' action
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        TextFieldInput(
            modifier = Modifier.focusRequester(passwordFocusRequester),
            value = password,
            onValueChange = {
                onPasswordChange(it) // Let the caller handle state and strength calculation
            },
            label = "Password",
            placeholder = "Enter your password (min 8 chars)",
            isError = passwordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                     // Using AsyncImage for SVG as before
                     AsyncImage(
                         model = ImageRequest.Builder(context)
                             .data("file:///android_asset/svg/${if (passwordVisible) "visibility_off.svg" else "visibility.svg"}")
                             .decoderFactory(SvgDecoder.Factory())
                             .build(),
                         contentDescription = if (passwordVisible) "Hide password" else "Show password"
                     )
                    /* Alternative using drawable resources if you switch from SVG:
                    Icon(
                        painter = painterResource(id = if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                    */
                }
            },
            supportingText = {
                Column {
                    if (passwordStrength != PasswordStrength.NONE) {
                        Text(
                            text = passwordStrength.displayText(),
                            color = passwordStrength.getColor(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        val requirements = ValidationUtils.checkPasswordRequirements(password)
                        Text(text = "${if (requirements.minLength) "✓" else "✗"} At least 8 characters", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${if (requirements.hasLowerCase) "✓" else "✗"} Lowercase letter", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${if (requirements.hasUpperCase) "✓" else "✗"} Uppercase letter", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${if (requirements.hasDigit) "✓" else "✗"} Number", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${if (requirements.hasSpecialChar) "✓" else "✗"} Special character", style = MaterialTheme.typography.bodySmall)
                    }
                    if (passwordError) {
                        val errorText = when {
                            password.isEmpty() -> "Password cannot be empty." // Handle empty case if needed
                            passwordStrength == PasswordStrength.TOO_SHORT -> "Password is too short (min 8 chars)."
                            !ValidationUtils.isValidPassword(password) -> "Password must contain uppercase, lowercase, digit, and special character."
                            else -> "Password does not meet requirements." // Generic fallback
                        }
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { confirmPasswordFocusRequester.requestFocus() })
        )

        Spacer(modifier = Modifier.height(0.dp)) // Adjust spacing as needed

        TextFieldInput(
            modifier = Modifier.focusRequester(confirmPasswordFocusRequester),
            value = confirmPassword,
            onValueChange = {
                onConfirmPasswordChange(it) // Let the caller handle state and error check
            },
            label = "Confirm Password",
            placeholder = "Re-enter your password",
            isError = confirmPasswordError,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { onConfirmPasswordVisibilityChange(!confirmPasswordVisible) }) {
                    // Using AsyncImage for SVG as before
                     AsyncImage(
                         model = ImageRequest.Builder(context)
                             .data("file:///android_asset/svg/${if (confirmPasswordVisible) "visibility_off.svg" else "visibility.svg"}")
                             .decoderFactory(SvgDecoder.Factory())
                             .build(),
                         contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                     )
                    /* Alternative using drawable resources:
                    Icon(
                        painter = painterResource(id = if (confirmPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                    )
                    */
                }
            },
            supportingText = {
                if (confirmPasswordError) {
                    Text("Passwords do not match")
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}
