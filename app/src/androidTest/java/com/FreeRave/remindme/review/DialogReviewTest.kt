package com.FreeRave.remindme.review

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.remindme.R
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.screens.add.SaveTemplateNameDialog
import com.FreeRave.remindme.screens.auth.LoginUiReviewRunner
import com.FreeRave.remindme.screens.category.*
import com.FreeRave.remindme.screens.category.components.CategoryEditorSheet
import com.FreeRave.remindme.screens.home.*
import com.FreeRave.remindme.screens.settings.*
import com.FreeRave.remindme.screens.settings.components.ChangePasswordDialog
import com.FreeRave.remindme.screens.settings.components.SoundBottomSheet
import com.FreeRave.remindme.screens.templates.*
import com.FreeRave.remindme.screens.templates.components.TemplateEditorSheet
import com.FreeRave.remindme.ui.components.SnoozeBottomSheet
import com.FreeRave.remindme.ui.components.ThemeBottomSheet
import com.FreeRave.remindme.ui.location.*
import com.FreeRave.remindme.ui.reminder.components.ReminderDateDialog
import com.FreeRave.remindme.ui.reminder.components.ReminderTimeDialog
import com.FreeRave.remindme.ui.theme.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.ZoneId

/** Fake form callbacks only. No GPS, map SDK, ads, sound preview or account writes. */
@RunWith(AndroidJUnit4::class)
class DialogReviewTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun isolatedRunnerOnly() { assumeTrue(InstrumentationRegistry.getInstrumentation() is LoginUiReviewRunner) }
    @Test fun invalidLocationCannotBeAccepted() {
        var saves = 0
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                LocationEditorDialog(LocationDraft("999", "NaN", "-1"), onChange = {}, onSelected = { _, _, _ -> saves++ },
                    onDismiss = {}, onCurrent = {}, onSearch = {})
            }
        }
        node(R.string.polish_location_save).performScrollTo().performClick()
        node(R.string.polish_radius_error).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, saves) }
        screenshot("location-errors")
    }
    @Test fun locationSupportsArabicCoordinatesAndKeyboardWithoutClippingSave() {
        var selected: Triple<Double, Double, Float>? = null
        compose.setContent {
            var draft by remember { mutableStateOf(LocationDraft("٣٠٫٢٥", "٣١٫٢٥", "٢٠٠")) }
            ReviewEnvironment("rtl") {
                LocationEditorDialog(draft, onChange = { draft = it }, onSelected = { a, b, c -> selected = Triple(a, b, c) },
                    onDismiss = {}, onCurrent = {}, onSearch = {})
            }
        }
        node(R.string.polish_radius).performScrollTo().performClick()
        node(R.string.polish_location_save).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(Triple(30.25, 31.25, 200f), selected) }
        screenshot("location-rtl")
    }
    @Test fun mapDialogLayoutKeepsCoordinatesAndActionsReachable() {
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                LocationEditorDialog(LocationDraft("30", "31", "200"), onChange = {}, onSelected = { _, _, _ -> },
                    onDismiss = {}, onCurrent = {}, onSearch = {}, map = {
                        Surface(Modifier.fillMaxWidth().height(230.dp)) { Text("Map review placeholder") }
                    })
            }
        }
        node(R.string.polish_radius).performScrollTo().assertIsDisplayed()
        node(R.string.polish_location_save).performScrollTo().assertIsDisplayed()
    }
    @Test fun timeInputCanConfirmArabicMinuteDigitsWithTheKeyboardOpen() {
        var selected: Long? = null
        val original = System.currentTimeMillis()
        compose.setContent { ReviewEnvironment("compact-dark") { ReminderTimeDialog(original, {}, { selected = it }) } }
        node(R.string.polish_minute).performScrollTo().performTextReplacement("٤٥")
        node(R.string.reminder_confirm).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(45, Instant.ofEpochMilli(selected!!).atZone(ZoneId.systemDefault()).minute) }
        screenshot("time-large-text")
    }
    @Test fun invalidTimeDoesNotInvokeTheSelectionCallback() {
        var calls = 0
        compose.setContent { ReviewEnvironment("rtl") { ReminderTimeDialog(System.currentTimeMillis(), {}, { calls++ }) } }
        node(R.string.polish_hour).performScrollTo().performTextReplacement("99")
        node(R.string.polish_minute).performScrollTo().performTextReplacement("99")
        node(R.string.reminder_confirm).performScrollTo().performClick()
        node(R.string.polish_minute_error).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, calls) }
    }
    @Test fun categorySheetSaveStaysAboveTheKeyboard() {
        compose.setContent {
            var editor by remember { mutableStateOf(CategoryEditorState(draft = CategoryDraft(name = "Review"))) }
            ReviewEnvironment("compact-dark") {
                CategoryEditorSheet(editor, true, onChange = { editor = editor.copy(draft = it) }, onSave = {}, onDismiss = {})
            }
        }
        node(R.string.category_name).performClick()
        node(R.string.category_save).assertIsDisplayed().assertIsEnabled()
        node(R.string.category_cancel).assertIsDisplayed()
    }
    @Test fun templateSheetSaveStaysAboveTheKeyboard() {
        compose.setContent {
            var editor by remember { mutableStateOf(TemplateEditorState(draft = TemplateDraft(name = "Review", title = "Call Mum"))) }
            ReviewEnvironment("compact-dark") {
                TemplateEditorSheet(editor, emptyList(), true, onChange = { editor = editor.copy(draft = it) }, onSave = {}, onDismiss = {})
            }
        }
        node(R.string.templates_name).performClick()
        node(R.string.templates_save).assertIsDisplayed().assertIsEnabled()
        node(R.string.templates_cancel).assertIsDisplayed()
    }
    @Test fun savingATemplateNameWorksWithLargeTextAndKeyboard() {
        var savedName: String? = null
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                SaveTemplateNameDialog(false, false, onDismiss = {}, onEdit = {}, onSave = { savedName = it })
            }
        }
        node(R.string.reminder_template_name).performClick().performTextInput("  Daily walk  ")
        node(R.string.reminder_confirm).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Daily walk", savedName) }
    }
    @Test fun passwordDialogCanReachItsActionsWithLargeTextAndKeyboard() {
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                ChangePasswordDialog(onConfirm = { _, _ -> }, onDismiss = {}, busy = false, error = null, onInputChanged = {})
            }
        }
        node(R.string.security_current_password).performClick()
        node(R.string.security_update_password).performScrollTo().assertIsDisplayed()
        node(R.string.security_cancel).performScrollTo().assertIsDisplayed()
    }
    @Test fun datePickerActionsRemainAccessible() {
        compose.setContent { ReviewEnvironment("compact-dark") { ReminderDateDialog(System.currentTimeMillis(), {}, {}) } }
        node(R.string.reminder_confirm).assertIsDisplayed()
        node(R.string.reminder_cancel).assertIsDisplayed()
        screenshot("date-large-text")
    }
    @Test fun snoozeSheetSupportsEnlargedText() {
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                SnoozeBottomSheet(true, listOf(SnoozeOption("5", 5), SnoozeOption("120", 120)), 5, {}, {})
            }
        }
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.settings_minutes, 120, 120)).assertIsDisplayed()
    }
    @Test fun soundSheetRendersWithoutPlayingAPreview() {
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                SoundBottomSheet(listOf(SoundOption("Default sound", "default")), "default", {}, {})
            }
        }
        compose.onNodeWithText("Default sound").assertIsDisplayed()
    }
    @Test fun themeSheetRendersWithoutRequestingAnAd() {
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                ThemeBottomSheet(true, ThemeSetting.entries.map { ThemeOption(it.name, it) }, ThemeSetting.DARK,
                    ThemeUnlockProgress(), onThemeSelected = {}, onReward = {}, onDismiss = {})
            }
        }
        node(R.string.settings_choose_theme).assertIsDisplayed()
    }
    @Test fun homeLoadFailureOffersRetryInsteadOfAnEmptyLibraryMessage() {
        var retries = 0
        compose.setContent {
            ReviewEnvironment("compact-dark") {
                HomeScreenContent(HomeUiState(hasError = true), System.currentTimeMillis(), onNavigate = {}, onSearchChange = {},
                    onCategoryChange = {}, onQuickFilterChange = {}, onRefresh = { retries++ }, onDelete = {}, onResetFilters = {})
            }
        }
        compose.onNodeWithTag("home-reminders").performScrollToNode(hasText(context.getString(R.string.polish_retry)))
        node(R.string.polish_retry).performClick()
        node(R.string.home_empty_title).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, retries) }
    }
    @Test fun allThemeVariantsKeepTextAndButtonContrastReadable() {
        val pairs = mutableMapOf<String, List<Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>>>()
        compose.setContent {
            Column {
                ThemeSetting.entries.forEach { theme ->
                    listOf(false, true).forEach { dark ->
                        RemindMeAppTheme(theme, useDarkTheme = dark) {
                            GlassTheme {
                                val colors = MaterialTheme.colorScheme
                                SideEffect {
                                    pairs["$theme-$dark"] = listOf(colors.primary to colors.onPrimary, colors.error to colors.onError,
                                        colors.surface to colors.onSurface, colors.surfaceContainerHighest to colors.onSurfaceVariant,
                                        colors.primaryContainer to colors.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(ThemeSetting.entries.size * 2, pairs.size)
            pairs.forEach { (theme, combinations) -> combinations.forEach { (a, b) ->
                assertTrue("$theme: contrast ${colorContrast(a, b)}", colorContrast(a, b) >= 4.5f)
            } }
        }
    }
    private fun node(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun screenshot(name: String) {
        val image = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(context.cacheDir, "review-$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
