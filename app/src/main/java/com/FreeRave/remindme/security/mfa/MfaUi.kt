package com.FreeRave.remindme.security.mfa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.settings.components.SecurityFormDialog
import com.FreeRave.remindme.screens.settings.components.VerificationCodeField

internal fun mfaErrorResource(error: MfaError): Int = when (error) {
    MfaError.NETWORK -> R.string.mfa_error_network
    MfaError.RATE_LIMIT -> R.string.mfa_error_rate_limit
    MfaError.INVALID_CODE -> R.string.mfa_error_code
    MfaError.EXPIRED -> R.string.mfa_error_expired
    MfaError.RECENT_LOGIN -> R.string.mfa_error_recent
    MfaError.EMAIL_UNVERIFIED -> R.string.mfa_error_email
    MfaError.UNSUPPORTED -> R.string.mfa_error_unsupported
    MfaError.NOT_CONFIGURED -> R.string.mfa_error_config
    MfaError.ACCOUNT_CHANGED -> R.string.mfa_error_changed
    MfaError.SIGN_IN_REQUIRED -> R.string.mfa_error_session
    MfaError.CREDENTIALS -> R.string.mfa_error_credentials
    MfaError.LIMIT -> R.string.mfa_error_limit
    MfaError.GENERIC -> R.string.mfa_error_generic
}

@Composable
internal fun MfaChallengeDialog(
    state: MfaChallengeState, onVerify: (String) -> Unit, onSelect: (String) -> Unit,
    onEdit: () -> Unit, onDismiss: () -> Unit
) {
    var code by remember(state.selectedId) { mutableStateOf("") }
    SecurityFormDialog(stringResource(R.string.mfa_challenge_title), stringResource(R.string.mfa_challenge_intro),
        stringResource(R.string.mfa_verify), { onVerify(code) }, onDismiss,
        confirmEnabled = validMfaCode(code) && state.selectedId != null, busy = state.busy,
        error = state.error?.let { stringResource(mfaErrorResource(it)) }) {
        state.factors.forEach { factor ->
            FilterChip(selected = factor.id == state.selectedId, onClick = { onSelect(factor.id) }, enabled = !state.busy,
                label = { Text(factor.name) })
        }
        VerificationCodeField(code, { code = it; onEdit() }, enabled = !state.busy)
        Text(stringResource(R.string.mfa_help), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun MfaReauthenticationDialog(
    state: MfaSettingsState, onConfirm: (MfaProvider, String) -> Unit, onEdit: () -> Unit, onDismiss: () -> Unit
) {
    val providers = state.account?.providers.orEmpty()
    var selected by remember(providers) { mutableStateOf(providers.firstOrNull()) }
    var password by remember { mutableStateOf("") }
    val removal = state.action as? MfaAction.Remove
    SecurityFormDialog(stringResource(if (removal == null) R.string.mfa_reauth_title else R.string.mfa_remove_title),
        if (removal == null) stringResource(R.string.mfa_reauth_intro) else stringResource(R.string.mfa_remove_intro, removal.factor.name),
        stringResource(R.string.mfa_continue), { selected?.let { onConfirm(it, password); password = "" } }, onDismiss,
        confirmEnabled = selected != null && (selected != MfaProvider.PASSWORD || password.isNotBlank()), busy = state.busy,
        error = state.error?.let { stringResource(mfaErrorResource(it)) }) {
        if (removal != null && state.account?.factors?.size == 1) Text(stringResource(R.string.mfa_remove_last),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            providers.forEach { provider ->
                FilterChip(selected = selected == provider, onClick = { selected = provider; password = ""; onEdit() },
                    enabled = !state.busy, label = { Text(stringResource(when (provider) {
                        MfaProvider.PASSWORD -> R.string.mfa_provider_password
                        MfaProvider.GOOGLE -> R.string.mfa_provider_google
                        MfaProvider.FACEBOOK -> R.string.mfa_provider_facebook
                    })) })
            }
        }
        if (selected == MfaProvider.PASSWORD) OutlinedTextField(password, { password = it; onEdit() },
            Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.mfa_password)) }, enabled = !state.busy,
            visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
    }
}
