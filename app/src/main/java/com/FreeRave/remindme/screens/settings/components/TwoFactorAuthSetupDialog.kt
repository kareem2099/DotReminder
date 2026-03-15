package com.FreeRave.remindme.screens.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Added
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll // Added
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat // Moved import
import com.google.zxing.EncodeHintType // Moved import
import com.google.zxing.qrcode.QRCodeWriter // Moved import
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel // Moved import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberQrBitmapPainter(
    content: String,
    size: Int,
    padding: Int = 0
): ImageBitmap? {
    val density = LocalDensity.current.density
    val sizePx = (size * density).toInt()
    val paddingPx = (padding * density).toInt()

    return remember(content, sizePx, paddingPx) {
        if (content.isBlank()) {
            return@remember null
        }
        try {
            val qrCodeWriter = QRCodeWriter()
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L) // L, M, Q, H
                put(EncodeHintType.MARGIN, paddingPx)
            }
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorAuthSetupDialog(
    secretKey: String,
    otpAuthUri: String, // URI for QR code
    onVerify: (String) -> Unit,
    onDismiss: () -> Unit,
    onSecretCopied: (String) -> Unit
) {
    var verificationCode by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Added scrollability
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Set up Two-Factor Authentication", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Text("1. Scan the QR code with your authenticator app or manually enter the key:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // QR Code Display
                val qrBitmap = rememberQrBitmapPainter(content = otpAuthUri, size = 200) // Adjust size as needed
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "2FA QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Text(
                        "QR code generation failed. Please use manual entry.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text("Secret Key:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(secretKey))
                            onSecretCopied("Secret key copied to clipboard!")
                        }
                ) {
                    Text(
                        text = secretKey,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy secret key",
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("2. Enter the 6-digit code from your app:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { if (it.length <= 6) verificationCode = it.filter { char -> char.isDigit() } },
                    label = { Text("Verification Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onVerify(verificationCode) },
                        enabled = verificationCode.length == 6
                    ) {
                        Text("Verify & Enable")
                    }
                }
            }
        }
    }
}
