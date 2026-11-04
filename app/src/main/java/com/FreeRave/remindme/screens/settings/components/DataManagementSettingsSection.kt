package com.FreeRave.remindme.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.FreeRave.remindme.R

@Composable
fun DataManagementSettingsSection(isDeleting: Boolean, onClearAllRemindersClicked: () -> Unit) {
    SettingsSection(stringResource(R.string.settings_data)) {
        SettingItem(stringResource(R.string.settings_clear_reminders),
            stringResource(if (isDeleting) R.string.settings_deleting else R.string.settings_clear_reminders_hint),
            onClearAllRemindersClicked, icon = Icons.Outlined.DeleteOutline, enabled = !isDeleting, destructive = true, busy = isDeleting)
    }
}
