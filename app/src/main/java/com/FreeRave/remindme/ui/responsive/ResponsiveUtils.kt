package com.FreeRave.remindme.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Screen size classes for adaptive design
 */
enum class WindowSizeClass {
    COMPACT,  // < 600dp width (phones in portrait)
    MEDIUM,   // 600-840dp width (tablets in portrait, phones in landscape)
    EXPANDED  // > 840dp width (tablets in landscape, desktops)
}

/**
 * Get the current window size class based on screen width
 */
@Composable
fun getWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp < 600 -> WindowSizeClass.COMPACT
        configuration.screenWidthDp <= 840 -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

/**
 * Check if current screen is compact (phone in portrait)
 */
@Composable
fun isCompactScreen(): Boolean = getWindowSizeClass() == WindowSizeClass.COMPACT

/**
 * Check if current screen is medium (tablet or phone landscape)
 */
@Composable
fun isMediumScreen(): Boolean = getWindowSizeClass() == WindowSizeClass.MEDIUM

/**
 * Check if current screen is expanded (tablet landscape or desktop)
 */
@Composable
fun isExpandedScreen(): Boolean = getWindowSizeClass() == WindowSizeClass.EXPANDED

/**
 * Get responsive padding based on screen size
 */
@Composable
fun getResponsivePadding(
    compact: Dp,
    medium: Dp = compact * 1.5f,
    expanded: Dp = compact * 2f
): Dp {
    return when (getWindowSizeClass()) {
        WindowSizeClass.COMPACT -> compact
        WindowSizeClass.MEDIUM -> medium
        WindowSizeClass.EXPANDED -> expanded
    }
}

/**
 * Get responsive spacing based on screen size
 */
@Composable
fun getResponsiveSpacing(
    compact: Dp,
    medium: Dp = compact * 1.5f,
    expanded: Dp = compact * 2f
): Dp {
    return when (getWindowSizeClass()) {
        WindowSizeClass.COMPACT -> compact
        WindowSizeClass.MEDIUM -> medium
        WindowSizeClass.EXPANDED -> expanded
    }
}

/**
 * Get responsive text size based on screen size
 */
//@Composable
//fun getResponsiveTextSize(
//    compact: Dp,
//    medium: Dp = compact * 1.1f,
//    expanded: Dp = compact * 1.2f
//): Dp {
//    return when (getWindowSizeClass()) {
//        WindowSizeClass.COMPACT -> compact
//        WindowSizeClass.MEDIUM -> medium
//        WindowSizeClass.EXPANDED -> expanded
//    }
//}


@Composable
fun getResponsiveTextSizeSp(
    compact: TextUnit,
    medium: TextUnit = compact * 1.1f,
    expanded: TextUnit = compact * 1.2f
): TextUnit {
    return when (getWindowSizeClass()) {
        WindowSizeClass.COMPACT -> compact
        WindowSizeClass.MEDIUM -> medium
        WindowSizeClass.EXPANDED -> expanded
    }
}

/**
 * Get responsive max width for forms
 */
@Composable
fun getResponsiveFormMaxWidth(
    compact: Dp,
    medium: Dp = 400.dp,
    expanded: Dp = 480.dp
): Dp {
    return when (getWindowSizeClass()) {
        WindowSizeClass.COMPACT -> compact
        WindowSizeClass.MEDIUM -> medium
        WindowSizeClass.EXPANDED -> expanded
    }
}

/**
 * Get responsive logo size
 */
@Composable
fun getResponsiveLogoSize(
    compact: Dp,
    medium: Dp = compact * 1.2f,
    expanded: Dp = compact * 1.5f
): Dp {
    return when (getWindowSizeClass()) {
        WindowSizeClass.COMPACT -> compact
        WindowSizeClass.MEDIUM -> medium
        WindowSizeClass.EXPANDED -> expanded
    }
}

/**
 * Get responsive column width based on screen size
 */
@Composable
fun getResponsiveColumnWidth(
    compact: Dp,
    medium: Dp = compact,
    expanded: Dp = compact * 2
): Dp {
    return when (getWindowSizeClass()) {
        WindowSizeClass.COMPACT -> compact
        WindowSizeClass.MEDIUM -> medium
        WindowSizeClass.EXPANDED -> expanded
    }
}
