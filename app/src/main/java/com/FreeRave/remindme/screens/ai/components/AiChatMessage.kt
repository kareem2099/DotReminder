package com.FreeRave.remindme.screens.ai.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.R
import com.FreeRave.remindme.model.AiReminderDraft
import com.FreeRave.remindme.screens.ai.ChatMessage
import com.FreeRave.remindme.screens.ai.SenderType
import com.FreeRave.remindme.ui.components.GlassSurface
import java.util.Date

@Composable
internal fun AiChatMessage(message: ChatMessage, onReview: (AiReminderDraft) -> Unit, onOpen: (String) -> Unit) {
    val user = message.sender == SenderType.USER
    Column(Modifier.fillMaxWidth().padding(start = if (user) 32.dp else 0.dp, end = if (user) 0.dp else 24.dp),
        horizontalAlignment = if (user) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(stringResource(if (user) R.string.ai_chat_you else R.string.ai_chat_assistant),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp))
        if (message.draft != null) ReminderDraftCard(message.draft, message.savedReminderId, onReview, onOpen)
        else if (user) {
            SelectionContainer {
                Text(message.text, Modifier.background(MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)).padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            GlassSurface(shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)) {
                SelectionContainer { Text(message.text, Modifier.padding(18.dp), style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

@Composable
private fun ReminderDraftCard(draft: AiReminderDraft, savedId: String?, onReview: (AiReminderDraft) -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val date = Date(draft.reminderTime)
    val schedule = "${DateFormat.getMediumDateFormat(context).format(date)} · ${DateFormat.getTimeFormat(context).format(date)}"
    GlassSurface(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.EventNote, null, Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
                Text(stringResource(if (savedId == null) R.string.ai_chat_draft else R.string.ai_chat_saved),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (savedId == null) {
                Text(draft.title, style = MaterialTheme.typography.titleLarge)
                Text(schedule, style = MaterialTheme.typography.bodyMedium)
                if (draft.isRecurring) Text(stringResource(R.string.ai_chat_repeating), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.ai_chat_review_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else Text(stringResource(R.string.ai_chat_saved_intro), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { if (savedId == null) onReview(draft) else onOpen(savedId) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(if (savedId == null) R.string.ai_chat_review else R.string.ai_chat_open))
            }
        }
    }
}
