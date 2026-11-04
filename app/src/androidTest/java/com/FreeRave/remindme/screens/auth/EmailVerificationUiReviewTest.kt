package com.FreeRave.remindme.screens.auth

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.settings.AccountSecurityScreenContent
import com.FreeRave.remindme.screens.settings.AccountSecurityUiState
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Component review only: fake callbacks never send email or change the current account. */
@RunWith(AndroidJUnit4::class)
class EmailVerificationUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val ready = EmailVerificationUiState(phase = EmailVerificationPhase.READY,
        account = VerificationAccount("review", "user@example.com", false))
    @Before fun isolatedRunnerOnly() {
        assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner)
    }
    @Test fun lightReadyLayoutDoesNotClaimAnEmailWasSent() {
        var clicks = 0
        render(false, onSend = { clicks++ })
        node(R.string.email_verification_sent).assertDoesNotExist()
        node(R.string.email_verification_send).performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
        save("email-light-review.png")
    }
    @Test fun darkSentLayoutShowsCooldownAndAllowsStatusCheck() {
        render(true, ready.copy(notice = EmailVerificationNotice.SENT, hasSentLink = true, resendSeconds = 42))
        node(R.string.email_verification_sent).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.email_verification_resend_wait, 42)).performScrollTo().assertIsNotEnabled()
        node(R.string.email_verification_check).performScrollTo().assertIsEnabled()
        save("email-dark-sent-review.png")
    }
    @Test fun sendInProgressDisablesBothRequests() {
        render(true, ready.copy(operation = EmailVerificationOperation.SEND))
        node(R.string.email_verification_sending).performScrollTo().assertIsNotEnabled()
        node(R.string.email_verification_check).performScrollTo().assertIsNotEnabled()
    }
    @Test fun verifiedStateShowsContinueAndNoSendAction() {
        render(true, ready.copy(phase = EmailVerificationPhase.VERIFIED, account = ready.account?.copy(verified = true)))
        node(R.string.email_verification_done_title).assertIsDisplayed()
        node(R.string.email_verification_continue).performScrollTo().assertIsEnabled()
        node(R.string.email_verification_send).assertDoesNotExist()
        save("email-verified-review.png")
    }
    @Test fun syncFailureOffersIndependentRetry() {
        render(false, ready.copy(phase = EmailVerificationPhase.VERIFIED, error = EmailVerificationError.SYNC))
        node(R.string.email_verification_sync_retry).performScrollTo().assertIsEnabled()
        node(R.string.email_verification_continue).performScrollTo().assertIsEnabled()
        node(R.string.email_verification_send).assertDoesNotExist()
    }
    @Test fun phoneOnlyAccountHasNoEmailActions() {
        render(false, ready.copy(phase = EmailVerificationPhase.NO_EMAIL, account = ready.account?.copy(email = "")))
        node(R.string.email_verification_no_email_title).assertIsDisplayed()
        node(R.string.email_verification_send).assertDoesNotExist()
        node(R.string.email_verification_check).assertDoesNotExist()
    }
    @Test fun signedOutPageShowsSignInWithoutSendingEmail() {
        render(true, EmailVerificationUiState(phase = EmailVerificationPhase.SIGNED_OUT))
        node(R.string.login_sign_in).performScrollTo().assertIsEnabled()
        node(R.string.email_verification_send).assertDoesNotExist()
    }
    @Test fun enlargedTextCanReachBothActionsAndLongAddress() {
        render(false, ready.copy(account = ready.account?.copy(email = "long.email.address.for.layout.review@example.com")), largeText = true)
        node(R.string.email_verification_send).performScrollTo().assertIsDisplayed()
        node(R.string.email_verification_check).performScrollTo().assertIsDisplayed()
    }
    @Test fun accountSecurityEntryOpensVerificationCallback() {
        var opens = 0
        compose.setContent {
            ReviewTheme(false) {
                val snackbar = remember { SnackbarHostState() }
                AccountSecurityScreenContent(AccountSecurityUiState(), snackbar, onBack = {}, onPassword = {}, onBiometric = {},
                    onDevices = {}, onMfaAdd = {}, onVerifyEmail = { opens++ })
            }
        }
        node(R.string.email_verification_title).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun render(dark: Boolean, state: EmailVerificationUiState = ready, largeText: Boolean = false, onSend: () -> Unit = {}) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else density.fontScale)) {
                ReviewTheme(dark) {
                    VerifyEmailScreenContent(state, onBack = {}, onCheck = {}, onSend = onSend, onRetry = {},
                        onRetrySync = {}, onContinue = {}, onLogin = {})
                }
            }
        }
    }
    @Composable private fun ReviewTheme(dark: Boolean, content: @Composable () -> Unit) {
        RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) { GlassTheme(content) }
    }
    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
