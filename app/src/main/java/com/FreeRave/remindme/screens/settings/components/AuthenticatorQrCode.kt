package com.FreeRave.remindme.screens.settings.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class QrImage(val bitmap: ImageBitmap? = null, val loading: Boolean = true)

@Composable
internal fun AuthenticatorQrCode(content: String) {
    val size = with(LocalDensity.current) { 180.dp.roundToPx() }
    val image by produceState(QrImage(), content, size) {
        value = withContext(Dispatchers.Default) {
            try {
                val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size,
                    mapOf(EncodeHintType.MARGIN to 4))
                val pixels = IntArray(size * size) { index ->
                    if (matrix[index % size, index / size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
                val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
                QrImage(bitmap.asImageBitmap(), loading = false)
            } catch (_: WriterException) { QrImage(loading = false) }
            catch (_: IllegalArgumentException) { QrImage(loading = false) }
        }
    }
    val bitmap = image.bitmap
    when {
        bitmap != null -> Image(bitmap, stringResource(R.string.mfa_setup_qr),
            Modifier.background(Color.White).padding(8.dp).size(180.dp))
        image.loading -> Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> Text(stringResource(R.string.security_qr_failed), style = MaterialTheme.typography.bodySmall)
    }
}
