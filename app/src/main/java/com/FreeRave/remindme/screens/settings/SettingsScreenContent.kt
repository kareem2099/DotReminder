package com.FreeRave.remindme.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.BuildConfig
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.settings.components.*
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    state: SettingsUiState,
    soundLabel: String,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onPanel: (SettingsPanel) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onClearDefaultTime: () -> Unit,
    onSecurity: () -> Unit,
    onSignOut: () -> Unit,
    onSupport: () -> Unit,
    onAppOpenAdsChanged: (Boolean) -> Unit
) {
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(),
                    title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reminder_back))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    item(key = "intro") {
                        Text(stringResource(R.string.settings_intro), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item(key = "appearance") {
                        AppearanceSettingsSection(state.theme) { onPanel(SettingsPanel.THEME) }
                    }
                    item(key = "notifications") {
                        NotificationSettingsSection(soundLabel, state.snoozeMinutes, state.vibrationEnabled,
                            { onPanel(SettingsPanel.SOUND) }, { onPanel(SettingsPanel.SNOOZE) }, onVibrationChanged)
                    }
                    item(key = "reminders") {
                        ReminderSettingsSection(state.defaultHour, state.defaultMinute,
                            { onPanel(SettingsPanel.DEFAULT_TIME) }, onClearDefaultTime)
                    }
                    item(key = "account") {
                        SecuritySettingsSection(onSecurity, onSignOut, enabled = !state.isDeleting)
                    }
                    item(key = "support") { SupportDeveloperSection(onSupport) }
                    item(key = "ads") { AdPreferencesSection(state.appOpenAdsEnabled, onAppOpenAdsChanged) }
                    item(key = "data") {
                        DataManagementSettingsSection(state.isDeleting) { onPanel(SettingsPanel.CLEAR_REMINDERS) }
                    }
                    item(key = "version") {
                        Text(stringResource(R.string.settings_version, "DotReminder", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
