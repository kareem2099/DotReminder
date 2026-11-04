package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.settings.*
import com.FreeRave.remindme.screens.settings.components.*

@Composable
internal fun ThemeBottomSheet(
    showSheet: Boolean, availableThemes: List<ThemeOption>, selectedTheme: ThemeSetting,
    progress: ThemeUnlockProgress, onThemeSelected: (ThemeSetting) -> Unit,
    onReward: (ThemeSetting) -> Unit, onDismiss: () -> Unit
) {
    if (!showSheet) return
    val context = LocalContext.current
    val rewardCallback by rememberUpdatedState(onReward)
    val selectCallback by rememberUpdatedState(onThemeSelected)
    val ads = remember(context) { ThemeRewardedAdController(context, { rewardCallback(it) }, { selectCallback(it) }) }
    DisposableEffect(ads) { onDispose { ads.dispose() } }
    val dismiss = { ads.reset(); onDismiss() }
    SettingsChoiceSheet(stringResource(R.string.settings_choose_theme), stringResource(R.string.settings_theme_hint), dismiss) {
        availableThemes.forEach { option ->
            val theme = option.setting
            val unlocked = progress.unlocked(theme)
            SettingsChoiceRow(
                title = stringResource(theme.titleResource), selected = selectedTheme == theme,
                subtitle = if (unlocked) "" else stringResource(R.string.settings_theme_locked, progress.watched(theme), REQUIRED_THEME_ADS),
                enabled = !ads.busy,
                onClick = {
                    if (unlocked) { onThemeSelected(theme); dismiss() }
                    else ads.load(theme, progress.watched(theme))
                },
                leading = { ThemeSwatch(theme) },
                trailing = {
                    if (!unlocked) Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                    else RadioButton(selected = selectedTheme == theme, onClick = null, enabled = !ads.busy)
                }
            )
        }
    }
    ThemeAdDialog(ads.status, onReset = ads::reset, onDismiss = dismiss)
}

@Composable
private fun ThemeAdDialog(status: ThemeAdStatus, onReset: () -> Unit, onDismiss: () -> Unit) {
    when (status) {
        ThemeAdStatus.Idle, ThemeAdStatus.Showing -> Unit
        ThemeAdStatus.Loading -> AlertDialog(
            onDismissRequest = onReset,
            title = { Text(stringResource(R.string.settings_theme_loading_ad)) },
            text = { LinearProgressIndicator() },
            confirmButton = { TextButton(onClick = onReset) { Text(stringResource(R.string.settings_cancel)) } }
        )
        ThemeAdStatus.Error -> AlertDialog(
            onDismissRequest = onReset,
            title = { Text(stringResource(R.string.settings_theme_ad_error_title)) },
            text = { Text(stringResource(R.string.settings_theme_ad_error)) },
            confirmButton = { TextButton(onClick = onReset) { Text(stringResource(R.string.settings_done)) } }
        )
        is ThemeAdStatus.Rewarded -> {
            val unlocked = status.count >= REQUIRED_THEME_ADS
            val close = if (unlocked) onDismiss else onReset
            AlertDialog(onDismissRequest = close,
                title = { Text(stringResource(if (unlocked) R.string.settings_theme_unlocked_title else R.string.settings_theme_progress_title)) },
                text = {
                    val name = stringResource(status.theme.titleResource)
                    Text(if (unlocked) stringResource(R.string.settings_theme_unlocked, name)
                        else stringResource(R.string.settings_theme_progress, status.count, REQUIRED_THEME_ADS, name))
                },
                confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.settings_done)) } })
        }
    }
}

@Composable
private fun ThemeSwatch(theme: ThemeSetting) {
    val colors = when (theme) {
        ThemeSetting.LIGHT -> listOf(Color(0xFFE4EAF3), Color.White)
        ThemeSetting.DARK -> listOf(Color(0xFF1A2433), Color(0xFF536582))
        ThemeSetting.SYSTEM -> listOf(Color(0xFFE4EAF3), Color(0xFF29394E))
        ThemeSetting.OCEAN_BLUE -> listOf(Color(0xFF127FA6), Color(0xFF67D3DF))
        ThemeSetting.FOREST_GREEN -> listOf(Color(0xFF286651), Color(0xFF8DC99A))
        ThemeSetting.SUNSET_ORANGE -> listOf(Color(0xFFDD7946), Color(0xFFF7C383))
        ThemeSetting.MIDNIGHT_PURPLE -> listOf(Color(0xFF663FA2), Color(0xFFB59ADB))
        ThemeSetting.CANDY -> listOf(Color(0xFFD66A98), Color(0xFFF5B9D5))
    }
    Box(Modifier.size(38.dp).background(Brush.linearGradient(colors), RoundedCornerShape(12.dp)))
}
