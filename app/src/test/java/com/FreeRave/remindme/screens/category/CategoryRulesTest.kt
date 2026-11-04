package com.FreeRave.remindme.screens.category

import com.FreeRave.remindme.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRulesTest {
    private val custom = Category(id = "travel-123", userId = "owner", name = "Travel", colorHex = "#FF42A5F5",
        description = "Flights and trains", icon = "✈️", createdAt = 100, updatedAt = 200)

    @Test
    fun emptyNamesAreRejected() {
        assertEquals(setOf(CategoryFieldError.NAME_REQUIRED), CategoryDraft(name = "  ").validate(builtInCategories, null))
    }

    @Test
    fun duplicateCustomNamesAreDetectedIgnoringCaseAndOuterSpaces() {
        val categories = mergeCategories(listOf(custom))
        assertEquals(setOf(CategoryFieldError.NAME_DUPLICATE), CategoryDraft(name = " TRAVEL ").validate(categories, null))
    }

    @Test
    fun aCustomCategoryCanKeepItsOwnNameWhileChangingAppearance() {
        val draft = custom.toDraft().copy(icon = "🚆", colorHex = "#FF66BB6A")
        assertTrue(draft.validate(mergeCategories(listOf(custom)), custom).isEmpty())
    }

    @Test
    fun renamingACustomCategoryCannotCollideWithAnotherCategory() {
        assertEquals(setOf(CategoryFieldError.NAME_DUPLICATE),
            custom.toDraft().copy(name = "work").validate(mergeCategories(listOf(custom)), custom))
    }

    @Test
    fun customisingABuiltInCategoryCanKeepItsName() {
        val work = builtInCategories.first { it.name == "Work" }
        assertTrue(work.toDraft().copy(icon = "🎯").validate(builtInCategories, work).isEmpty())
        assertEquals(setOf(CategoryFieldError.NAME_DUPLICATE), work.toDraft().copy(name = "Personal").validate(builtInCategories, work))
    }

    @Test
    fun anIncomingCustomOverrideBlocksASecondOverrideWithTheSameName() {
        val work = builtInCategories.first { it.name == "Work" }
        val override = custom.copy(id = "work-override", name = "Work")
        assertEquals(setOf(CategoryFieldError.NAME_DUPLICATE), work.toDraft().validate(mergeCategories(listOf(override)), work))
    }

    @Test
    fun customOverridesReplaceOnlyTheMatchingBuiltInCategory() {
        val override = custom.copy(name = " work ")
        val categories = mergeCategories(listOf(override))
        assertEquals(4, categories.size)
        assertEquals(override, categories.first())
        assertFalse(categories.any { it.isPredefined && it.name == "Work" })
        assertTrue(categories.any { it.isPredefined && it.name == "Personal" })
    }

    @Test
    fun removingAnOverrideRestoresTheBuiltInCategory() {
        val override = custom.copy(name = "Work")
        val remaining = mergeCategories(listOf(override).filterNot { it.id == override.id })
        assertEquals(builtInCategories, remaining)
    }

    @Test
    fun searchAndTypeFiltersApplyTogether() {
        val categories = mergeCategories(listOf(custom))
        assertEquals(listOf(custom), filterCategories(categories, " TRAINS ", CategoryFilter.ALL))
        assertEquals(listOf(custom), filterCategories(categories, "travel", CategoryFilter.CUSTOM))
        assertTrue(filterCategories(categories, "travel", CategoryFilter.BUILT_IN).isEmpty())
        assertEquals(builtInCategories, filterCategories(categories, "", CategoryFilter.BUILT_IN))
    }

    @Test
    fun savingACustomCategoryPreservesItsIdentityOwnerAndCreationTime() {
        val result = custom.toDraft().copy(name = " Rail travel ", description = " Train journeys ")
            .toCategory(custom, "other-owner", 300)
        assertEquals(custom.id, result.id)
        assertEquals(custom.userId, result.userId)
        assertEquals(custom.createdAt, result.createdAt)
        assertEquals(300L, result.updatedAt)
        assertEquals("Rail travel", result.name)
        assertEquals("Train journeys", result.description)
    }

    @Test
    fun customisingABuiltInCreatesAPersonalRecord() {
        val work = builtInCategories.first()
        val result = work.toDraft().copy(icon = "🎯").toCategory(work, "owner", 300)
        assertEquals("", result.id)
        assertEquals("owner", result.userId)
        assertFalse(result.isPredefined)
        assertEquals(300L, result.createdAt)
        assertEquals("Work", result.name)
    }

    @Test
    fun emojiSequencesArePreservedAndBlankIconsUseAFallback() {
        val family = "👨‍👩‍👧‍👦"
        assertEquals(family, CategoryDraft(name = "Family", icon = family).toCategory(null, "owner", 300).icon)
        assertEquals(DEFAULT_CATEGORY_ICON, CategoryDraft(name = "Family", icon = " ").toCategory(null, "owner", 300).icon)
    }

    @Test
    fun unchangedEditorsDoNotEnableSavingUntilAFieldChanges() {
        val editor = CategoryEditorState(original = custom)
        assertFalse(editor.hasChanges)
        assertTrue(editor.copy(draft = editor.draft.copy(icon = "🚆")).hasChanges)
        assertTrue(CategoryEditorState().hasChanges)
    }
}
