package com.FreeRave.remindme.security.lock

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.components.SecureWindowEffect
import com.FreeRave.remindme.ui.theme.GlassBackground
import com.FreeRave.remindme.ui.theme.GlassTheme

@Composable
internal fun AppLockGate(onRequireLogin: () -> Unit, content: @Composable () -> Unit) {
    val viewModel: AppLockViewModel = hiltViewModel()
    // Keep collecting while stopped so a previously rendered account never flashes on return.
    val state by viewModel.state.collectAsState()
    val activity = LocalActivity.current as? FragmentActivity
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var prompt by remember { mutableStateOf<BiometricPrompt?>(null) }
    SecureWindowEffect(state.required)
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.start()
                Lifecycle.Event.ON_STOP -> { viewModel.stop(); prompt?.cancelAuthentication(); prompt = null }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); viewModel.stop(); prompt?.cancelAuthentication() }
    }
    LaunchedEffect(state.canReveal) {
        if (!state.canReveal) { focus.clearFocus(force = true); keyboard?.hide() }
    }
    val unlock = { if (activity != null) prompt = viewModel.authenticate(activity) }
    LaunchedEffect(state.revision, lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED && state.phase == AppLockPhase.LOCKED && state.error == null) unlock()
    }
    ProtectedAppContent(state.canReveal, locked = {
        GlassTheme { AppLockScreenContent(state, unlock, { viewModel.signInAgain(onRequireLogin) }, viewModel::retryPolicy) }
    }, content = content)
    BackHandler(enabled = !state.canReveal) { activity?.moveTaskToBack(true) }
}

/** Removing the protected subtree also removes its native dialogs, focus and accessibility nodes. */
@Composable
internal fun ProtectedAppContent(unlocked: Boolean, locked: @Composable () -> Unit, content: @Composable () -> Unit) {
    val savedState = rememberSaveableStateHolder()
    if (unlocked) savedState.SaveableStateProvider("protected-navigation", content)
    else locked()
}

@Composable
internal fun AppLockScreenContent(state: AppLockState, onUnlock: () -> Unit, onSignIn: () -> Unit, onRetry: () -> Unit) {
    GlassBackground {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                GlassSurface {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Outlined.Fingerprint, null, Modifier.size(44.dp), MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.app_lock_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.app_lock_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val waiting = state.phase in listOf(AppLockPhase.CHECKING, AppLockPhase.AUTHENTICATING, AppLockPhase.SIGNING_OUT)
                        if (waiting && state.error == null) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(stringResource(if (state.phase == AppLockPhase.AUTHENTICATING) R.string.app_lock_authenticating else R.string.app_lock_checking))
                        }
                        state.error?.let { error -> Text(stringResource(when (error) {
                            AppLockError.CANCELLED -> R.string.app_lock_cancelled
                            AppLockError.UNAVAILABLE -> R.string.app_lock_unavailable
                            AppLockError.LOCKOUT -> R.string.app_lock_lockout
                            AppLockError.FAILED -> R.string.app_lock_failed
                            AppLockError.PREFERENCES -> R.string.app_lock_preferences
                            AppLockError.SIGN_OUT -> R.string.app_lock_sign_out_error
                        }), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                        if (state.phase == AppLockPhase.CHECKING && state.error != null) Button(onRetry, Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.app_lock_retry))
                        } else if (state.phase == AppLockPhase.LOCKED) Button(onUnlock, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(stringResource(R.string.app_lock_unlock))
                        }
                        OutlinedButton(onSignIn, Modifier.fillMaxWidth(), enabled = !waiting || state.error != null) {
                            Text(stringResource(R.string.app_lock_sign_in))
                        }
                    }
                }
                Text(stringResource(R.string.app_lock_fallback), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
