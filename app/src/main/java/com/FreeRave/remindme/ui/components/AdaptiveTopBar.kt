package com.FreeRave.remindme.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Allow wrapped titles and subtitles to grow with accessibility text size. */
@Composable
internal fun glassTopBarHeight(subtitle: Boolean = false) =
    ((if (subtitle) 80f else 64f) * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
