package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.auth.LoginMethod
import com.FreeRave.remindme.screens.auth.LoginUiState

@Composable
internal fun LoginActions(state: LoginUiState, onGoogle: () -> Unit, onFacebook: () -> Unit, onBiometric: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GoogleSignInButton(onGoogle, Modifier.fillMaxWidth(), enabled = !state.busy,
            loading = state.busy && state.method == LoginMethod.GOOGLE)
        FacebookSignInButton(onFacebook, enabled = !state.busy,
            loading = state.busy && state.method == LoginMethod.FACEBOOK)
        if (state.showBiometrics) {
            OutlinedButton(onClick = onBiometric, enabled = !state.busy && state.biometricReady,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Outlined.Fingerprint, null, Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(if (state.busy && state.method == LoginMethod.BIOMETRIC) R.string.login_biometric_busy else R.string.login_biometric))
            }
            if (!state.biometricReady) Text(stringResource(R.string.login_biometric_session_required),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
