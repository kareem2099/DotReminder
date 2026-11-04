package com.FreeRave.remindme.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
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
internal fun SignOutScreenContent(state: SignOutUiState, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val complete = state.status == SignOutStatus.COMPLETE
    GlassBackground {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(), title = { Text(stringResource(R.string.signout_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onCancel, enabled = !state.busy) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.signout_back))
                            }
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassSurface(shape = RoundedCornerShape(28.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(if (complete) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.Logout, null,
                            Modifier.padding(24.dp).size(42.dp), MaterialTheme.colorScheme.primary)
                    }
                    Text(stringResource(if (complete) R.string.signout_complete else R.string.signout_heading),
                        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(stringResource(if (complete) R.string.signout_redirect else R.string.signout_intro),
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                    if (complete) CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                    else {
                        state.account?.let { SignOutAccountCard(it) }
                        SignOutSavedNotice()
                        if (state.status == SignOutStatus.ERROR) {
                            Text(stringResource(R.string.signout_error), color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
                        }
                        Button(onClick = onConfirm, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError)) {
                            if (state.busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                            Text(stringResource(when (state.status) {
                                SignOutStatus.BUSY -> R.string.signout_loading
                                SignOutStatus.ERROR -> R.string.signout_retry
                                else -> R.string.signout_confirm
                            }), textAlign = TextAlign.Center)
                        }
                        OutlinedButton(onClick = onCancel, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) {
                            Text(stringResource(R.string.signout_cancel), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignOutAccountCard(account: SignOutAccount) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountCircle, null, Modifier.size(30.dp), MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.signout_account), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(account.name.takeIf { it.isNotBlank() } ?: account.email.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.signout_account_fallback), style = MaterialTheme.typography.titleMedium)
                if (account.name.isNotBlank() && account.email.isNotBlank()) {
                    Text(account.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SignOutSavedNotice() {
    GlassSurface(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.CloudDone, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.signout_saved_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.signout_saved_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
