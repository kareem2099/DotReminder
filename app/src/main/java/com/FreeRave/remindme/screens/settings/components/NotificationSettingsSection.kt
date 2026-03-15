package com.FreeRave.remindme.screens.settings.components

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.screens.settings.SoundOption // Corrected import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSection(
    availableSounds: List<SoundOption>,
    selectedSoundResourceName: String,
    onSoundSelected: (String) -> Unit,
    showSoundDropdown: Boolean,
    onSoundDropdownExpandedChange: (Boolean) -> Unit,
    context: Context,
    selectedSnoozeMinutes: Int,
    onSnoozeSettingClicked: () -> Unit,
    vibrationEnabled: Boolean,
    onVibrationChanged: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        ExposedDropdownMenuBox(
            expanded = showSoundDropdown,
            onExpandedChange = onSoundDropdownExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = availableSounds.find { it.resourceName == selectedSoundResourceName }?.displayName ?: "Select Sound",
                onValueChange = {},
                readOnly = true,
                label = { Text("Alarm Sound") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSoundDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = showSoundDropdown,
                onDismissRequest = { onSoundDropdownExpandedChange(false) },
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onSoundSelected("")
                        onSoundDropdownExpandedChange(false)
                    }
                )
                availableSounds.forEach { soundOption ->
                    DropdownMenuItem(
                        text = { Text(soundOption.displayName) },
                        onClick = {
                            onSoundSelected(soundOption.resourceName)
                            onSoundDropdownExpandedChange(false)
                            if (soundOption.resourceName.isNotEmpty()) {
                                try {
                                    val mediaPlayer = MediaPlayer.create(
                                        context,
                                        context.resources.getIdentifier(
                                            soundOption.resourceName,
                                            "raw",
                                            context.packageName
                                        )
                                    )
                                    mediaPlayer?.let { player ->
                                        player.start()
                                        android.os.Handler().postDelayed({
                                            player.release()
                                        }, 5000)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingItem(
            title = "Snooze Duration",
            currentValue = "$selectedSnoozeMinutes minutes",
            onClick = onSnoozeSettingClicked
        )

        SettingItemWithSwitch(
            title = "Vibrate on Alarm",
            isChecked = vibrationEnabled,
            onCheckedChange = onVibrationChanged
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
