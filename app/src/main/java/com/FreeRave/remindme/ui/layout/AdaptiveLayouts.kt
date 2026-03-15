package com.FreeRave.remindme.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.FreeRave.remindme.ui.responsive.*
import androidx.compose.ui.unit.dp


/**
 * Adaptive container that provides different layouts based on screen size
 */
@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSizeClass, Modifier) -> Unit
) {
    val windowSizeClass = getWindowSizeClass()
    val containerModifier = when (windowSizeClass) {
        WindowSizeClass.COMPACT -> modifier
            .fillMaxSize()
            .padding(getResponsivePadding(16.dp))
        WindowSizeClass.MEDIUM -> modifier
            .fillMaxSize()
            .padding(getResponsivePadding(24.dp))
        WindowSizeClass.EXPANDED -> modifier
            .fillMaxSize()
            .padding(getResponsivePadding(32.dp))
    }
    content(windowSizeClass, containerModifier)
}

/**
 * Adaptive column layout for different screen sizes
 */
@Composable
fun AdaptiveColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollModifier = if (scrollable) {
        Modifier.verticalScroll(rememberScrollState())
    } else Modifier
    
    Column(
        modifier = modifier.then(scrollModifier),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment
    ) {
        content()
    }
}

/**
 * Adaptive row layout for tablets/desktops
 */
@Composable
fun AdaptiveRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(getResponsiveSpacing(16.dp)),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment
    ) {
        content()
    }
}

/**
 * Adaptive layout that switches between single column (phone) and two columns (tablet)
 */
@Composable
fun AdaptiveTwoPaneLayout(
    modifier: Modifier = Modifier,
    leftContent: @Composable () -> Unit,
    rightContent: @Composable () -> Unit,
    showTwoPanes: Boolean = isExpandedScreen()
) {
    if (showTwoPanes) {
        // Two-pane layout for tablets/desktops
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing(32.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left pane - takes up available space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                leftContent()
            }
            
            // Right pane - takes up available space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                rightContent()
            }
        }
    } else {
        // Single-pane layout for phones
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            leftContent()
            Spacer(modifier = Modifier.height(getResponsiveSpacing(24.dp)))
            rightContent()
        }
    }
}

/**
 * Adaptive form container
 */
@Composable
fun AdaptiveFormContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val maxWidth = getResponsiveFormMaxWidth(
        compact = 320.dp,
        medium = 400.dp,
        expanded = 480.dp
    )
    
    Box(
        modifier = modifier.then(
            Modifier.widthIn(max = maxWidth)
        ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Adaptive spacing between elements
 */
@Composable
fun AdaptiveSpacer(
    size: Dp? = null,
    axis: SpacerAxis = SpacerAxis.VERTICAL
) {
    val spacing = size ?: getResponsiveSpacing(
        compact = 16.dp,
        medium = 24.dp,
        expanded = 32.dp
    )
    
    when (axis) {
        SpacerAxis.VERTICAL -> Spacer(modifier = Modifier.height(spacing))
        SpacerAxis.HORIZONTAL -> Spacer(modifier = Modifier.width(spacing))
    }
}

/**
 * Enum for spacer axis direction
 */
enum class SpacerAxis {
    VERTICAL, HORIZONTAL
}

/**
 * Responsive card that adapts its padding and sizing
 */
@Composable
fun ResponsiveCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = getResponsivePadding(
        compact = 16.dp,
        medium = 24.dp,
        expanded = 32.dp
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding),
        content = content
    )
}
