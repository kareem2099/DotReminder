package com.FreeRave.remindme.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp

/** A quiet canvas for glass surfaces, retaining the user's selected accent colors. */
@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.35f
    val accent = glassAccent(colors.primary, dark)
    val background = if (dark) Color(0xFF0C121C) else Color(0xFFF3F6FA)
    val foreground = if (dark) Color(0xFFF0F4FA) else Color(0xFF172435)
    val glassColors = colors.copy(
        primary = accent,
        onPrimary = textOnAccent(accent),
        primaryContainer = lerp(background, accent, if (dark) 0.22f else 0.12f),
        onPrimaryContainer = foreground,
        secondaryContainer = lerp(background, colors.secondary, 0.12f),
        onSecondaryContainer = foreground,
        tertiaryContainer = lerp(background, accent, 0.08f),
        onTertiaryContainer = foreground,
        error = if (dark) Color(0xFFFFB4AB) else Color(0xFFB9384B),
        onError = if (dark) Color(0xFF5D1221) else Color.White,
        background = background,
        surface = if (dark) Color(0xFF1C2735) else Color.White,
        surfaceVariant = if (dark) Color(0xFF273548) else Color(0xFFE6EDF4),
        surfaceContainerLowest = if (dark) Color(0xFF090E17) else Color.White,
        surfaceContainerLow = if (dark) Color(0xFF141D29) else Color(0xFFF8FAFD),
        surfaceContainer = if (dark) Color(0xFF182331) else Color(0xFFF1F5F9),
        surfaceContainerHigh = if (dark) Color(0xFF213043) else Color(0xFFEBF0F6),
        surfaceContainerHighest = if (dark) Color(0xFF2A3A4F) else Color(0xFFE3EAF2),
        onBackground = foreground,
        onSurface = foreground,
        onSurfaceVariant = if (dark) Color(0xFFA9B8CA) else Color(0xFF536477),
        outline = if (dark) Color(0xFF8091A6) else Color(0xFF6A7B90),
        outlineVariant = if (dark) Color(0xFF3C4C60) else Color(0xFFD0DAE5)
    )
    MaterialTheme(colorScheme = glassColors, typography = AppTypography) {
        CompositionLocalProvider(LocalContentColor provides glassColors.onSurface, content = content)
    }
}

@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(colors.background)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.08f),
                    radius = size.width * 1.05f
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(colors.secondary.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(0f, size.height * 0.65f),
                    radius = size.width * 0.9f
                )
            )
        }
        content()
    }
}
