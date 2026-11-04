package com.FreeRave.remindme.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.components.SignUpBranding
import com.FreeRave.remindme.ui.components.SignUpForm
import com.FreeRave.remindme.ui.theme.GlassBackground

@Composable
internal fun SignUpScreenContent(
    state: SignUpUiState, onEdit: (SignUpField, String) -> Unit, onSend: () -> Unit,
    onCreate: () -> Unit, onEditDetails: () -> Unit, onRetry: () -> Unit,
    onLogin: () -> Unit, onContinue: () -> Unit,
    onResend: () -> Unit = {}, onCancelSend: () -> Unit = {}
) {
    val focus = LocalFocusManager.current
    val editable = state.step == SignUpStep.DETAILS || state.step == SignUpStep.CODE
    GlassBackground {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (editable) {
                        TextButton(onClick = { if (state.step == SignUpStep.CODE) onEditDetails() else onLogin() },
                            enabled = !state.busy, modifier = Modifier.align(Alignment.Start)) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (state.step == SignUpStep.CODE) R.string.signup_details else R.string.signup_back))
                        }
                    }
                    SignUpBranding()
                    if (editable) {
                        Text(stringResource(if (state.step == SignUpStep.DETAILS) R.string.signup_step_details else R.string.signup_step_code),
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    when (state.step) {
                        SignUpStep.DETAILS -> SignUpForm(state, onEdit, onSend)
                        SignUpStep.CODE -> SignUpCodeCard(state, onEdit, onCreate, onEditDetails, onResend)
                        SignUpStep.PROFILE -> SignUpStatusCard(R.string.signup_profile_title, R.string.signup_profile_intro)
                        SignUpStep.COMPLETE -> SignUpStatusCard(R.string.signup_complete, R.string.signup_complete_intro, complete = true)
                    }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    val action = when (state.step) {
                        SignUpStep.DETAILS -> onSend
                        SignUpStep.CODE -> onCreate
                        SignUpStep.PROFILE -> onRetry
                        SignUpStep.COMPLETE -> onContinue
                    }
                    val label = when (state.step) {
                        SignUpStep.DETAILS -> if (state.busy) R.string.signup_sending else if (state.resendSeconds > 0) R.string.signup_send_wait else R.string.signup_send
                        SignUpStep.CODE -> if (state.sendingCode) R.string.signup_sending else if (state.busy) R.string.signup_creating
                            else if (state.phoneVerified) R.string.signup_create_verified else R.string.signup_create
                        SignUpStep.PROFILE -> if (state.busy) R.string.signup_saving else R.string.signup_profile_retry
                        SignUpStep.COMPLETE -> R.string.signup_continue
                    }
                    Button(onClick = { focus.clearFocus(); action() }, enabled = !state.busy && (state.step != SignUpStep.DETAILS || state.resendSeconds == 0),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) {
                        if (state.busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                        Text(if (label == R.string.signup_send_wait) stringResource(label, state.resendSeconds) else stringResource(label), textAlign = TextAlign.Center)
                    }
                    if (state.sendingCode) TextButton(onClick = onCancelSend) { Text(stringResource(R.string.signup_cancel_send)) }
                    if (editable) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.signup_have_account), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = onLogin, enabled = !state.busy) { Text(stringResource(R.string.signup_sign_in)) }
                        }
                    }
                    com.FreeRave.remindme.ui.components.PrivacyPolicyLink()
                }
            }
        }
    }
}

@Composable
private fun SignUpCodeCard(state: SignUpUiState, onEdit: (SignUpField, String) -> Unit,
    onCreate: () -> Unit, onEditDetails: () -> Unit, onResend: () -> Unit) {
    val focus = LocalFocusManager.current
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.Sms, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(if (state.phoneVerified) R.string.signup_phone_verified else R.string.signup_code_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (state.phoneVerified) R.string.signup_phone_verified_intro else R.string.signup_code_intro, state.recipient),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val error = state.submitted && !validSignUpCode(state.code)
            if (!state.phoneVerified) OutlinedTextField(state.code, { onEdit(SignUpField.CODE, it) }, Modifier.fillMaxWidth(),
                enabled = !state.busy, singleLine = true, shape = RoundedCornerShape(16.dp),
                label = { Text(stringResource(R.string.signup_code)) }, isError = error,
                supportingText = if (error) {{ Text(stringResource(R.string.signup_code_error)) }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!state.busy) { focus.clearFocus(); onCreate() } }))
            if (!state.phoneVerified) TextButton(onClick = onResend, enabled = state.canResend) {
                Text(if (state.resendSeconds > 0) stringResource(R.string.signup_resend_wait, state.resendSeconds) else stringResource(R.string.signup_resend))
            }
            TextButton(onClick = onEditDetails, enabled = !state.busy) { Text(stringResource(R.string.signup_edit)) }
        }
    }
}

@Composable
private fun SignUpStatusCard(title: Int, message: Int, complete: Boolean = false) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (complete) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(44.dp), MaterialTheme.colorScheme.primary)
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
