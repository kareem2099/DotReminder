package com.FreeRave.remindme.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VerifyEmailScreenContent(
    state: EmailVerificationUiState, onBack: () -> Unit, onCheck: () -> Unit, onSend: () -> Unit,
    onRetry: () -> Unit, onRetrySync: () -> Unit, onContinue: () -> Unit, onLogin: () -> Unit
) {
    GlassBackground {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(), title = { Text(stringResource(R.string.email_verification_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.email_verification_back))
                            }
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    VerificationHeading(state)
                    state.account?.email?.takeIf { it.isNotBlank() }?.let { email ->
                        GlassSurface(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.email_verification_address), style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SelectionContainer { Text(email, style = MaterialTheme.typography.titleMedium) }
                            }
                        }
                    }
                    if (state.phase == EmailVerificationPhase.READY) {
                        Text(stringResource(R.string.email_verification_tip), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        val notice = when (state.notice) {
                            EmailVerificationNotice.SENT -> R.string.email_verification_sent
                            EmailVerificationNotice.PENDING -> R.string.email_verification_pending
                            null -> null
                        }
                        notice?.let { VerificationNotice(stringResource(it)) }
                    }
                    state.error?.let {
                        Text(stringResource(verificationErrorLabel(it)), color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    VerificationActions(state, onCheck, onSend, onRetry, onRetrySync, onContinue, onLogin, onBack)
                }
            }
        }
    }
}

@Composable
private fun VerificationHeading(state: EmailVerificationUiState) {
    val verified = state.phase == EmailVerificationPhase.VERIFIED
    val (title, subtitle) = when (state.phase) {
        EmailVerificationPhase.INITIAL -> R.string.email_verification_checking to null
        EmailVerificationPhase.READY -> R.string.email_verification_heading to R.string.email_verification_intro
        EmailVerificationPhase.VERIFIED -> R.string.email_verification_done_title to R.string.email_verification_done_intro
        EmailVerificationPhase.SIGNED_OUT -> R.string.email_verification_signed_out_title to R.string.email_verification_signed_out_intro
        EmailVerificationPhase.NO_EMAIL -> R.string.email_verification_no_email_title to R.string.email_verification_no_email_intro
        EmailVerificationPhase.LOAD_ERROR -> R.string.email_verification_load_error_title to R.string.email_verification_load_error_intro
    }
    GlassSurface(shape = RoundedCornerShape(28.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
        Icon(if (verified) Icons.Outlined.MarkEmailRead else Icons.Outlined.MarkEmailUnread, null,
            Modifier.padding(24.dp).size(42.dp), tint = MaterialTheme.colorScheme.primary)
    }
    Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    subtitle?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
}

@Composable
private fun VerificationNotice(message: String) {
    GlassSurface(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.primaryContainer) {
        Text(message, Modifier.padding(18.dp).semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun VerificationActions(
    state: EmailVerificationUiState, onCheck: () -> Unit, onSend: () -> Unit, onRetry: () -> Unit,
    onRetrySync: () -> Unit, onContinue: () -> Unit, onLogin: () -> Unit, onBack: () -> Unit
) {
    when (state.phase) {
        EmailVerificationPhase.INITIAL -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
        EmailVerificationPhase.READY -> {
            val sendLabel = when {
                state.operation == EmailVerificationOperation.SEND -> stringResource(R.string.email_verification_sending)
                state.resendSeconds > 0 -> stringResource(R.string.email_verification_resend_wait, state.resendSeconds)
                state.hasSentLink -> stringResource(R.string.email_verification_resend)
                else -> stringResource(R.string.email_verification_send)
            }
            VerificationButton(sendLabel, onSend, state.canSend, state.operation == EmailVerificationOperation.SEND)
            OutlinedButton(onClick = onCheck, enabled = state.canCheck,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) {
                if (state.operation == EmailVerificationOperation.CHECK) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp))
                }
                Text(stringResource(if (state.operation == EmailVerificationOperation.CHECK) R.string.email_verification_checking else R.string.email_verification_check),
                    textAlign = TextAlign.Center)
            }
        }
        EmailVerificationPhase.VERIFIED -> {
            if (state.error == EmailVerificationError.SYNC || state.operation == EmailVerificationOperation.SYNC) {
                VerificationButton(stringResource(if (state.busy) R.string.email_verification_syncing else R.string.email_verification_sync_retry),
                    onRetrySync, !state.busy, state.busy)
            }
            VerificationButton(stringResource(R.string.email_verification_continue), onContinue, !state.busy)
        }
        EmailVerificationPhase.SIGNED_OUT -> VerificationButton(stringResource(R.string.login_sign_in), onLogin, !state.busy)
        EmailVerificationPhase.NO_EMAIL -> VerificationButton(stringResource(R.string.email_verification_back), onBack, !state.busy)
        EmailVerificationPhase.LOAD_ERROR -> VerificationButton(stringResource(R.string.email_verification_retry), onRetry, !state.busy, state.busy)
    }
}

@Composable
private fun VerificationButton(text: String, action: () -> Unit, enabled: Boolean, busy: Boolean = false) {
    Button(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) {
        if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
        Text(text, textAlign = TextAlign.Center)
    }
}

private fun verificationErrorLabel(error: EmailVerificationError) = when (error) {
    EmailVerificationError.NETWORK -> R.string.email_verification_error_network
    EmailVerificationError.RATE_LIMIT -> R.string.email_verification_error_rate_limit
    EmailVerificationError.CHECK -> R.string.email_verification_error_check
    EmailVerificationError.SEND -> R.string.email_verification_error_send
    EmailVerificationError.SYNC -> R.string.email_verification_error_sync
    EmailVerificationError.ACCOUNT_CHANGED -> R.string.email_verification_error_account_changed
}
