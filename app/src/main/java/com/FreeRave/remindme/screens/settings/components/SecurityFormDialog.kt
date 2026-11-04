package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.FreeRave.remindme.R
import com.FreeRave.remindme.security.mfa.normalizeMfaCode

/** Security forms share the same adaptive surface and actions as the other app dialogs. */
@Composable
internal fun SecurityFormDialog(
    title: String, subtitle: String, confirmLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
    confirmEnabled: Boolean = true, busy: Boolean = false, error: String? = null, dismissLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) = com.FreeRave.remindme.ui.components.GlassFormDialog(
    title, confirmLabel, onConfirm, onDismiss, subtitle, confirmEnabled, busy, error,
    dismissLabel ?: stringResource(R.string.security_cancel), stringResource(R.string.security_working), content)

@Composable
internal fun VerificationCodeField(code: String, onChange: (String) -> Unit, enabled: Boolean = true) {
    OutlinedTextField(value = code, onValueChange = { onChange(normalizeMfaCode(it)) },
        modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.security_code)) },
        singleLine = true, enabled = enabled, shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
}
