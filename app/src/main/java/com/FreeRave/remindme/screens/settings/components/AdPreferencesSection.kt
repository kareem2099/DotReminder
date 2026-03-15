package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdPreferencesSection(
    appOpenAdsEnabled: Boolean,
    onAppOpenAdsToggle: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Ad Preferences",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        SettingItemWithSwitch(
            title = "Enable App-Open Ads",
            isChecked = appOpenAdsEnabled,
            onCheckedChange = onAppOpenAdsToggle
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
