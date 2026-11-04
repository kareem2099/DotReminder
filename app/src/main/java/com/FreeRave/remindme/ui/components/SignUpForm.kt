package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.auth.*
import com.FreeRave.remindme.screens.auth.components.SignUpPhoneFields
import com.FreeRave.remindme.utils.ValidationUtils
import com.FreeRave.remindme.utils.displayText
import com.FreeRave.remindme.utils.getColor

@Composable
internal fun SignUpForm(state: SignUpUiState, onEdit: (SignUpField, String) -> Unit, onSubmit: () -> Unit) {
    val validation = validateSignUp(state)
    val focus = LocalFocusManager.current
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.signup_details), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(state.name, { onEdit(SignUpField.NAME, it) }, Modifier.fillMaxWidth(),
                enabled = !state.busy, singleLine = true, shape = RoundedCornerShape(16.dp),
                label = { Text(stringResource(R.string.signup_name)) },
                leadingIcon = { Icon(Icons.Outlined.PersonOutline, null, Modifier.size(20.dp)) },
                isError = state.submitted && validation.nameInvalid,
                supportingText = if (state.submitted && validation.nameInvalid) {{ Text(stringResource(R.string.signup_name_error)) }} else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
            LoginEmailField(state.email, { onEdit(SignUpField.EMAIL, it) }, !state.busy,
                state.submitted && validation.emailInvalid, onNext = { focus.moveFocus(FocusDirection.Down) })
            SignUpPhoneFields(state, onEdit, state.submitted && validation.phoneInvalid)
        }
    }
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SignUpPasswordField(state.password, { onEdit(SignUpField.PASSWORD, it) }, R.string.login_password,
                state.submitted && validation.passwordInvalid, R.string.signup_password_error, !state.busy,
                onNext = { focus.moveFocus(FocusDirection.Down) })
            if (state.password.isNotEmpty()) {
                val strength = ValidationUtils.calculatePasswordStrength(state.password)
                Text(strength.displayText(), color = strength.getColor(), style = MaterialTheme.typography.labelMedium)
            }
            Text(stringResource(R.string.signup_password_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SignUpPasswordField(state.confirmation, { onEdit(SignUpField.CONFIRMATION, it) }, R.string.signup_confirm,
                state.submitted && validation.confirmationInvalid, R.string.signup_confirm_error, !state.busy,
                onDone = { focus.clearFocus(); onSubmit() })
        }
    }
}

@Composable
private fun SignUpPasswordField(
    value: String, onChange: (String) -> Unit, label: Int, error: Boolean, errorText: Int, enabled: Boolean,
    onNext: (() -> Unit)? = null, onDone: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), enabled = enabled, singleLine = true,
        shape = RoundedCornerShape(16.dp), label = { Text(stringResource(label)) }, isError = error,
        supportingText = if (error) {{ Text(stringResource(errorText)) }} else null,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    stringResource(if (visible) R.string.login_hide_password else R.string.login_show_password))
            }
        }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
            imeAction = if (onNext != null) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(onNext = { onNext?.invoke() }, onDone = { if (enabled) onDone?.invoke() }))
}
