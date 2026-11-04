package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R

@Composable
internal fun SecuritySettingsSection(onAccountSecurityClicked: () -> Unit, onSignOutClicked: () -> Unit, enabled: Boolean) {
    SettingsSection(stringResource(R.string.settings_account)) {
        SettingItem(stringResource(R.string.settings_security), stringResource(R.string.settings_security_hint),
            onAccountSecurityClicked, icon = Icons.Outlined.Shield)
        SettingsDivider()
        SettingItem(stringResource(R.string.settings_sign_out), stringResource(R.string.settings_sign_out_hint),
            onSignOutClicked, icon = Icons.AutoMirrored.Outlined.Logout, enabled = enabled)
    }
}
