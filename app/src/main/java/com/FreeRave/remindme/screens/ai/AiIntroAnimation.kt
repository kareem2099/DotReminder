package com.FreeRave.remindme.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box // Added Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Simplified Animation Stages
enum class IntroStage {
    FADING_IN, VISIBLE, FADING_OUT, FINISHED
}

@Composable
fun AiIntroAnimation(onAnimationFinished: () -> Unit) {
    var stage by remember { mutableStateOf(IntroStage.FADING_IN) }
    var elementsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(stage) {
        when (stage) {
            IntroStage.FADING_IN -> {
                elementsVisible = true // Trigger fade in
                delay(800) // Duration of fade in + a little pause
                stage = IntroStage.VISIBLE
            }
            IntroStage.VISIBLE -> {
                delay(2000) // How long the message stays visible
                elementsVisible = false // Trigger fade out
                stage = IntroStage.FADING_OUT
            }
            IntroStage.FADING_OUT -> {
                delay(500) // Duration of fade out
                stage = IntroStage.FINISHED
            }
            IntroStage.FINISHED -> {
                onAnimationFinished()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Overall padding for the animation elements
        contentAlignment = Alignment.TopEnd // Position the content at the top-right
    ) {
        AnimatedVisibility(
            visible = elementsVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500))
        ) {
            Column(
                horizontalAlignment = Alignment.End // Align text to the end if it wraps
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "AI Icon",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Hi, how can I help you today?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
