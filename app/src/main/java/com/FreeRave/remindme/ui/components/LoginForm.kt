package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: Boolean,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    rememberMeChecked: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(
            value = email,
            onValueChange = {
                onEmailChange(it)
                // Email validation logic will remain in the ViewModel or screen-level state
            },
            isError = emailError,
            label = { Text("Email") },
            supportingText = { if (emailError) Text("Please enter a valid email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = {
                onPasswordChange(it)
                // Password validation logic will remain in the ViewModel or screen-level state
            },
            isError = passwordError,
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            supportingText = { if (passwordError) Text("Password must be at least 8 characters") },
            trailingIcon = {
                IconButton(onClick = { onPasswordVisibilityChange() }) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("file:///android_asset/svg/${
                                if (passwordVisible) "visibility_off.svg" else "visibility.svg"
                            }")
                            .decoderFactory(SvgDecoder.Factory())
                            .build(),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Forgot Password?",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onForgotPasswordClick() }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start // Align checkbox and text to the start
        ) {
            Checkbox(
                checked = rememberMeChecked,
                onCheckedChange = onRememberMeChange
            )
            Text(
                text = "Remember Me",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onRememberMeChange(!rememberMeChecked) } // Allow clicking text
            )
        }
    }
}
