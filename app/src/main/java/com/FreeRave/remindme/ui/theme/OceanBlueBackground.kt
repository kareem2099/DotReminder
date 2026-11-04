package com.FreeRave.remindme.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.FreeRave.remindme.data.ThemeSetting

// Premium Ocean Blue gradient backgrounds with radial effects
val OceanBlueLightGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFE8F4FD), // Pearl white-blue
        Color(0xFFB3E5FC), // Light sky blue
        Color(0xFF81D4FA), // Medium sky blue
        Color(0xFF4FC3F7), // Bright blue
        Color(0xFF29B6F6), // Vibrant blue
        Color(0xFFE8F4FD)  // Back to pearl
    ),
    center = androidx.compose.ui.geometry.Offset(0.3f, 0.3f),
    radius = 800f
)

val OceanBlueLightOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x00FFFFFF), // Transparent white
        Color(0x1AFFFFFF), // Subtle white overlay
        Color(0x00FFFFFF)  // Transparent
    )
)

val OceanBlueDarkGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFF0D47A1), // Deep navy
        Color(0xFF1565C0), // Royal blue
        Color(0xFF1976D2), // Ocean blue
        Color(0xFF1E88E5), // Bright ocean blue
        Color(0xFF42A5F5), // Light ocean blue
        Color(0xFF0D47A1)  // Back to deep navy
    ),
    center = androidx.compose.ui.geometry.Offset(0.7f, 0.3f),
    radius = 1000f
)

val OceanBlueDarkOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x001E3A8A), // Transparent metallic blue
        Color(0x331E3A8A), // Subtle metallic overlay
        Color(0x001E3A8A)  // Transparent
    )
)

// Premium Forest Green gradient backgrounds with natural effects
val ForestGreenLightGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFF1F8E9), // Light moss green
        Color(0xFFDCEDC8), // Light green
        Color(0xFFC8E6C9), // Soft green
        Color(0xFFA5D6A7), // Medium green
        Color(0xFF81C784), // Fresh green
        Color(0xFFF1F8E9)  // Back to light moss
    ),
    center = androidx.compose.ui.geometry.Offset(0.2f, 0.4f),
    radius = 900f
)

val ForestGreenLightOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x00E8F5E8), // Transparent light green
        Color(0x26E8F5E8), // Subtle green overlay
        Color(0x00E8F5E8)  // Transparent
    )
)

val ForestGreenDarkGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFF1B5E20), // Deep forest green
        Color(0xFF2E7D32), // Forest green
        Color(0xFF388E3C), // Medium green
        Color(0xFF43A047), // Bright green
        Color(0xFF66BB6A), // Light green
        Color(0xFF1B5E20)  // Back to deep forest
    ),
    center = androidx.compose.ui.geometry.Offset(0.8f, 0.2f),
    radius = 1100f
)

val ForestGreenDarkOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x002E7D32), // Transparent forest green
        Color(0x332E7D32), // Subtle forest overlay
        Color(0x002E7D32)  // Transparent
    )
)

// Premium Sunset Orange gradient backgrounds with warm effects
val SunsetOrangeLightGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFFFF8E1), // Light golden
        Color(0xFFFFF3E0), // Light orange
        Color(0xFFFFE0B2), // Soft orange
        Color(0xFFFFD54F), // Golden yellow
        Color(0xFFFFB74D), // Bright orange
        Color(0xFFFFF8E1)  // Back to light golden
    ),
    center = androidx.compose.ui.geometry.Offset(0.4f, 0.6f),
    radius = 850f
)

val SunsetOrangeLightOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x00FFF8E1), // Transparent light golden
        Color(0x26FFF8E1), // Subtle golden overlay
        Color(0x00FFF8E1)  // Transparent
    )
)

val SunsetOrangeDarkGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFE65100), // Deep sunset orange
        Color(0xFFFF6F00), // Bright sunset
        Color(0xFFFF8F00), // Medium sunset
        Color(0xFFFFA726), // Light sunset
        Color(0xFFFFB74D), // Bright sunset
        Color(0xFFE65100)  // Back to deep sunset
    ),
    center = androidx.compose.ui.geometry.Offset(0.6f, 0.4f),
    radius = 950f
)

val SunsetOrangeDarkOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x00E65100), // Transparent sunset orange
        Color(0x33E65100), // Subtle sunset overlay
        Color(0x00E65100)  // Transparent
    )
)

// Premium Midnight Purple gradient backgrounds with cosmic star effects
val MidnightPurpleLightGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFF3E5F5), // Light lavender
        Color(0xFFE1BEE7), // Light purple
        Color(0xFFCE93D8), // Medium purple
        Color(0xFFBA68C8), // Bright purple
        Color(0xFFAB47BC), // Deep purple
        Color(0xFFF3E5F5)  // Back to light lavender
    ),
    center = androidx.compose.ui.geometry.Offset(0.3f, 0.4f),
    radius = 750f
)

val MidnightPurpleLightOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x00F3E5F5), // Transparent lavender
        Color(0x26F3E5F5), // Subtle lavender overlay
        Color(0x00F3E5F5)  // Transparent
    )
)

val MidnightPurpleDarkGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFF1A0033), // Deep midnight
        Color(0xFF2D0044), // Midnight purple
        Color(0xFF3D0055), // Dark purple
        Color(0xFF4A148C), // Deep purple
        Color(0xFF6A1B9A), // Bright purple
        Color(0xFF1A0033)  // Back to deep midnight
    ),
    center = androidx.compose.ui.geometry.Offset(0.7f, 0.3f),
    radius = 950f
)

val MidnightPurpleDarkOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x001A0033), // Transparent midnight
        Color(0x331A0033), // Subtle midnight overlay
        Color(0x001A0033)  // Transparent
    )
)

// Premium Candy gradient backgrounds with sweet colorful effects
val CandyLightGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFFFF8E1), // Light cream
        Color(0xFFFFF3E0), // Very light cream
        Color(0xFFFFE0B2), // Light yellow
        Color(0xFFFFD54F), // Bright yellow
        Color(0xFFFFC107), // Golden yellow
        Color(0xFFFFF8E1)  // Back to light cream
    ),
    center = androidx.compose.ui.geometry.Offset(0.4f, 0.5f),
    radius = 700f
)

val CandyLightOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x00FFF8E1), // Transparent cream
        Color(0x26FFF8E1), // Subtle cream overlay
        Color(0x00FFF8E1)  // Transparent
    )
)

val CandyDarkGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFF3E2723), // Dark chocolate
        Color(0xFF4E342E), // Chocolate
        Color(0xFF5D4037), // Medium chocolate
        Color(0xFF6D4C41), // Light chocolate
        Color(0xFF8D6E63), // Brown
        Color(0xFF3E2723)  // Back to dark chocolate
    ),
    center = androidx.compose.ui.geometry.Offset(0.6f, 0.4f),
    radius = 850f
)

val CandyDarkOverlay = Brush.verticalGradient(
    colors = listOf(
        Color(0x003E2723), // Transparent chocolate
        Color(0x333E2723), // Subtle chocolate overlay
        Color(0x003E2723)  // Transparent
    )
)

@Composable
fun OceanBlueBackground(
    themeSetting: ThemeSetting,
    useDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveDarkTheme = when (themeSetting) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.SYSTEM -> isSystemDark
        ThemeSetting.OCEAN_BLUE -> useDarkTheme || isSystemDark
        ThemeSetting.FOREST_GREEN -> useDarkTheme || isSystemDark
        ThemeSetting.SUNSET_ORANGE -> useDarkTheme || isSystemDark
        ThemeSetting.MIDNIGHT_PURPLE -> true
        ThemeSetting.CANDY -> useDarkTheme || isSystemDark
    }

    val (gradient, overlay, hasEffects) = when (themeSetting) {
        ThemeSetting.OCEAN_BLUE -> {
            val grad = if (effectiveDarkTheme) OceanBlueDarkGradient else OceanBlueLightGradient
            val over = if (effectiveDarkTheme) OceanBlueDarkOverlay else OceanBlueLightOverlay
            Triple(grad, over, true)
        }
        ThemeSetting.FOREST_GREEN -> {
            val grad = if (effectiveDarkTheme) ForestGreenDarkGradient else ForestGreenLightGradient
            val over = if (effectiveDarkTheme) ForestGreenDarkOverlay else ForestGreenLightOverlay
            Triple(grad, over, true)
        }
        ThemeSetting.SUNSET_ORANGE -> {
            val grad = if (effectiveDarkTheme) SunsetOrangeDarkGradient else SunsetOrangeLightGradient
            val over = if (effectiveDarkTheme) SunsetOrangeDarkOverlay else SunsetOrangeLightOverlay
            Triple(grad, over, true)
        }
        ThemeSetting.MIDNIGHT_PURPLE -> {
            val grad = if (effectiveDarkTheme) MidnightPurpleDarkGradient else MidnightPurpleLightGradient
            val over = if (effectiveDarkTheme) MidnightPurpleDarkOverlay else MidnightPurpleLightOverlay
            Triple(grad, over, true)
        }
        ThemeSetting.CANDY -> {
            val grad = if (effectiveDarkTheme) CandyDarkGradient else CandyLightGradient
            val over = if (effectiveDarkTheme) CandyDarkOverlay else CandyLightOverlay
            Triple(grad, over, true)
        }
        else -> Triple(null, null, false)
    }

    RemindMeAppTheme(
        themeSetting = themeSetting,
        useDarkTheme = effectiveDarkTheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (gradient != null && overlay != null) {
                            Modifier
                                .background(gradient)
                                .background(overlay)
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
            ) {
                // Add premium visual effects
                if (hasEffects) {
                    when (themeSetting) {
                        ThemeSetting.LIGHT -> {}
                        ThemeSetting.DARK -> {}
                        ThemeSetting.SYSTEM -> {}
                        ThemeSetting.OCEAN_BLUE -> {
                            OceanBlueParticles(themeSetting = themeSetting)
                            OceanBlueWaveAnimation(themeSetting = themeSetting)
                        }
                        ThemeSetting.FOREST_GREEN -> {
                            ForestGreenParticles(themeSetting = themeSetting)
                            ForestGreenLeafAnimation(themeSetting = themeSetting)
                        }
                        ThemeSetting.SUNSET_ORANGE -> {
                            SunsetOrangeParticles(themeSetting = themeSetting)
                            SunsetOrangeGlowAnimation(themeSetting = themeSetting)
                        }
                        ThemeSetting.MIDNIGHT_PURPLE -> {}
                        ThemeSetting.CANDY -> {}
                    }
                }

                content()
            }
        }
    }
}

// Forest Green particle effects
@Composable
fun ForestGreenParticles(themeSetting: ThemeSetting) {
    // Add subtle particle effects for Forest Green theme
    // Similar to OceanBlueParticles but with leaf/butterfly effects
}

@Composable
fun ForestGreenLeafAnimation(themeSetting: ThemeSetting) {
    // Add leaf falling animation for Forest Green theme
    // Similar to OceanBlueWaveAnimation but with leaf effects
}

// Sunset Orange particle effects
@Composable
fun SunsetOrangeParticles(themeSetting: ThemeSetting) {
    // Add subtle particle effects for Sunset Orange theme
    // Similar to OceanBlueParticles but with warm glow effects
}

@Composable
fun SunsetOrangeGlowAnimation(themeSetting: ThemeSetting) {
    // Add warm glow animation for Sunset Orange theme
    // Similar to OceanBlueWaveAnimation but with sunset glow effects
}
