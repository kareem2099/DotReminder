package com.FreeRave.remindme.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.data.ThemeSetting
import kotlinx.coroutines.delay
import kotlin.random.Random

// Premium particle animation for Ocean Blue theme with performance optimization
@Composable
fun OceanBlueParticles(
    themeSetting: ThemeSetting,
    modifier: Modifier = Modifier
) {
    if (themeSetting != ThemeSetting.OCEAN_BLUE) return

    val config = rememberOceanBluePerformanceConfig(themeSetting)
    if (!config.enableParticles) return

    val particleCount = config.particleCount
    val particles = remember(particleCount) { List(particleCount) { ParticleState() } }

    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            OceanParticle(particle, config.animationFrameRate)
        }
    }
}

data class ParticleState(
    val id: Int = Random.nextInt(),
    var x: Float = Random.nextFloat(),
    var y: Float = Random.nextFloat(),
    var size: Float = Random.nextFloat() * 4f + 2f,
    var speed: Float = Random.nextFloat() * 0.02f + 0.005f,
    var alpha: Float = Random.nextFloat() * 0.5f + 0.2f
)

@Composable
private fun OceanParticle(particle: ParticleState, frameRate: Int = 60) {
    val infiniteTransition = rememberInfiniteTransition()

    // Adjust animation duration based on frame rate for performance
    val durationMultiplier = 60f / frameRate
    val yDuration = (3000 * durationMultiplier).toInt()
    val alphaDuration = (2000 * durationMultiplier).toInt()

    val animatedY by infiniteTransition.animateFloat(
        initialValue = particle.y,
        targetValue = particle.y - 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(yDuration + particle.id % 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = particle.alpha,
        targetValue = particle.alpha * 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(alphaDuration + particle.id % 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(
                x = (particle.x * 100).dp,
                y = (animatedY * 100).dp
            )
    ) {
        Box(
            modifier = Modifier
                .size(particle.size.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = animatedAlpha),
                            Color(0xFF90CAF9).copy(alpha = animatedAlpha * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// Glassmorphism effect for premium surfaces
@Composable
fun OceanBlueGlassSurface(
    themeSetting: ThemeSetting,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (themeSetting == ThemeSetting.OCEAN_BLUE) {
        Box(
            modifier = modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .blur(0.5.dp)
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

// Premium shadow effect
@Composable
fun OceanBlueShadow(
    themeSetting: ThemeSetting,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (themeSetting == ThemeSetting.OCEAN_BLUE) {
        Box(modifier = modifier) {
            // Shadow layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 2.dp)
                    .background(
                        color = Color(0xFF1E3A8A).copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .blur(4.dp)
            )
            // Main content
            content()
        }
    } else {
        content()
    }
}

// Animated wave effect for backgrounds
@Composable
fun OceanBlueWaveAnimation(
    themeSetting: ThemeSetting,
    modifier: Modifier = Modifier
) {
    if (themeSetting != ThemeSetting.OCEAN_BLUE) return

    val infiniteTransition = rememberInfiniteTransition()

    val waveOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val waveOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Subtle animated wave overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8F4FD).copy(alpha = 0.1f),
                            Color(0xFFB3E5FC).copy(alpha = 0.05f),
                            Color(0xFFE8F4FD).copy(alpha = 0.1f)
                        )
                    )
                )
        )
    }
}

// Premium loading shimmer effect
@Composable
fun OceanBlueShimmer(
    themeSetting: ThemeSetting,
    modifier: Modifier = Modifier
) {
    if (themeSetting != ThemeSetting.OCEAN_BLUE) return

    val infiniteTransition = rememberInfiniteTransition()
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE8F4FD).copy(alpha = 0.3f),
                        Color(0xFF90CAF9).copy(alpha = 0.6f),
                        Color(0xFFE8F4FD).copy(alpha = 0.3f)
                    ),
                    start = androidx.compose.ui.geometry.Offset(shimmerProgress * 1000, 0f),
                    end = androidx.compose.ui.geometry.Offset(shimmerProgress * 1000 + 500, 0f)
                )
            )
    )
}
