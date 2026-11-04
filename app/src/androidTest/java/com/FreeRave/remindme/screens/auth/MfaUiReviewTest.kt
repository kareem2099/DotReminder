package com.FreeRave.remindme.screens.auth

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.review.ReviewEnvironment
import com.FreeRave.remindme.screens.settings.AccountSecurityScreenContent
import com.FreeRave.remindme.screens.settings.AccountSecurityUiState
import com.FreeRave.remindme.screens.settings.components.TwoFactorAuthSetupDialog
import com.FreeRave.remindme.security.mfa.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Fake authenticator data and callbacks only. Never enrolls, removes or signs a real account in. */
@RunWith(AndroidJUnit4::class)
class MfaUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val factors = listOf(MfaFactor("one", "Phone authenticator"), MfaFactor("two", "Backup device"))
    private val account = MfaAccount("fake-user", "user@example.com", true, listOf(MfaProvider.PASSWORD, MfaProvider.GOOGLE, MfaProvider.FACEBOOK), factors)
    @Before fun isolatedRunnerOnly() { assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner) }

    @Test fun aChallengeRequiresSixDigitsAndNormalizesArabicInput() {
        var submitted: String? = null
        compose.setContent {
            ReviewEnvironment("light") {
                MfaChallengeDialog(MfaChallengeState(factors, "one"), { submitted = it }, {}, {}, {})
            }
        }
        node(R.string.mfa_verify).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("١٢٣٤٥٦")
        node(R.string.mfa_verify).performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("123456", submitted) }
    }
    @Test fun choosingABackupClearsThePreviousCode() {
        compose.setContent {
            var state by remember { mutableStateOf(MfaChallengeState(factors, "one")) }
            ReviewEnvironment("light") { MfaChallengeDialog(state, {}, { state = state.copy(selectedId = it) }, {}, {}) }
        }
        compose.onNode(hasSetTextAction()).performTextInput("123456")
        compose.onNodeWithText("Backup device").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals(context.getString(R.string.security_code), "")
        node(R.string.mfa_verify).assertIsNotEnabled()
    }
    @Test fun pendingVerificationCannotBeSubmittedTwiceOrDismissed() {
        var requests = 0
        compose.setContent {
            var state by remember { mutableStateOf(MfaChallengeState(factors, "one")) }
            ReviewEnvironment("light") {
                MfaChallengeDialog(state, { requests++; state = state.copy(busy = true) }, {}, {}, {})
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("123456")
        node(R.string.mfa_verify).performScrollTo().performClick()
        node(R.string.security_code).assertIsNotEnabled()
        node(R.string.security_cancel).assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, requests) }
    }
    @Test fun challengeSupportsLargeRtlTextAndKeyboard() {
        compose.setContent {
            ReviewEnvironment("rtl") {
                MfaChallengeDialog(MfaChallengeState(factors, "one", error = MfaError.INVALID_CODE), {}, {}, {}, {})
            }
        }
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("۱۲۳۴۵۶")
        node(R.string.mfa_verify).performScrollTo().assertIsDisplayed()
        node(R.string.mfa_error_code).performScrollTo().assertIsDisplayed()
        save("mfa-challenge-rtl.png")
    }
    @Test fun setupOffersQrAndManualKeyAndRequiresExplicitVerification() {
        var code: String? = null
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                TwoFactorAuthSetupDialog("FAKESETUPKEY23456", "otpauth://totp/Review?secret=FAKESETUPKEY23456&issuer=DotReminder",
                    { value, _ -> code = value }, {}, {}, false, null, {})
            }
        }
        node(R.string.mfa_enable).assertIsNotEnabled()
        compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.security_code)))
            .performScrollTo().performTextInput("184392")
        compose.runOnIdle { assertNull(code) }
        node(R.string.mfa_enable).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("184392", code) }
        save("mfa-setup-compact.png")
    }
    @Test fun unverifiedEmailShowsVerificationInsteadOfAnEnableSwitch() {
        var verifies = 0
        compose.setContent {
            ReviewEnvironment("light") {
                AccountSecurityScreenContent(AccountSecurityUiState(mfa = MfaSettingsState(account.copy(emailVerified = false, factors = emptyList()), loading = false)),
                    remember { SnackbarHostState() }, {}, {}, {}, {}, {}, { verifies++ })
            }
        }
        compose.onNodeWithText(context.getString(R.string.mfa_error_email)).performScrollTo().assertIsDisplayed()
        node(R.string.mfa_add).assertDoesNotExist()
        compose.onAllNodesWithText(context.getString(R.string.email_verification_title)).onLast().performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, verifies) }
    }
    @Test fun realFactorsCanBeReviewedAndRemovedIndividually() {
        var removed: MfaFactor? = null
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                AccountSecurityScreenContent(AccountSecurityUiState(mfa = MfaSettingsState(account, loading = false)),
                    remember { SnackbarHostState() }, {}, {}, {}, {}, {}, {}, onMfaRemove = { removed = it })
            }
        }
        val removeLabel = context.getString(R.string.mfa_remove, "Backup device")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(removeLabel))
        compose.onNodeWithText(removeLabel).performClick()
        compose.runOnIdle { assertEquals(factors[1], removed) }
        save("mfa-security-factors.png")
    }
    @Test fun socialReauthenticationUsesTheSelectedProviderWithoutAPassword() {
        var provider: MfaProvider? = null
        var password: String? = null
        compose.setContent {
            ReviewEnvironment("light") {
                MfaReauthenticationDialog(MfaSettingsState(account, loading = false, action = MfaAction.Add),
                    { selected, value -> provider = selected; password = value }, {}, {})
            }
        }
        node(R.string.mfa_continue).assertIsNotEnabled()
        node(R.string.mfa_provider_google).performClick()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        node(R.string.mfa_continue).performClick()
        compose.runOnIdle { assertEquals(MfaProvider.GOOGLE, provider); assertEquals("", password) }
    }
    @Test fun removingTheLastFactorExplainsThatProtectionWillBeTurnedOff() {
        compose.setContent {
            ReviewEnvironment("rtl") {
                MfaReauthenticationDialog(MfaSettingsState(account.copy(factors = factors.take(1)), loading = false,
                    action = MfaAction.Remove(factors[0])), { _, _ -> }, {}, {})
            }
        }
        node(R.string.mfa_remove_last).performScrollTo().assertIsDisplayed()
        node(R.string.mfa_continue).assertIsNotEnabled()
    }
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun save(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
