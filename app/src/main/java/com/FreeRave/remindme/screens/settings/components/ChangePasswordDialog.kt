package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.settings.validatePasswordChange

@Composable
internal fun ChangePasswordDialog(
    onConfirm: (currentPassword: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit, busy: Boolean, error: String?, onInputChanged: () -> Unit
) {
    // Credentials intentionally stay in memory, outside saved instance state.
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val validation = validatePasswordChange(current, new, confirmation)
    val submit = {
        submitted = true
        if (validation.valid && !busy) onConfirm(current, new)
    }
    SecurityFormDialog(stringResource(R.string.security_password), stringResource(R.string.security_password_intro),
        stringResource(R.string.security_update_password), submit, onDismiss, busy = busy, error = error) {
        SecurityPasswordField(current, { current = it; onInputChanged() }, stringResource(R.string.security_current_password),
            if (submitted && validation.currentMissing) stringResource(R.string.security_password_required) else null, !busy)
        SecurityPasswordField(new, { new = it; onInputChanged() }, stringResource(R.string.security_new_password),
            when {
                submitted && validation.newTooShort -> stringResource(R.string.security_password_length)
                submitted && validation.unchanged -> stringResource(R.string.security_password_unchanged)
                else -> null
            }, !busy, hint = stringResource(R.string.security_password_length))
        SecurityPasswordField(confirmation, { confirmation = it; onInputChanged() }, stringResource(R.string.security_confirm_password),
            if (submitted && validation.confirmationMismatch) stringResource(R.string.security_password_mismatch) else null,
            !busy, onDone = submit)
    }
}

@Composable
private fun SecurityPasswordField(
    value: String, onChange: (String) -> Unit, label: String, error: String?, enabled: Boolean,
    hint: String? = null, onDone: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled,
        label = { Text(label) }, isError = error != null, shape = RoundedCornerShape(16.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    stringResource(if (visible) R.string.security_hide_password else R.string.security_show_password, label))
            }
        },
        supportingText = if (error != null || hint != null) { { Text(error ?: hint.orEmpty()) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (onDone == null) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }, onDone = { if (enabled) onDone?.invoke() }))
}
