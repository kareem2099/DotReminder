package com.FreeRave.remindme.screens.support

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** SDK integration stays outside the screen and can show only the request the user is waiting for. */
internal class SupportAdController(private val activity: Activity?) {
    private val session = SupportAdSession()
    val state = session.state

    fun request(kind: SupportAdKind) {
        val token = session.request(kind) ?: return
        val host = activity
        if (host == null || !host.canShowAd()) { session.failed(token); return }
        try {
            load(kind, host, token)
        } catch (_: RuntimeException) { session.failed(token) }
    }

    private fun load(kind: SupportAdKind, host: Activity, token: Long) {
        when (kind) {
            SupportAdKind.STANDARD -> InterstitialAd.load(host, SupportAdUnits.standard, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) { session.failed(token) }
                    override fun onAdLoaded(ad: InterstitialAd) {
                        show(token, host) {
                            ad.fullScreenContentCallback = callbacks(token)
                            ad.show(host)
                        }
                    }
                })
            SupportAdKind.REWARDED -> RewardedAd.load(host, SupportAdUnits.rewarded, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) { session.failed(token) }
                    override fun onAdLoaded(ad: RewardedAd) {
                        show(token, host) {
                            ad.fullScreenContentCallback = callbacks(token)
                            ad.show(host) { session.reward(token) }
                        }
                    }
                })
        }
    }

    private fun show(token: Long, host: Activity, action: () -> Unit) {
        if (!host.canShowAd()) { session.failed(token); return }
        if (!session.loaded(token)) return
        try { action() } catch (_: RuntimeException) { session.failed(token) }
    }

    private fun callbacks(token: Long) = object : FullScreenContentCallback() {
        override fun onAdFailedToShowFullScreenContent(error: AdError) { session.failed(token) }
        override fun onAdDismissedFullScreenContent() { session.dismissed(token) }
    }

    fun cancelLoading() = session.cancelLoading()
    fun dismissThanks() = session.dismissThanks()
    fun dispose() = session.dispose()
}

private fun Activity.canShowAd(): Boolean = !isFinishing && !isDestroyed &&
    ((this as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: hasWindowFocus())
