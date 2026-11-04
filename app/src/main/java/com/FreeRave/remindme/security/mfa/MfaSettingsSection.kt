package com.FreeRave.remindme.security.mfa

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R

@Composable
internal fun MfaSettingsSection(
    state: MfaSettingsState, onAdd: () -> Unit, onRemove: (MfaFactor) -> Unit,
    onRefresh: () -> Unit, onVerifyEmail: () -> Unit, onSignIn: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val factors = state.account?.factors.orEmpty()
        Text(stringResource(when {
            state.loading -> R.string.mfa_loading
            state.account == null -> R.string.mfa_unknown
            factors.isNotEmpty() -> R.string.mfa_enabled
            else -> R.string.mfa_disabled
        }), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.mfa_intro), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        factors.forEach { factor ->
            HorizontalDivider()
            Text(factor.name, style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { onRemove(factor) }, enabled = !state.busy && !state.loading) {
                Text(stringResource(R.string.mfa_remove, factor.name))
            }
        }
        if (state.step == MfaSettingsStep.NONE) state.error?.let { error ->
            Text(stringResource(mfaErrorResource(error)), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (state.account != null && state.account.emailVerified && state.account.providers.isNotEmpty()) {
            Button(onClick = onAdd, enabled = !state.loading && !state.busy && factors.size < 5) {
                Text(stringResource(if (factors.isEmpty()) R.string.mfa_add else R.string.mfa_add_backup))
            }
        } else if (state.account != null && !state.account.emailVerified) {
            Text(stringResource(R.string.mfa_error_email), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onVerifyEmail, enabled = !state.busy) { Text(stringResource(R.string.email_verification_title)) }
        }
        if (state.error in listOf(MfaError.SIGN_IN_REQUIRED, MfaError.ACCOUNT_CHANGED)) {
            OutlinedButton(onClick = onSignIn, enabled = !state.busy) { Text(stringResource(R.string.mfa_sign_in)) }
        }
        if (state.step == MfaSettingsStep.NONE) TextButton(onClick = onRefresh, enabled = !state.busy && !state.loading) {
            Text(stringResource(R.string.mfa_refresh))
        }
        Text(stringResource(R.string.mfa_backup_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
