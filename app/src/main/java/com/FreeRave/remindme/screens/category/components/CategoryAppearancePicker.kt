package com.FreeRave.remindme.screens.category.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.category.categoryColorArgb
import com.FreeRave.remindme.screens.category.normalizeCategoryColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryColorPicker(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categoryColors.forEach { (label, hex) ->
            val color = Color(requireNotNull(categoryColorArgb(hex)))
            val isSelected = normalizeCategoryColor(selected) == hex
            val description = stringResource(label)
            Box(Modifier.size(48.dp).clip(CircleShape)
                .selectable(isSelected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(hex) })
                .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(36.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                    if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(22.dp),
                        tint = if (color.luminance() > 0.35f) Color.Black else Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryIconPicker(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    val icons = if (selected.isNotBlank() && selected !in categoryIcons) listOf(selected) + categoryIcons else categoryIcons
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        icons.forEach { emoji ->
            val isSelected = selected == emoji
            val description = stringResource(R.string.category_icon_option, emoji)
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                .then(if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    RoundedCornerShape(14.dp)) else Modifier)
                .selectable(isSelected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(emoji) })
                .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

private val categoryColors = listOf(
    R.string.category_color_blue to "#FF42A5F5", R.string.category_color_green to "#FF66BB6A",
    R.string.category_color_coral to "#FFFF7043", R.string.category_color_purple to "#FFAB47BC",
    R.string.category_color_pink to "#FFF18CB8", R.string.category_color_gold to "#FFEFBF42",
    R.string.category_color_teal to "#FF61C6D5", R.string.category_color_slate to "#FF94A3B8"
)
private val categoryIcons = listOf("📁", "💼", "😊", "🏥", "🛒", "🏠", "📚", "🎯", "✈️", "💡", "☕", "💪")
