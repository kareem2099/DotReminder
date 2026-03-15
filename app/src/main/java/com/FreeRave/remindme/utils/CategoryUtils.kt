package com.FreeRave.remindme.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

object CategoryUtils {
    val predefinedCategories = listOf("Work", "Personal", "Health", "Shopping")

    val categoryColors = mapOf(
        "Work" to Color(0xFF42A5F5), // Blue
        "Personal" to Color(0xFF66BB6A), // Green
        "Health" to Color(0xFFFF7043), // Orange
        "Shopping" to Color(0xFFAB47BC), // Purple
        "Default" to Color(0xFF78909C) // Grey for fallbacks
    )

    val categoryIcons = mapOf(
        "Work" to "💼",
        "Personal" to "👤",
        "Health" to "❤️",
        "Shopping" to "🛒",
        "Default" to "📁"
    )

    fun getCategoryColor(category: String?): Color? = categoryColors[category] ?: categoryColors["Default"]

    fun getCategoryIcon(category: String?): String = categoryIcons[category] ?: categoryIcons["Default"]!!
}
