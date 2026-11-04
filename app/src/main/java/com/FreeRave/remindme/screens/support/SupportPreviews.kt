package com.FreeRave.remindme.screens.support

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.support.components.SupportBannerFrame
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Preview(name = "Support · Light", widthDp = 393, heightDp = 852)
@Composable
private fun SupportLightPreview() = SupportPreview(false)
@Preview(name = "Support · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun SupportDarkPreview() = SupportPreview(true)
@Preview(name = "Support · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun SupportCompactPreview() = SupportPreview(false)
@Preview(name = "Support · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun SupportTabletPreview() = SupportPreview(false)
@Preview(name = "Support · Loading", widthDp = 393, heightDp = 852)
@Composable
private fun SupportLoadingPreview() = SupportPreview(true, SupportAdState(SupportAdPhase.LOADING, SupportAdKind.STANDARD))
@Preview(name = "Support · Ad unavailable", widthDp = 393, heightDp = 852)
@Composable
private fun SupportFailedPreview() = SupportPreview(true, SupportAdState(SupportAdPhase.FAILED, SupportAdKind.STANDARD))
@Preview(name = "Support · Thank you", widthDp = 393, heightDp = 852)
@Composable
private fun SupportThanksPreview() = SupportPreview(true, SupportAdState(SupportAdPhase.THANK_YOU, SupportAdKind.REWARDED))

@Composable
private fun SupportPreview(dark: Boolean, state: SupportAdState = SupportAdState()) {
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            SupportScreenContent(state, onBack = {}, onRequest = {}, onCancel = {}, onThanked = {}, banner = {
                SupportBannerFrame {
                    Box(Modifier.fillMaxWidth().heightIn(min = 80.dp), contentAlignment = Alignment.Center) {
                        Text("Ad preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            })
        }
    }
}
