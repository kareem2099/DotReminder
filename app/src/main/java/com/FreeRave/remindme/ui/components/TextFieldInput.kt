package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun TextFieldInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isPasswordField: Boolean = false,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Toggle state for password visibility
    var passwordVisible by remember { mutableStateOf(false) }

    val visibilityIcon = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data("file:///android_asset/svg/visibility.svg")
            .decoderFactory(SvgDecoder.Factory())
            .build()
    )

    val visibilityOffIcon = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data("file:///android_asset/svg/visibility_off.svg")
            .decoderFactory(SvgDecoder.Factory())
            .build()
    )

    val currentTransformation = if (isPasswordField && !passwordVisible) {
        PasswordVisualTransformation()
    } else {
        visualTransformation
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = TextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            errorIndicatorColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error
        ),
        visualTransformation = currentTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        isError = isError,
        supportingText = supportingText,
        trailingIcon = { trailingIcon?.invoke() ?: run {
            if (isPasswordField) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = if (passwordVisible) visibilityOffIcon else visibilityIcon,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.Unspecified 
                    )
                }
            }
            }
        },
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine
    )
}
