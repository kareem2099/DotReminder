package com.FreeRave.remindme.screens.auth

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.review.ReviewEnvironment
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated SMS UI: no messages are sent and no Firebase account is created. */
@RunWith(AndroidJUnit4::class)
class RegistrationCodeUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun isolatedRunnerOnly() { assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner) }
    @Test fun resendIsDisabledDuringCooldownAndEnabledAfterItEnds() {
        var state by mutableStateOf(SignUpUiState(step = SignUpStep.CODE, recipient = "+201012345678", resendSeconds = 48))
        var sends = 0
        compose.setContent { ReviewEnvironment("light") { Content(state, { sends++ }) } }
        compose.onNodeWithText(context.getString(R.string.signup_resend_wait, 48)).performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(resendSeconds = 0) }
        node(R.string.signup_resend).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, sends) }
    }
    @Test fun sendingCodeHasItsOwnProgressAndCanBeCancelled() {
        var cancellations = 0
        compose.setContent {
            ReviewEnvironment("light") {
                SignUpScreenContent(SignUpUiState(step = SignUpStep.CODE, recipient = "+201012345678", busy = true, sendingCode = true),
                    { _, _ -> }, {}, {}, {}, {}, {}, {}, onCancelSend = { cancellations++ })
            }
        }
        node(R.string.signup_creating).assertDoesNotExist()
        node(R.string.signup_sending).performScrollTo().assertIsNotEnabled()
        node(R.string.signup_cancel_send).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, cancellations) }
    }
    @Test fun aVerifiedPhoneDoesNotOfferAnotherSms() {
        compose.setContent { ReviewEnvironment("light") { Content(SignUpUiState(step = SignUpStep.CODE, phoneVerified = true), {}) } }
        node(R.string.signup_resend).assertDoesNotExist()
        node(R.string.signup_create_verified).performScrollTo().assertIsDisplayed()
    }
    @OptIn(ExperimentalLayoutApi::class)
    @Test fun codeAndResendRemainReachableWithLargeRtlTextAndKeyboard() {
        var imeBottom = 0
        var imeTarget = 0
        compose.setContent {
            var state by remember { mutableStateOf(SignUpUiState(step = SignUpStep.CODE, recipient = "+201012345678")) }
            ReviewEnvironment("rtl") {
                val density = LocalDensity.current
                val bottom = WindowInsets.ime.getBottom(density)
                val target = WindowInsets.imeAnimationTarget.getBottom(density)
                SideEffect { imeBottom = bottom; imeTarget = target }
                SignUpScreenContent(state, { field, value -> if (field == SignUpField.CODE) state = state.copy(code = value) },
                    {}, {}, {}, {}, {}, {}, onResend = {})
            }
        }
        node(R.string.signup_code).performScrollTo().performClick().performTextInput("١٨٤٣٩٢")
        compose.waitUntil(5_000) { imeBottom > 0 && imeBottom == imeTarget }
        node(R.string.signup_resend).performScrollTo()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(context.cacheDir, "registration-code-rtl.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        node(R.string.signup_resend).assertIsDisplayed()
        node(R.string.signup_create).performScrollTo().assertIsDisplayed()
    }
    @Composable private fun Content(state: SignUpUiState, onResend: () -> Unit) {
        SignUpScreenContent(state, { _, _ -> }, {}, {}, {}, {}, {}, {}, onResend = onResend)
    }
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
}
