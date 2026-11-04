package com.FreeRave.remindme.screens.templates.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.ReminderTemplate
import com.FreeRave.remindme.ui.components.GlassSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TemplateCard(template: ReminderTemplate, enabled: Boolean, onUse: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Icon(Icons.Outlined.Layers, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Text(template.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Box {
                        IconButton(onClick = { menu = true }, enabled = enabled) {
                            Icon(Icons.Default.MoreHoriz, stringResource(R.string.templates_options, template.name))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.templates_edit)) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; onEdit() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.templates_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; onDelete() })
                        }
                    }
                }
                Text(template.title, style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (template.description.isNotBlank()) Text(template.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (template.category.isNotBlank() || template.isRecurring || template.isLocationBased) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (template.category.isNotBlank()) TemplateBadge(template.category)
                        if (template.isRecurring) TemplateBadge(templateIntervalLabel(template.recurrenceIntervalMillis))
                        if (template.isLocationBased) TemplateBadge(stringResource(R.string.templates_location))
                    }
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            TextButton(onClick = onUse, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                Text(stringResource(R.string.templates_use))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TemplateBadge(label: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
