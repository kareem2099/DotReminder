package com.FreeRave.remindme.screens.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryColorTest {
    @Test
    fun sixDigitColorsBecomeOpaqueArgbColors() {
        assertEquals("#FF42A5F5", normalizeCategoryColor(" #42a5f5 "))
        assertEquals("#FF42A5F5", normalizeCategoryColor("42a5f5"))
        assertEquals(0xFF42A5F5.toInt(), categoryColorArgb("#42a5f5"))
    }

    @Test
    fun eightDigitColorsPreserveTheirAlphaChannel() {
        assertEquals("#8042A5F5", normalizeCategoryColor("#8042a5f5"))
        assertEquals(0x8042A5F5.toInt(), categoryColorArgb("#8042a5f5"))
        assertEquals(0, categoryColorArgb("#00000000"))
    }

    @Test
    fun invalidColorsAreRejectedInsteadOfCrashingThePreview() {
        for (value in listOf("", "#GG1122", "#12345", "##123456", "not a color", "#123456789")) {
            assertNull(normalizeCategoryColor(value))
            assertNull(categoryColorArgb(value))
        }
        assertEquals(setOf(CategoryFieldError.COLOR),
            CategoryDraft(name = "Travel", colorHex = "not a color").validate(builtInCategories, null))
    }
}
