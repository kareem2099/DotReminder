package com.FreeRave.remindme.ui.components

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.util.WeakHashMap

private data class SecureOwners(var count: Int, val originallySecure: Boolean)
private val secureWindows = WeakHashMap<Window, SecureOwners>()

/** Nested security surfaces release only their own screen-capture protection. */
@Composable
internal fun SecureWindowEffect(enabled: Boolean) {
    val window = LocalActivity.current?.window
    DisposableEffect(window, enabled) {
        if (enabled && window != null) {
            val owners = secureWindows.getOrPut(window) {
                SecureOwners(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
            owners.count++
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (enabled && window != null) secureWindows[window]?.let { owners ->
                owners.count--
                if (owners.count == 0) {
                    if (!owners.originallySecure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    secureWindows.remove(window)
                }
            }
        }
    }
}
