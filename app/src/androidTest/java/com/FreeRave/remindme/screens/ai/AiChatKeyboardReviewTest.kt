package com.FreeRave.remindme.screens.ai

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.auth.LoginUiReviewRunner
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Uses the production window policy with fake messages, without starting app services. */
@RunWith(AndroidJUnit4::class)
class AiChatKeyboardReviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun keyboardDoesNotPanTheHeaderOrLeaveAGapBelowTheComposer() {
        assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner)
        val activity = compose.activity
        compose.runOnUiThread {
            activity.enableEdgeToEdge()
        }
        compose.setContent {
            var state by remember { mutableStateOf(AiChatUiState()) }
            RemindMeAppTheme(ThemeSetting.DARK, useDarkTheme = true) {
                GlassTheme {
                    AiChatScreenContent(state, onBack = {}, onInput = { state = state.copy(input = it) }, onSend = {
                        state = state.copy(input = "", messages = state.messages + listOf(
                            ChatMessage(text = state.input, sender = SenderType.USER),
                            ChatMessage(text = "Test reply", sender = SenderType.AI)))
                    }, onRetry = {}, onClear = {}, onReview = {}, onManual = {})
                }
            }
        }
        val input = compose.onNode(hasSetTextAction())
        input.performClick().performTextInput("A test message")
        assertKeyboardLayout()
        screenshot("ai-keyboard-before-send.png")
        input.performImeAction()
        compose.onNodeWithText("Test reply").assertIsDisplayed()
        assertKeyboardLayout()
        compose.runOnUiThread {
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        compose.waitUntil(5_000) { !keyboardVisible() }
        input.performClick().performTextInput("Another message")
        assertKeyboardLayout()
        screenshot("ai-keyboard-after-reopen.png")
    }

    private fun keyboardVisible() = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true

    private fun assertKeyboardLayout() {
        compose.waitUntil(5_000) { keyboardVisible() }
        // Wait for IME animation completion; just being visible includes its first animation frame.
        var geometry = ""
        try { compose.waitUntil(5_000) {
            val decor = compose.activity.window.decorView
            val insets = ViewCompat.getRootWindowInsets(decor) ?: return@waitUntil false
            val keyboardTop = decor.height - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val composerBottom = compose.onNodeWithTag("ai-chat-composer").fetchSemanticsNode().boundsInWindow.bottom
            geometry = "policy=${compose.activity.window.attributes.softInputMode}, decorHeight=${decor.height}, keyboardTop=$keyboardTop, composerBottom=$composerBottom, insets=${insets.getInsets(WindowInsetsCompat.Type.ime())}"
            kotlin.math.abs(keyboardTop - composerBottom) <= decor.resources.displayMetrics.density * 2
        } } catch (failure: ComposeTimeoutException) {
            screenshot("ai-keyboard-layout-failure.png")
            throw AssertionError("Composer must sit above the keyboard: $geometry", failure)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.ai_chat_title)).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.ai_chat_send)).assertIsDisplayed()
        compose.runOnUiThread {
            val location = IntArray(2)
            compose.activity.window.decorView.getLocationOnScreen(location)
            assertTrue("The whole window must not pan above the status bar", location[1] >= 0)
        }
    }

    private fun screenshot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.cacheDir, name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
