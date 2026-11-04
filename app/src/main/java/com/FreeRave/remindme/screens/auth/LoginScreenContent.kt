package com.FreeRave.remindme.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.LoginActions
import com.FreeRave.remindme.ui.components.LoginBranding
import com.FreeRave.remindme.ui.components.LoginForm
import com.FreeRave.remindme.ui.components.SignUpNavigationLink
import com.FreeRave.remindme.ui.theme.GlassBackground

@Composable
internal fun LoginScreenContent(
    state: LoginUiState, snackbar: SnackbarHostState,
    onEmail: (String) -> Unit, onPassword: (String) -> Unit, onRemember: (Boolean) -> Unit,
    onSubmit: () -> Unit, onForgot: () -> Unit, onGoogle: () -> Unit,
    onFacebook: () -> Unit, onBiometric: () -> Unit, onSignUp: () -> Unit
) {
    GlassBackground {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    LoginBranding()
                    LoginForm(state, onEmail, onPassword, onRemember, onForgot, onSubmit)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(stringResource(R.string.login_or), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    LoginActions(state, onGoogle, onFacebook, onBiometric)
                    SignUpNavigationLink(onSignUp, enabled = !state.busy)
                    com.FreeRave.remindme.ui.components.PrivacyPolicyLink()
                }
            }
        }
    }
}
