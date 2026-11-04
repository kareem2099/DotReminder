package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.PersistableBundle
import com.FreeRave.remindme.R

@Composable
internal fun TwoFactorAuthSetupDialog(
    secretKey: String, otpAuthUri: String, onVerify: (String, String) -> Unit,
    onDismiss: () -> Unit, onSecretCopied: (String) -> Unit, busy: Boolean, error: String?, onInputChanged: () -> Unit,
    shouldClearClipboard: () -> Boolean = { true }
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var openError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val copied = stringResource(R.string.mfa_setup_copied)
    DisposableEffect(secretKey) {
        onDispose {
            if (shouldClearClipboard() && clipboard.primaryClip?.getItemAt(0)?.text?.toString() == secretKey) clipboard.clearPrimaryClip()
        }
    }
    SecurityFormDialog(stringResource(R.string.mfa_setup_title), stringResource(R.string.mfa_setup_intro),
        stringResource(R.string.mfa_enable), { onVerify(code, name) }, onDismiss,
        confirmEnabled = code.length == 6, busy = busy, error = error) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AuthenticatorQrCode(otpAuthUri) }
        OutlinedButton(onClick = {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(otpAuthUri))); openError = false }
            catch (_: ActivityNotFoundException) { openError = true }
        }, enabled = !busy) { Text(stringResource(R.string.mfa_setup_open)) }
        if (openError) Text(stringResource(R.string.mfa_setup_no_app), color = MaterialTheme.colorScheme.error)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.mfa_setup_key), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(secretKey, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
                TextButton(onClick = {
                    val clip = ClipData.newPlainText("Authenticator setup", secretKey)
                    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
                    clipboard.setPrimaryClip(clip)
                    onSecretCopied(copied)
                }, enabled = !busy) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.security_copy_key))
                }
            }
        }
        Text(stringResource(R.string.mfa_setup_private), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(name, { name = it.take(40); onInputChanged() }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.mfa_setup_name)) }, singleLine = true, enabled = !busy,
            shape = RoundedCornerShape(16.dp))
        Text(stringResource(R.string.security_code_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        VerificationCodeField(code, { code = it; onInputChanged() }, enabled = !busy)
    }
}
