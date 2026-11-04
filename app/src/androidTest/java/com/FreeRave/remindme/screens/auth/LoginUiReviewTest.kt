package com.FreeRave.remindme.screens.auth

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.auth.components.PasswordResetDialog
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Run with -PuiReviewRunner=com.FreeRave.remindme.screens.auth.LoginUiReviewRunner. */
@RunWith(AndroidJUnit4::class)
class LoginUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun isolatedRunnerOnly() {
        assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner)
    }

    @Test fun googleSignInDisablesOtherRequests() {
        render(dark = true, state = LoginUiState(busy = true, method = LoginMethod.GOOGLE))
        compose.onNodeWithText(context.getString(R.string.login_sign_in)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.login_google_loading)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.login_facebook)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.login_create_account)).assertIsNotEnabled()
    }

    @Test fun malformedResetEmailIsRejectedBeforeTheSendCallback() {
        var sends = 0
        compose.setContent {
            RemindMeAppTheme(ThemeSetting.DARK, useDarkTheme = true) {
                GlassTheme {
                    PasswordResetDialog("not-an-email", PasswordResetState.Idle,
                        onSend = { sends++ }, onDismiss = {}, onEdit = {})
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.login_reset_send)).performClick()
        compose.onNodeWithText(context.getString(R.string.login_email_error)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, sends) }
    }

    @Test fun lightLayoutReview() {
        render(dark = false)
        saveImage("login-light-review.png")
    }

    @Test fun darkLayoutReview() {
        render(dark = true)
        saveImage("login-dark-review.png")
    }

    private fun render(dark: Boolean, state: LoginUiState = LoginUiState()) {
        compose.setContent {
            RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
                GlassTheme {
                    val snackbar = remember { SnackbarHostState() }
                    LoginScreenContent(state, snackbar, onEmail = {}, onPassword = {}, onRemember = {}, onSubmit = {},
                        onForgot = {}, onGoogle = {}, onFacebook = {}, onBiometric = {}, onSignUp = {})
                }
            }
        }
    }

    private fun saveImage(name: String) {
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
