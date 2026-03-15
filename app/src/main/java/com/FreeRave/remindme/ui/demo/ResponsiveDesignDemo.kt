package com.FreeRave.remindme.ui.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.layout.*
import com.FreeRave.remindme.ui.responsive.*
import com.FreeRave.remindme.ui.theme.ScreenBackground
import androidx.compose.ui.unit.sp


/**
 * Demo screen showing how to use the global responsive design system
 */
@Composable
fun ResponsiveDesignDemo() {
    ScreenBackground(
        screenType = com.FreeRave.remindme.ui.theme.ScreenType.LOGIN,
        themeSetting = com.FreeRave.remindme.data.ThemeSetting.SYSTEM,
        useDarkTheme = false
    ) {
        AdaptiveContainer { windowSizeClass, modifier ->
            when (windowSizeClass) {
                WindowSizeClass.COMPACT -> {
                    // Phone layout - single column
                    AdaptiveColumn(modifier = modifier) {
                        DemoContent(
                            windowSizeClass = windowSizeClass,
                            isTwoPane = false
                        )
                    }
                }
                WindowSizeClass.MEDIUM, WindowSizeClass.EXPANDED -> {
                    // Tablet/desktop layout - two panes
                    AdaptiveTwoPaneLayout(
                        modifier = modifier,
                        leftContent = {
                            DemoContent(
                                windowSizeClass = windowSizeClass,
                                isTwoPane = true,
                                isLeftPane = true
                            )
                        },
                        rightContent = {
                            DemoContent(
                                windowSizeClass = windowSizeClass,
                                isTwoPane = true,
                                isLeftPane = false
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoContent(
    windowSizeClass: WindowSizeClass,
    isTwoPane: Boolean,
    isLeftPane: Boolean = false
) {
    // Use responsive dimensions
    val titleSize = getResponsiveTextSizeSp(
        compact = 20.sp,
        medium = 28.sp,
        expanded = 36.sp
    )
    
    val cardPadding = getResponsivePadding(
        compact = 16.dp,
        medium = 24.dp,
        expanded = 32.dp
    )
    
    val spacing = getResponsiveSpacing(
        compact = 16.dp,
        medium = 24.dp,
        expanded = 32.dp
    )

    if (isTwoPane) {
        // Tablet/desktop: Show content in a card
        ResponsiveCard {
            DemoCardContent(
                titleSize = titleSize,
                spacing = spacing,
                windowSizeClass = windowSizeClass,
                isLeftPane = isLeftPane
            )
        }
    } else {
        // Phone: Show content directly
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DemoCardContent(
                titleSize = titleSize,
                spacing = spacing,
                windowSizeClass = windowSizeClass,
                isLeftPane = isLeftPane
            )
        }
    }
}

@Composable
private fun DemoCardContent(
    titleSize: androidx.compose.ui.unit.TextUnit,
    spacing: androidx.compose.ui.unit.Dp,
    windowSizeClass: WindowSizeClass,
    isLeftPane: Boolean
) {
    val title = when {
        isLeftPane -> "Branding & Logo"
        windowSizeClass == WindowSizeClass.COMPACT -> "Login Form"
        else -> "Login Form (Right Side)"
    }
    
    val description = when (windowSizeClass) {
        WindowSizeClass.COMPACT -> "This layout adapts to your screen size automatically. On phones, content stacks vertically."
        WindowSizeClass.MEDIUM -> "Medium screens get expanded spacing and larger text for better readability."
        WindowSizeClass.EXPANDED -> "Large screens use two-pane layouts with optimal spacing and typography."
    }

    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(spacing))
    
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(spacing))
    
    // Show screen size info
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Screen Info:",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Size: ${windowSizeClass.name}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Adaptive: ${if (isLeftPane) "Left Pane" else "Right/Main"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
