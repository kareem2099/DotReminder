package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.auth.LoginMethod
import com.FreeRave.remindme.screens.auth.LoginUiState

@Composable
internal fun LoginForm(
    state: LoginUiState, onEmail: (String) -> Unit, onPassword: (String) -> Unit,
    onRemember: (Boolean) -> Unit, onForgot: () -> Unit, onSubmit: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LoginEmailField(state.email, onEmail, !state.busy, state.emailError,
                onNext = { focus.moveFocus(FocusDirection.Down) })
            OutlinedTextField(state.password, onPassword, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.login_password)) }, enabled = !state.busy,
                isError = state.passwordError, shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Outlined.Lock, null, Modifier.size(20.dp)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !state.busy) {
                        Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            stringResource(if (passwordVisible) R.string.login_hide_password else R.string.login_show_password))
                    }
                },
                supportingText = if (state.passwordError) { { Text(stringResource(R.string.login_password_error)) } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!state.busy) { focus.clearFocus(); onSubmit() } }))
            Column {
                Row(Modifier.fillMaxWidth().toggleable(state.rememberEmail, enabled = !state.busy,
                    role = Role.Checkbox, onValueChange = onRemember).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(state.rememberEmail, onCheckedChange = null, enabled = !state.busy)
                    Text(stringResource(R.string.login_remember), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(start = 10.dp))
                }
                TextButton(onClick = onForgot, enabled = !state.busy, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.login_forgot))
                }
            }
            state.error?.let { message ->
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            Button(onClick = { focus.clearFocus(); onSubmit() }, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) {
                if (state.busy && state.method == LoginMethod.EMAIL) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp))
                }
                Text(stringResource(if (state.busy && state.method == LoginMethod.EMAIL) R.string.login_signing_in else R.string.login_sign_in))
            }
        }
    }
}

@Composable
internal fun LoginEmailField(
    value: String, onChange: (String) -> Unit, enabled: Boolean, error: Boolean, onNext: (() -> Unit)? = null
) {
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled,
        label = { Text(stringResource(R.string.login_email)) }, placeholder = { Text(stringResource(R.string.login_email_hint)) },
        leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null, Modifier.size(20.dp)) },
        shape = RoundedCornerShape(16.dp), isError = error,
        supportingText = if (error) { { Text(stringResource(R.string.login_email_error)) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = if (onNext == null) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext?.invoke() }))
}
