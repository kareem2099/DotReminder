package com.FreeRave.remindme.screens.support.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.FreeRave.remindme.screens.support.SupportAdUnits
import com.FreeRave.remindme.R
import com.FreeRave.remindme.ui.components.GlassSurface
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private enum class BannerStatus { LOADING, READY, FAILED }

@Composable
internal fun SupportBannerCard() {
    SupportBannerFrame {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val context = LocalContext.current
            val orientation = LocalConfiguration.current.orientation
            val width = maxWidth.value.toInt().coerceAtLeast(1)
            val size = remember(context, width, orientation) {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
            }
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val adView = remember(context, size, lifecycle) {
                AdView(context).apply { setAdSize(size); adUnitId = SupportAdUnits.banner }
            }
            var status by remember(adView) { mutableStateOf(BannerStatus.LOADING) }
            DisposableEffect(adView, lifecycle) {
                var active = true
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() { if (active) status = BannerStatus.READY }
                    override fun onAdFailedToLoad(error: LoadAdError) { if (active) status = BannerStatus.FAILED }
                }
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> adView.resume()
                        Lifecycle.Event.ON_PAUSE -> adView.pause()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                adView.loadAd(AdRequest.Builder().build())
                onDispose {
                    active = false
                    lifecycle.removeObserver(observer)
                    adView.destroy()
                }
            }
            val height = with(LocalDensity.current) { size.getHeightInPixels(context).toDp() }.coerceAtLeast(50.dp)
            Box(Modifier.fillMaxWidth().heightIn(min = height), contentAlignment = Alignment.Center) {
                key(adView) { AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(height)) }
                if (status != BannerStatus.READY) Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().heightIn(min = height).padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (status == BannerStatus.LOADING) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.support_banner_loading), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(stringResource(R.string.support_banner_unavailable), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { status = BannerStatus.LOADING; adView.loadAd(AdRequest.Builder().build()) }) {
                                Text(stringResource(R.string.support_retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SupportBannerFrame(content: @Composable () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.support_banner_label), modifier = Modifier.padding(start = 18.dp, top = 16.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
            Spacer(Modifier.height(4.dp))
        }
    }
}
