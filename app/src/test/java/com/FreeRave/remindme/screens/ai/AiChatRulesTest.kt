package com.FreeRave.remindme.screens.ai

import com.FreeRave.remindme.model.AiReminderDraft
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class AiChatRulesTest {
    private val json = """{"title":" Call Mum ","reminderTime":2000,"category":"Personal","isRecurring":false}"""
    @Test fun acceptsFutureDraftAndPreservesSourceText() {
        val draft = parseAiReminder(json, "Call Mum tomorrow", 1000)!!
        assertEquals("Call Mum", draft.title)
        assertEquals(2000L, draft.reminderTime)
        assertEquals("Personal", draft.category)
        assertEquals("Call Mum tomorrow", draft.sourceText)
    }
    @Test fun acceptsAnOptionalJsonCodeFence() {
        assertNotNull(parseAiReminder("```json\n$json\n```", "command", 1000))
    }
    @Test fun requiresAnActivityAndAFutureTime() {
        assertNull(parseAiReminder(json.replace(" Call Mum ", " "), "command", 1000))
        assertNull(parseAiReminder(json, "command", 2000))
        assertNull(parseAiReminder(json, "command", 3000))
        assertNull(parseAiReminder(json.replace("2000", "null"), "command", 1000))
    }
    @Test fun rejectsOverflowFractionalAndQuotedTimestamps() {
        listOf("9223372036854775808", "2000.25", "\"2000\"").forEach {
            assertNull(parseAiReminder(json.replace("2000", it), "command", 1000))
        }
    }
    @Test fun doesNotInventDailyRepetitionWhenTheIntervalIsMissing() {
        val draft = parseAiReminder(json.replace("false", "true"), "command", 1000)!!
        assertTrue(draft.isRecurring)
        assertNull(draft.recurrenceIntervalMillis)
    }
    @Test fun preservesExactRecurrenceIntervalAndValidEndDate() {
        val recurring = json.replace("false", "true").dropLast(1) + ",\"recurrenceIntervalMillis\":7200000,\"recurrenceEndTime\":3000}"
        val draft = parseAiReminder(recurring, "command", 1000)!!
        assertEquals(7200000L, draft.recurrenceIntervalMillis)
        assertEquals(3000L, draft.recurrenceEndTime)
    }
    @Test fun doesNotDiscardAnInvalidRequestedEndDateSilently() {
        val recurring = json.replace("false", "true").dropLast(1) + ",\"recurrenceEndTime\":1500}"
        assertNull(parseAiReminder(recurring, "command", 1000))
    }
    @Test fun rejectsAMalformedRecurringFlag() {
        assertNull(parseAiReminder(json.replace("false", "\"true\""), "command", 1000))
    }
    @Test fun clearsRecurrenceMetadataForAOneOffDraft() {
        val draft = parseAiReminder(json.dropLast(1) + ",\"recurrenceIntervalMillis\":1000,\"recurrenceEndTime\":3000}", "command", 1000)!!
        assertNull(draft.recurrenceIntervalMillis)
        assertNull(draft.recurrenceEndTime)
    }
    @Test fun unsupportedCategoryFallsBackToOther() {
        assertEquals("Other", parseAiReminder(json.replace("Personal", "Unknown"), "command", 1000)?.category)
    }
    @Test fun malformedOrOversizedDataDoesNotProduceADraft() {
        listOf(null, "", "not JSON", "[]", "x".repeat(32_001)).forEach { assertNull(parseAiReminder(it, "command", 1000)) }
    }
    @Test fun actionRequiresAnExplicitPrefixWithContent() {
        assertEquals("Call Mum tomorrow", reminderCommand("  ACTION_PARSE_REMINDER: Call Mum tomorrow  "))
        assertNull(reminderCommand("The format is ACTION_PARSE_REMINDER: activity"))
        assertNull(reminderCommand("ACTION_PARSE_REMINDER: "))
    }
    @Test fun contextKeepsPreviousTurnsAndEscapesQuotedUserText() {
        val text = "Remember \"الكتاب\"\nTomorrow"
        val messages = listOf(ChatMessage(text = text, sender = SenderType.USER), ChatMessage(text = "When?", sender = SenderType.AI))
        val array = JsonParser.parseString(conversationForAi(messages)).asJsonArray
        assertEquals(text, array[0].asJsonObject["content"].asString)
        assertEquals("assistant", array[1].asJsonObject["role"].asString)
    }
    @Test fun contextIsBoundedToRecentWholeTurns() {
        val messages = (1..20).map { ChatMessage(text = "$it " + "a".repeat(3990), sender = SenderType.USER) }
        val content = conversationForAi(messages)
        val array = JsonParser.parseString(content).asJsonArray
        assertTrue(content.length <= 24_000)
        assertTrue(array.size() <= 12)
        assertTrue(array.last().asJsonObject["content"].asString.startsWith("20 "))
    }
    @Test fun draftInContextIsExplicitlyMarkedAsUnsaved() {
        val message = ChatMessage(sender = SenderType.AI, draft = AiReminderDraft("Call Mum", 2000))
        assertTrue(conversationForAi(listOf(message)).contains("not saved"))
    }
    @Test fun savedDraftContextDoesNotClaimTheOldSuggestedDetailsAreCurrent() {
        val message = ChatMessage(sender = SenderType.AI, draft = AiReminderDraft("Old title", 2000), savedReminderId = "saved-1")
        val context = conversationForAi(listOf(message))
        assertTrue(context.contains("reviewed and saved"))
        assertFalse(context.contains("Old title"))
    }
}
