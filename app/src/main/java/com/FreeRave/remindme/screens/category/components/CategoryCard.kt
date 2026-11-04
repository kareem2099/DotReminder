package com.FreeRave.remindme.screens.category.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.screens.category.DEFAULT_CATEGORY_ICON
import com.FreeRave.remindme.screens.category.categoryColorArgb
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun CategoryCard(category: Category, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    GlassSurface(modifier.fillMaxWidth(), onClick = onEdit) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CategoryGlyph(category.icon, category.colorHex)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (category.description.isNotBlank()) {
                    Text(category.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(stringResource(if (category.isPredefined) R.string.categories_built_in else R.string.categories_custom),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreHoriz, stringResource(R.string.category_options, category.name))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (category.isPredefined) R.string.category_customize else R.string.category_edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    if (!category.isPredefined) DropdownMenuItem(
                        text = { Text(stringResource(R.string.category_delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
internal fun CategoryGlyph(icon: String, colorHex: String, modifier: Modifier = Modifier) {
    val color = Color(categoryColorArgb(colorHex) ?: 0xFF78909C.toInt())
    Box(modifier.size(52.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        Text(icon.ifBlank { DEFAULT_CATEGORY_ICON }, style = MaterialTheme.typography.headlineSmall)
    }
}
