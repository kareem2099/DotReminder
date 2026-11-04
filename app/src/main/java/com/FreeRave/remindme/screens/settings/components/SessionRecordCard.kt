package com.FreeRave.remindme.screens.settings.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Session
import com.FreeRave.remindme.ui.components.GlassSurface
import com.google.firebase.Timestamp
import java.util.Locale

@Composable
internal fun sessionDeviceName(session: Session): String = session.deviceName.trim().ifBlank {
    session.deviceModel.trim().ifBlank { stringResource(R.string.sessions_unknown_device) }
}

@Composable
internal fun SessionRecordCard(session: Session, current: Boolean, removalEnabled: Boolean, onRemove: () -> Unit) {
    val name = sessionDeviceName(session)
    val tablet = session.deviceModel.contains("ipad", true) || session.deviceName.contains("tablet", true) ||
        Regex("\\btab\\b", RegexOption.IGNORE_CASE).containsMatchIn(session.deviceName)
    GlassSurface(Modifier.fillMaxWidth(), tint = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Icon(if (tablet) Icons.Outlined.TabletAndroid else Icons.Outlined.PhoneAndroid, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(24.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        if (current) Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(stringResource(R.string.sessions_current), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
                SessionTimestamp(stringResource(R.string.sessions_login), session.loginTime)
                SessionTimestamp(stringResource(R.string.sessions_active), session.lastActive)
            }
            if (!current) {
                SettingsDivider()
                val description = stringResource(R.string.sessions_remove_device, name)
                TextButton(onClick = onRemove, enabled = removalEnabled,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                        .semantics { contentDescription = description },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.sessions_remove))
                }
            }
        }
    }
}

@Composable
private fun SessionTimestamp(label: String, timestamp: Timestamp) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val use24Hour = DateFormat.is24HourFormat(context)
    val date = remember(locale) { DateFormat.getDateFormat(context) }
    val time = remember(locale, use24Hour) { DateFormat.getTimeFormat(context) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (timestamp.seconds <= 0) stringResource(R.string.sessions_not_recorded)
            else stringResource(R.string.sessions_timestamp, date.format(timestamp.toDate()), time.format(timestamp.toDate())),
            style = MaterialTheme.typography.bodyMedium)
    }
}
