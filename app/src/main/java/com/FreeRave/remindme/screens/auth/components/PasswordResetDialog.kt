package com.FreeRave.remindme.screens.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.auth.PasswordResetState
import com.FreeRave.remindme.ui.components.LoginEmailField
import com.FreeRave.remindme.utils.ValidationUtils

@Composable
internal fun PasswordResetDialog(
    initialEmail: String, state: PasswordResetState, onSend: (String) -> Unit,
    onDismiss: () -> Unit, onEdit: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val busy = state is PasswordResetState.Loading
    val valid = ValidationUtils.isValidEmail(email.trim())
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.login_reset_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.login_reset_intro), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LoginEmailField(email, { email = it; onEdit() }, enabled = !busy, error = submitted && !valid)
                if (state is PasswordResetState.Error) Text(state.message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = {
            TextButton(onClick = { submitted = true; if (valid) onSend(email.trim()) }, enabled = !busy) {
                if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                Text(stringResource(if (busy) R.string.login_reset_sending else R.string.login_reset_send))
            }
        }, dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.login_reset_cancel)) } })
}
