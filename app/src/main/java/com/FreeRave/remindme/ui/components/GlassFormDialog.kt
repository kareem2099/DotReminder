package com.FreeRave.remindme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.FreeRave.remindme.R

/** One scrollable form and wrapping actions keep dialogs usable with enlarged text and the IME. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GlassFormDialog(
    title: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    subtitle: String? = null, confirmEnabled: Boolean = true, busy: Boolean = false,
    error: String? = null, dismissLabel: String? = null, busyLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = !busy)) {
        Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp)
            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f),
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                content()
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, enabled = !busy) { Text(dismissLabel ?: stringResource(R.string.reminder_cancel)) }
                    Button(onClick = onConfirm, enabled = confirmEnabled && !busy, shape = RoundedCornerShape(14.dp)) {
                        if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                        Text(if (busy) busyLabel ?: stringResource(R.string.reminder_saving) else confirmLabel)
                    }
                }
            }
        }
    }
}
