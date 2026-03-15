package com.FreeRave.remindme.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.FreeRave.remindme.data.ThemeSetting

enum class ScreenType {
    LOGIN, SIGNUP, HOME, SETTINGS
}

@UnstableApi
@Composable
fun ScreenBackground(
    screenType: ScreenType,
    themeSetting: ThemeSetting,
    useDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current

    // Get asset path based on screen and theme
    val assetPath = remember(screenType, themeSetting) {
        getAssetPathForScreenAndTheme(screenType, themeSetting)
    }

    // Check if asset exists and get media type
    val (assetExists, isVideo) = remember(assetPath) {
        checkAssetExists(context, assetPath)
    }

    Box(modifier = modifier.fillMaxSize()) {

        // background content
        if (assetExists) {
            if (isVideo) {
                // Video background for home screen
                VideoBackgroundPlayer(
                    videoPath = assetPath,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Image background for other screens
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/$assetPath")
                        .build(),
                    contentDescription = "Screen background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            }
        } else {
            // Fallback to theme gradients
            val (gradient, overlay) = when (themeSetting) {
                ThemeSetting.OCEAN_BLUE -> {
                    val grad = if (useDarkTheme) OceanBlueDarkGradient else OceanBlueLightGradient
                    val over = if (useDarkTheme) OceanBlueDarkOverlay else OceanBlueLightOverlay
                    grad to over
                }
                ThemeSetting.FOREST_GREEN -> {
                    val grad = if (useDarkTheme) ForestGreenDarkGradient else ForestGreenLightGradient
                    val over = if (useDarkTheme) ForestGreenDarkOverlay else ForestGreenLightOverlay
                    grad to over
                }
                ThemeSetting.SUNSET_ORANGE -> {
                    val grad = if (useDarkTheme) SunsetOrangeDarkGradient else SunsetOrangeLightGradient
                    val over = if (useDarkTheme) SunsetOrangeDarkOverlay else SunsetOrangeLightOverlay
                    grad to over
                }
                ThemeSetting.MIDNIGHT_PURPLE -> {
                    val grad = if (useDarkTheme) MidnightPurpleDarkGradient else MidnightPurpleLightGradient
                    val over = if (useDarkTheme) MidnightPurpleDarkOverlay else MidnightPurpleLightOverlay
                    grad to over
                }
                ThemeSetting.CANDY -> {
                    val grad = if (useDarkTheme) CandyDarkGradient else CandyLightGradient
                    val over = if (useDarkTheme) CandyDarkOverlay else CandyLightOverlay
                    grad to over
                }
                // For LIGHT, DARK, SYSTEM, use MaterialTheme's background color
                else -> {
                    val backgroundColor = if (useDarkTheme) DarkBackground else LightBackground
                    (null to null) // No specific gradient or overlay for these, let MaterialTheme handle it
                }
            }

            // Apply gradient background or MaterialTheme background
            if (gradient != null && overlay != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradient)
                        .background(overlay)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                )
            }
        }
        // foreground content WITH controlled system bar handling to eliminate white borders
        Box (
            modifier = Modifier
                .fillMaxSize()
        ) {
            content()
        }
    }
}

private fun getAssetPathForScreenAndTheme(screenType: ScreenType, themeSetting: ThemeSetting): String {
    val themeName = when (themeSetting) {
        ThemeSetting.OCEAN_BLUE -> "ocean_blue"
        ThemeSetting.FOREST_GREEN -> "forest_green"
        ThemeSetting.SUNSET_ORANGE -> "sunset_orange"
        ThemeSetting.MIDNIGHT_PURPLE -> "midnight_purple"
        ThemeSetting.CANDY -> "candy"
        else -> "default"
    }

    val screenName = when (screenType) {
        ScreenType.LOGIN -> "login"
        ScreenType.SIGNUP -> "signup"
        ScreenType.HOME -> "home"
        ScreenType.SETTINGS -> "settings"
    }

    val extension = when (screenType) {
        ScreenType.HOME -> ".mp4"
        else -> ".png"
    }

    return "backgrounds/$screenName/${themeName}_${screenName}$extension"
}

private fun checkAssetExists(context: android.content.Context, assetPath: String): Pair<Boolean, Boolean> {
    return try {
        val assetManager = context.assets
        val inputStream = assetManager.open(assetPath)
        inputStream.close()
        val isVideo = assetPath.endsWith(".mp4", ignoreCase = true)
        true to isVideo
    } catch (e: Exception) {
        false to false
    }
}
