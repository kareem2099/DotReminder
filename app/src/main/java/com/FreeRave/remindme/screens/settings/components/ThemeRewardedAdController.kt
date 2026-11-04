package com.FreeRave.remindme.screens.settings.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.FreeRave.remindme.BuildConfig
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.settings.REQUIRED_THEME_ADS
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

internal sealed interface ThemeAdStatus {
    data object Idle : ThemeAdStatus
    data object Loading : ThemeAdStatus
    data object Showing : ThemeAdStatus
    data object Error : ThemeAdStatus
    data class Rewarded(val theme: ThemeSetting, val count: Int) : ThemeAdStatus
}

/** A cancelled or disposed request cannot open a late ad. Only the SDK reward callback credits a view. */
internal class ThemeRewardedAdController(
    private val context: Context,
    private val onReward: (ThemeSetting) -> Unit,
    private val onUnlocked: (ThemeSetting) -> Unit
) {
    var status: ThemeAdStatus by mutableStateOf(ThemeAdStatus.Idle)
        private set
    val busy get() = status == ThemeAdStatus.Loading || status == ThemeAdStatus.Showing
    private var requestId = 0
    private var active = true

    fun load(theme: ThemeSetting, watched: Int) {
        if (!active || busy) return
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) { status = ThemeAdStatus.Error; return }
        val request = ++requestId
        status = ThemeAdStatus.Loading
        RewardedAd.load(context, BuildConfig.REWARDED_AD_UNIT_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (active && request == requestId) status = ThemeAdStatus.Error
                }
                override fun onAdLoaded(ad: RewardedAd) {
                    if (!active || request != requestId) return
                    if (activity.isFinishing || activity.isDestroyed) { status = ThemeAdStatus.Error; return }
                    var reward: ThemeAdStatus.Rewarded? = null
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            if (active) status = ThemeAdStatus.Error
                        }
                        override fun onAdDismissedFullScreenContent() {
                            if (active) status = reward ?: ThemeAdStatus.Idle
                        }
                    }
                    status = ThemeAdStatus.Showing
                    ad.show(activity) {
                        if (reward == null) {
                            val count = (watched.coerceIn(0, REQUIRED_THEME_ADS) + 1).coerceAtMost(REQUIRED_THEME_ADS)
                            reward = ThemeAdStatus.Rewarded(theme, count)
                            onReward(theme)
                            if (count == REQUIRED_THEME_ADS) onUnlocked(theme)
                        }
                    }
                }
            })
    }

    fun reset() { requestId++; status = ThemeAdStatus.Idle }
    fun dispose() { active = false; requestId++ }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val base = current.baseContext
        if (base === current) return null
        current = base
    }
    return null
}
