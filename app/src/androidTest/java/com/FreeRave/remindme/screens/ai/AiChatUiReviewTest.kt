package com.FreeRave.remindme.screens.ai

import android.graphics.Bitmap
import android.os.Parcel
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.AiReminderDraft
import com.FreeRave.remindme.navigation.Screen
import com.FreeRave.remindme.screens.auth.LoginUiReviewRunner
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Stateless review: no model calls, Firebase writes or notification scheduling. */
@RunWith(AndroidJUnit4::class)
class AiChatUiReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val user = ChatMessage(text = "Remind me to call Mum tomorrow at 6 pm", sender = SenderType.USER)
    private val draft = AiReminderDraft("Call Mum", System.currentTimeMillis() + 86_400_000, category = "Personal",
        sourceText = "Call Mum tomorrow at 6 pm")
    private val conversation = listOf(user, ChatMessage(sender = SenderType.AI, draft = draft))
    @Before fun isolatedRunnerOnly() {
        assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner)
    }
    @Test fun lightWelcomeShowsExamplesAndDisabledEmptySend() {
        render(false)
        node(R.string.ai_chat_empty_title).assertIsDisplayed()
        send().assertIsNotEnabled()
        save("ai-light-welcome-review.png")
    }
    @Test fun aStartingPointFillsTheComposerWithoutSending() {
        var selected: String? = null
        var sends = 0
        render(false, onInput = { selected = it }, onSend = { sends++ })
        node(R.string.ai_chat_example_call).performClick()
        compose.runOnIdle {
            assertEquals(context.getString(R.string.ai_chat_example_call), selected)
            assertEquals(0, sends)
        }
    }
    @Test fun darkDraftIsMarkedUnsavedAndReviewPassesTheDetails() {
        var reviewed: AiReminderDraft? = null
        render(true, AiChatUiState(messages = conversation), onReview = { reviewed = it })
        scrollTo(R.string.ai_chat_review)
        node(R.string.ai_chat_review_hint).assertIsDisplayed()
        save("ai-dark-draft-review.png")
        node(R.string.ai_chat_review).performClick()
        compose.runOnIdle { assertEquals(draft, reviewed) }
    }
    @Test fun aSavedDraftOpensTheExistingReminderInsteadOfOfferingAnotherSave() {
        var opened: String? = null
        compose.setContent {
            ReviewTheme(true) {
                AiChatScreenContent(AiChatUiState(messages = listOf(ChatMessage(sender = SenderType.AI, draft = draft, savedReminderId = "saved-1"))),
                    onBack = {}, onInput = {}, onSend = {}, onRetry = {}, onClear = {}, onReview = {}, onManual = {}, onOpen = { opened = it })
            }
        }
        scrollTo(R.string.ai_chat_open)
        node(R.string.ai_chat_review).assertDoesNotExist()
        node(R.string.ai_chat_review_hint).assertDoesNotExist()
        node(R.string.ai_chat_open).performClick()
        compose.runOnIdle { assertEquals("saved-1", opened) }
    }
    @Test fun pendingReplyDisablesSendButAllowsTheNextDraft() {
        render(true, AiChatUiState(input = "Next thought", messages = listOf(user), busy = true))
        send().assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).assertIsEnabled()
        scrollTo(R.string.ai_chat_thinking)
        node(R.string.ai_chat_thinking).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.ai_chat_new)).assertIsNotEnabled()
    }
    @Test fun networkErrorOffersAnExplicitRetry() {
        var retries = 0
        render(false, AiChatUiState(messages = listOf(user), error = AiChatError.NETWORK, failedMessageId = user.id),
            onRetry = { retries++ })
        scrollTo(R.string.ai_chat_retry)
        node(R.string.ai_chat_retry).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        save("ai-error-review.png")
    }
    @Test fun clearingRequiresConfirmationAndCanBeCancelled() {
        var clears = 0
        render(true, AiChatUiState(messages = conversation), onClear = { clears++ })
        compose.onNodeWithContentDescription(context.getString(R.string.ai_chat_new)).performClick()
        compose.runOnIdle { assertEquals(0, clears) }
        node(R.string.ai_chat_keep).performClick()
        compose.runOnIdle { assertEquals(0, clears) }
        compose.onNodeWithContentDescription(context.getString(R.string.ai_chat_new)).performClick()
        node(R.string.ai_chat_clear).performClick()
        compose.runOnIdle { assertEquals(1, clears) }
    }
    @Test fun composerStaysVisibleWithKeyboardAndImeSendWorks() {
        var sends = 0
        compose.setContent {
            var state by remember { mutableStateOf(AiChatUiState()) }
            ReviewTheme(false) {
                AiChatScreenContent(state, onBack = {}, onInput = { state = state.copy(input = it) }, onSend = { sends++ },
                    onRetry = {}, onClear = {}, onReview = {}, onManual = {})
            }
        }
        compose.onNode(hasSetTextAction()).performClick().performTextInput("Call Mum tomorrow at 6 pm")
        send().assertIsDisplayed().assertIsEnabled()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(1, sends) }
    }
    @Test fun oversizedInputCannotBeSentAndShowsTheLimit() {
        render(false, AiChatUiState(input = "a".repeat(AI_MESSAGE_LIMIT + 1)))
        send().assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.ai_chat_input_limit, AI_MESSAGE_LIMIT + 1, AI_MESSAGE_LIMIT)).assertIsDisplayed()
    }
    @Test fun largeTextAndArabicMessagesKeepTheComposerAccessible() {
        render(true, AiChatUiState(input = "فكرني بالمذاكرة بكرة", messages = listOf(
            ChatMessage(text = "فكرني إني أذاكر التاريخ بكرة الساعة ستة بالليل", sender = SenderType.USER),
            ChatMessage(text = "مسودة التذكير جاهزة للمراجعة. ".repeat(20), sender = SenderType.AI))), largeText = true)
        send().assertIsDisplayed().assertIsEnabled()
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
    }
    @Test fun reviewingOpensTheAddFormOnceWithTheUnsavedPayload() {
        lateinit var nav: NavHostController
        compose.setContent {
            ReviewTheme(false) {
                nav = rememberNavController()
                NavHost(nav, startDestination = Screen.AiChat.route) {
                    composable(Screen.AiChat.route) {
                        AiChatScreenContent(AiChatUiState(messages = conversation), onBack = {}, onInput = {}, onSend = {}, onRetry = {},
                            onClear = {}, onReview = { openAiReminderForm(nav, it); openAiReminderForm(nav, it) }, onManual = {})
                    }
                    composable(Screen.Add.route) { Text("Review add form") }
                }
            }
        }
        scrollTo(R.string.ai_chat_review)
        node(R.string.ai_chat_review).performClick()
        compose.onNodeWithText("Review add form").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(Screen.AiChat.route, nav.previousBackStackEntry?.destination?.route)
            assertEquals(draft, nav.currentBackStackEntry?.savedStateHandle?.get<AiReminderDraft>(AI_REMINDER_DRAFT_KEY))
        }
    }
    @Suppress("DEPRECATION")
    @Test fun draftPayloadPreservesScheduleThroughAndroidSavedState() {
        val recurring = draft.copy(isRecurring = true, recurrenceIntervalMillis = 7_200_000, recurrenceEndTime = draft.reminderTime + 86_400_000)
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(recurring, 0); parcel.setDataPosition(0)
            val restored = parcel.readParcelable<AiReminderDraft>(AiReminderDraft::class.java.classLoader)
            assertEquals(recurring, restored)
        } finally { parcel.recycle() }
    }
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun send() = compose.onNodeWithContentDescription(context.getString(R.string.ai_chat_send))
    private fun scrollTo(id: Int) = compose.onNodeWithTag("ai-chat-messages").performScrollToNode(hasText(context.getString(id)))
    private fun render(dark: Boolean, state: AiChatUiState = AiChatUiState(), largeText: Boolean = false,
        onInput: (String) -> Unit = {}, onSend: () -> Unit = {}, onRetry: () -> Unit = {}, onClear: () -> Unit = {},
        onReview: (AiReminderDraft) -> Unit = {}) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else density.fontScale)) {
                ReviewTheme(dark) {
                    AiChatScreenContent(state, onBack = {}, onInput = onInput, onSend = onSend, onRetry = onRetry,
                        onClear = onClear, onReview = onReview, onManual = {})
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
