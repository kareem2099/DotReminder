package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R

@Composable
fun SupportDeveloperSection(onSupportClicked: () -> Unit) {
    SettingsSection(stringResource(R.string.settings_support)) {
        SettingItem(stringResource(R.string.settings_support_developer), stringResource(R.string.settings_support_hint),
            onSupportClicked, icon = Icons.Outlined.FavoriteBorder)
    }
}
