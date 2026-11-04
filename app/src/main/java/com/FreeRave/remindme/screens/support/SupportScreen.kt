package com.FreeRave.remindme.screens.support

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.FreeRave.remindme.screens.support.components.SupportBannerCard
import com.FreeRave.remindme.ui.theme.GlassTheme

@Composable
fun SupportScreen(navController: NavController) {
    val activity = LocalActivity.current
    val ads = remember(activity) { SupportAdController(activity) }
    val state by ads.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ads, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) ads.cancelLoading()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); ads.dispose() }
    }
    GlassTheme {
        SupportScreenContent(state, onBack = { navController.popBackStack() }, onRequest = ads::request,
            onCancel = ads::cancelLoading, onThanked = ads::dismissThanks, banner = { SupportBannerCard() })
    }
}
