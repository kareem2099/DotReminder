package com.FreeRave.remindme.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun HomeToolbar(onNavigate: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        GlassSurface(shape = CircleShape) {
            IconButton(onClick = { onNavigate(Screen.Calendar.route) }) {
                Icon(Icons.Default.CalendarMonth, stringResource(R.string.home_calendar), modifier = Modifier.size(20.dp))
            }
        }
        Box {
            GlassSurface(shape = CircleShape) {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreHoriz, stringResource(R.string.home_menu))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                val destinations = listOf(
                    Triple(R.string.home_calendar, Icons.Default.CalendarMonth, Screen.Calendar.route),
                    Triple(R.string.home_categories, Icons.Default.Category, Screen.Categories.route),
                    Triple(R.string.home_templates, Icons.Default.GridView, Screen.Templates.route),
                    Triple(R.string.home_settings, Icons.Default.Settings, Screen.Settings.route),
                    Triple(R.string.home_sign_out, Icons.AutoMirrored.Filled.Logout, Screen.SignOut.route)
                )
                destinations.forEach { (label, icon, route) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        leadingIcon = { Icon(icon, null) },
                        onClick = { expanded = false; onNavigate(route) }
                    )
                }
            }
        }
    }
}
