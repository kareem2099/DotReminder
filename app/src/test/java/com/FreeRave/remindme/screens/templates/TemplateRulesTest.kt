package com.FreeRave.remindme.screens.templates

import com.FreeRave.remindme.model.ReminderTemplate
import org.junit.Assert.*
import org.junit.Test

class TemplateRulesTest {
    private fun template(id: String = "one", name: String = "Morning") = ReminderTemplate(
        id = id, userId = "owner", name = name, title = "Water the plants", description = "Balcony routine", category = "Personal",
        isRecurring = true, recurrenceIntervalMillis = 1_234, isLocationBased = true, createdAt = 10, updatedAt = 20)

    @Test fun `name and default title are required`() {
        val errors = TemplateDraft(name = " ", title = "").validate()
        assertTrue(TemplateFieldError.NAME in errors)
        assertTrue(TemplateFieldError.TITLE in errors)
    }
    @Test fun `repeating templates require a valid interval`() {
        listOf("", "0", "-1", "abc").forEach {
            val draft = TemplateDraft(name = "A", title = "B", isRecurring = true, interval = TemplateInterval(it))
            assertTrue(TemplateFieldError.INTERVAL in draft.validate())
        }
    }
    @Test fun `one time templates ignore a hidden invalid interval`() {
        assertTrue(TemplateDraft(name = "A", title = "B", interval = TemplateInterval("")).validate().isEmpty())
    }
    @Test fun `decimal hours convert exactly`() {
        assertEquals(5_400_000L, TemplateInterval("1.5", TemplateIntervalUnit.HOURS).toMillis())
    }
    @Test fun `comma decimals are supported`() {
        assertEquals(150_000L, TemplateInterval("2,5", TemplateIntervalUnit.MINUTES).toMillis())
    }
    @Test fun `overflows cannot wrap into a valid duration`() {
        assertNull(TemplateInterval(Long.MAX_VALUE.toString(), TemplateIntervalUnit.DAYS).toMillis())
    }
    @Test fun `fractions smaller than a millisecond are rejected`() {
        assertNull(TemplateInterval("0.0001", TemplateIntervalUnit.SECONDS).toMillis())
        assertEquals(1L, TemplateInterval("0.001", TemplateIntervalUnit.SECONDS).toMillis())
    }
    @Test fun `ambiguous separators and non numeric input are rejected`() {
        listOf("1,000.5", "1e4", "١٢", "+2", "NaN").forEach {
            assertNull(TemplateInterval(it, TemplateIntervalUnit.SECONDS).toMillis())
        }
    }
    @Test fun `all legacy positive intervals survive an unchanged edit exactly`() {
        listOf(1L, 1_234L, 60_000L, 86_400_000L, 604_800_000L, Long.MAX_VALUE).forEach {
            assertEquals(it, templateInterval(it).toMillis())
        }
    }
    @Test fun `editing keeps identity owner creation time and location flag`() {
        val original = template()
        val updated = original.toDraft().copy(name = " New name ").toTemplate(original, "owner", 99)
        assertEquals("one", updated.id)
        assertEquals("owner", updated.userId)
        assertEquals(10L, updated.createdAt)
        assertEquals(99L, updated.updatedAt)
        assertEquals("New name", updated.name)
        assertTrue(updated.isLocationBased)
        assertEquals(original.recurrenceIntervalMillis, updated.recurrenceIntervalMillis)
    }
    @Test fun `turning repeat off removes the saved interval`() {
        val original = template()
        val saved = original.toDraft().copy(isRecurring = false).toTemplate(original, "owner", 99)
        assertFalse(saved.isRecurring)
        assertNull(saved.recurrenceIntervalMillis)
    }
    @Test fun `unchanged drafts do not enable redundant saves`() {
        val original = template()
        assertFalse(original.toDraft().hasChanges(original))
        assertTrue(original.toDraft().copy(title = "Different").hasChanges(original))
    }
    @Test fun `search trims input and matches descriptions without changing the filter`() {
        val first = template()
        val second = template("two", "Other").copy(description = "", isRecurring = false)
        val state = TemplatesUiState(templates = listOf(first, second), query = " BALCONY ", filter = TemplateFilter.RECURRING)
        assertEquals(listOf(first), state.visibleTemplates)
    }
    @Test fun `location filter includes only templates with a location flag`() {
        val first = template()
        val second = template("two").copy(isLocationBased = false)
        assertEquals(listOf(first), TemplatesUiState(templates = listOf(first, second), filter = TemplateFilter.LOCATION).visibleTemplates)
    }
    @Test fun `template ordering is stable and ignores blank duplicate identities`() {
        val items = listOf(template("b", "beta"), template("a", "Alpha"), template("a", "Alpha"), template("", "Empty"))
        assertEquals(listOf("a", "b"), orderTemplates(items).map { it.id })
    }
}
