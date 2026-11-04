package com.FreeRave.remindme.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal fun colorContrast(a: Color, b: Color): Float {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
internal fun glassAccent(color: Color, dark: Boolean): Color {
    if (dark || colorContrast(color, Color.White) >= 5f) return color
    var low = 0f
    var high = 1f
    repeat(16) {
        val fraction = (low + high) / 2
        if (colorContrast(lerp(color, Color.Black, fraction), Color.White) >= 5f) high = fraction else low = fraction
    }
    return lerp(color, Color.Black, high)
}
internal fun textOnAccent(color: Color): Color =
    if (colorContrast(color, Color.White) >= colorContrast(color, Color.Black)) Color.White else Color.Black
