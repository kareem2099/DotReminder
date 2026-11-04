package com.FreeRave.remindme.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDirection

// You can customize fontFamily later if you add fonts
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
).let { typography ->
    // Follow the text's own language while keeping controls aligned with the app layout direction.
    typography.copy(
        displayLarge = typography.displayLarge.copy(textDirection = TextDirection.Content),
        displayMedium = typography.displayMedium.copy(textDirection = TextDirection.Content),
        displaySmall = typography.displaySmall.copy(textDirection = TextDirection.Content),
        headlineLarge = typography.headlineLarge.copy(textDirection = TextDirection.Content),
        headlineMedium = typography.headlineMedium.copy(textDirection = TextDirection.Content),
        headlineSmall = typography.headlineSmall.copy(textDirection = TextDirection.Content),
        titleLarge = typography.titleLarge.copy(textDirection = TextDirection.Content),
        titleMedium = typography.titleMedium.copy(textDirection = TextDirection.Content),
        titleSmall = typography.titleSmall.copy(textDirection = TextDirection.Content),
        bodyLarge = typography.bodyLarge.copy(textDirection = TextDirection.Content),
        bodyMedium = typography.bodyMedium.copy(textDirection = TextDirection.Content),
        bodySmall = typography.bodySmall.copy(textDirection = TextDirection.Content),
        labelLarge = typography.labelLarge.copy(textDirection = TextDirection.Content),
        labelMedium = typography.labelMedium.copy(textDirection = TextDirection.Content),
        labelSmall = typography.labelSmall.copy(textDirection = TextDirection.Content)
    )
}
