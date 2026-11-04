package com.FreeRave.remindme.screens.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.ui.components.glassTopBarHeight
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.support.components.SupportActionCard
import com.FreeRave.remindme.ui.components.GlassSurface
import com.FreeRave.remindme.ui.theme.GlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupportScreenContent(
    state: SupportAdState, onBack: () -> Unit, onRequest: (SupportAdKind) -> Unit,
    onCancel: () -> Unit, onThanked: () -> Unit, banner: @Composable () -> Unit
) {
    GlassBackground {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(expandedHeight = glassTopBarHeight(), title = { Text(stringResource(R.string.support_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        GlassSurface(Modifier.padding(start = 8.dp), shape = CircleShape) {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reminder_back)) }
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item(key = "intro") {
                        GlassSurface(tint = MaterialTheme.colorScheme.primaryContainer) {
                            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                                    Icon(Icons.Outlined.FavoriteBorder, null, tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(14.dp).size(28.dp))
                                }
                                Text(stringResource(R.string.support_hero_title), style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.support_hero_body), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item(key = "choices") {
                        Text(stringResource(R.string.support_choose), style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                    item(key = "standard") {
                        SupportActionCard(SupportAdKind.STANDARD, state, onRequest = { onRequest(SupportAdKind.STANDARD) }, onCancel = onCancel)
                    }
                    item(key = "rewarded") {
                        SupportActionCard(SupportAdKind.REWARDED, state, onRequest = { onRequest(SupportAdKind.REWARDED) }, onCancel = onCancel)
                    }
                    item(key = "banner") { banner() }
                    item(key = "optional") {
                        Text(stringResource(R.string.support_optional), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (state.phase == SupportAdPhase.THANK_YOU) AlertDialog(onDismissRequest = onThanked,
            icon = { Icon(Icons.Outlined.FavoriteBorder, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.support_thanks_title)) },
            text = { Text(stringResource(R.string.support_thanks_body)) },
            confirmButton = { TextButton(onClick = onThanked) { Text(stringResource(R.string.support_thanks_done)) } })
    }
}
