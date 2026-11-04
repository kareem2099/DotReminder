package com.FreeRave.remindme.ui.reminder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderFormScaffold(
    title: String,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    isSaving: Boolean = false,
    saveLabel: String? = null,
    saveEnabled: Boolean = true,
    onSave: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    GlassBackground {
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(),
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack, enabled = !isSaving) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reminder_back))
                            }
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                if (saveLabel != null) {
                    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
                        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center) {
                            Button(
                                onClick = onSave, enabled = saveEnabled && !isSaving,
                                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(min = 56.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                                Text(if (isSaving) stringResource(R.string.reminder_saving) else saveLabel,
                                    modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
            },
            content = content
        )
    }
}
