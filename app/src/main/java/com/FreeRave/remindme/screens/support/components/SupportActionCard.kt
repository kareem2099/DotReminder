package com.FreeRave.remindme.screens.support.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.screens.support.SupportAdKind
import com.FreeRave.remindme.screens.support.SupportAdPhase
import com.FreeRave.remindme.screens.support.SupportAdState
import com.FreeRave.remindme.ui.components.GlassSurface

@Composable
internal fun SupportActionCard(kind: SupportAdKind, state: SupportAdState, onRequest: () -> Unit, onCancel: () -> Unit) {
    val standard = kind == SupportAdKind.STANDARD
    val targeted = state.kind == kind
    val loading = targeted && state.phase == SupportAdPhase.LOADING
    val showing = targeted && state.phase == SupportAdPhase.SHOWING
    val failed = targeted && state.phase == SupportAdPhase.FAILED
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Icon(if (standard) Icons.Outlined.PlayCircleOutline else Icons.Outlined.VolunteerActivism,
                        null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(24.dp))
                }
                Text(stringResource(if (standard) R.string.support_standard_title else R.string.support_rewarded_title),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Text(stringResource(if (standard) R.string.support_standard_body else R.string.support_rewarded_body),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (failed) Text(stringResource(R.string.support_ad_error), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
            Button(onClick = onRequest, enabled = state.canRequest, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (loading) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                Text(stringResource(when {
                    loading -> R.string.support_loading
                    showing -> R.string.support_showing
                    failed -> R.string.support_retry
                    standard -> R.string.support_standard_action
                    else -> R.string.support_rewarded_action
                }), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            if (loading) TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.support_cancel))
            }
        }
    }
}
