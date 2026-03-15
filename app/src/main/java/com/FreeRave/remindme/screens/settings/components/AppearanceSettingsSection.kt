package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.data.ThemeSetting

@Composable
fun AppearanceSettingsSection(
    selectedTheme: ThemeSetting,
    onThemeSettingClicked: () -> Unit
) {
    Column {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingItem(
            title = "Theme",
            currentValue = selectedTheme.value.capitalize(Locale.current),
            onClick = onThemeSettingClicked
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
