package com.FreeRave.remindme.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.data.ThemeSetting

/**
 * Performance optimization utilities for Ocean Blue premium theme
 */

// Performance settings based on device capabilities
data class OceanBluePerformanceConfig(
    val enableParticles: Boolean = true,
    val enableWaveAnimation: Boolean = true,
    val particleCount: Int = 12,
    val enableGlassmorphism: Boolean = true,
    val enableShadows: Boolean = true,
    val animationFrameRate: Int = 60
)

/**
 * Determines optimal performance settings based on device capabilities
 */
@Composable
fun rememberOceanBluePerformanceConfig(themeSetting: ThemeSetting): OceanBluePerformanceConfig {
    if (themeSetting != ThemeSetting.OCEAN_BLUE) {
        return OceanBluePerformanceConfig(
            enableParticles = false,
            enableWaveAnimation = false,
            enableGlassmorphism = false,
            enableShadows = false
        )
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Calculate screen size in dp
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    // Performance optimizations based on screen size and density
    val isLargeScreen = screenWidthDp > 600.dp || screenHeightDp > 800.dp
    val isHighDensity = density.density > 2.5f

    return OceanBluePerformanceConfig(
        enableParticles = !isHighDensity || isLargeScreen, // Disable particles on very high density small screens
        enableWaveAnimation = true, // Wave animation is lightweight
        particleCount = when {
            isLargeScreen -> 16
            isHighDensity -> 8
            else -> 12
        },
        enableGlassmorphism = !isHighDensity, // Glassmorphism can be expensive on high density
        enableShadows = true, // Shadows are generally performant
        animationFrameRate = if (isHighDensity) 30 else 60
    )
}

/**
 * Performance-aware particle count calculation
 */
@Composable
fun getOptimizedParticleCount(themeSetting: ThemeSetting): Int {
    if (themeSetting != ThemeSetting.OCEAN_BLUE) return 0

    val config = rememberOceanBluePerformanceConfig(themeSetting)
    return config.particleCount
}

/**
 * Checks if premium effects should be enabled
 */
@Composable
fun shouldEnablePremiumEffects(themeSetting: ThemeSetting): Boolean {
    if (themeSetting != ThemeSetting.OCEAN_BLUE) return false

    val config = rememberOceanBluePerformanceConfig(themeSetting)
    return config.enableParticles || config.enableWaveAnimation
}

/**
 * Memory-efficient animation state management
 */
class OceanBlueAnimationState {
    var isActive: Boolean = true
        private set

    fun pause() {
        isActive = false
    }

    fun resume() {
        isActive = true
    }

    fun dispose() {
        isActive = false
    }
}

/**
 * Provides animation state with automatic lifecycle management
 */
@Composable
fun rememberOceanBlueAnimationState(): OceanBlueAnimationState {
    val state = remember { OceanBlueAnimationState() }

    DisposableEffect(Unit) {
        onDispose {
            state.dispose()
        }
    }

    return state
}

/**
 * Performance monitoring for premium effects
 */
object OceanBluePerformanceMonitor {
    private var frameCount = 0
    private var lastFrameTime = 0L

    fun recordFrame() {
        frameCount++
        val currentTime = System.nanoTime()
        if (lastFrameTime != 0L) {
            val frameTime = (currentTime - lastFrameTime) / 1_000_000 // Convert to milliseconds
            if (frameTime > 16) { // More than 60fps
                // Log performance issue in debug builds
                println("OceanBlue: Frame time exceeded 16ms: ${frameTime}ms")
            }
        }
        lastFrameTime = currentTime
    }

    fun reset() {
        frameCount = 0
        lastFrameTime = 0L
    }
}
