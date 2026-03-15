package com.FreeRave.remindme.utils

import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.utils.CategoryUtils.getCategoryIcon
import com.FreeRave.remindme.utils.CategoryUtils.predefinedCategories
import java.util.*
import kotlin.math.max

data class SuggestionResult(
    val titleSuggestions: List<String> = emptyList(),
    val descriptionSuggestions: List<String> = emptyList(),
    val categorySuggestions: List<CategorySuggestion> = emptyList(),
    val timeSuggestions: List<TimeSuggestion> = emptyList()
)

data class CategorySuggestion(
    val categoryName: String,
    val confidence: Float,
    val icon: String
)

data class TimeSuggestion(
    val timeMillis: Long,
    val displayText: String,
    val context: String
)

data class SmartDefaults(
    val suggestedTime: Long?,
    val suggestedCategory: String?,
    val commonPhrases: List<String>
)

class SmartSuggestionEngine {

    // Common reminder patterns and their categories
    private val categoryKeywords = mapOf(
        "work" to listOf("meeting", "deadline", "project", "task", "work", "office", "email", "call", "presentation"),
        "personal" to listOf("shopping", "clean", "cook", "exercise", "gym", "doctor", "dentist", "appointment"),
        "health" to listOf("medicine", "pill", "doctor", "appointment", "exercise", "gym", "water", "sleep", "health"),
        "social" to listOf("party", "birthday", "dinner", "lunch", "meeting", "friend", "family", "event"),
        "finance" to listOf("bill", "payment", "bank", "money", "budget", "shopping", "buy", "pay"),
        "education" to listOf("study", "exam", "homework", "class", "lecture", "book", "learn", "course")
    )

    // Common time patterns
    private val timePatterns = mapOf(
        "morning" to Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }.timeInMillis,
        "afternoon" to Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }.timeInMillis,
        "evening" to Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
        }.timeInMillis,
        "night" to Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
    )

    // Common reminder titles and descriptions
    private val commonTitles = listOf(
        "Meeting with",
        "Call",
        "Pick up",
        "Buy",
        "Pay",
        "Take medicine",
        "Exercise",
        "Doctor appointment",
        "Submit",
        "Review",
        "Plan",
        "Schedule"
    )

    private val commonDescriptions = listOf(
        "Don't forget to",
        "Remember to",
        "Make sure to",
        "Important:",
        "Note:",
        "Follow up on"
    )

    fun analyzeText(text: String): SuggestionResult {
        val lowerText = text.lowercase()

        return SuggestionResult(
            titleSuggestions = generateTitleSuggestions(text),
            descriptionSuggestions = generateDescriptionSuggestions(text),
            categorySuggestions = detectCategories(lowerText),
            timeSuggestions = generateTimeSuggestions()
        )
    }

    fun suggestCategory(title: String, description: String): List<CategorySuggestion> {
        val combinedText = "$title $description".lowercase()
        return detectCategories(combinedText)
    }

    fun suggestTime(context: TimeContext): List<TimeSuggestion> {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val suggestions = mutableListOf<TimeSuggestion>()

        // Suggest based on time of day
        when {
            currentHour < 12 -> {
                // Morning - suggest afternoon/evening
                suggestions.add(TimeSuggestion(
                    timeMillis = getTimeMillis(14, 0), // 2 PM
                    displayText = "This afternoon",
                    context = "Good time for tasks after morning routine"
                ))
                suggestions.add(TimeSuggestion(
                    timeMillis = getTimeMillis(18, 0), // 6 PM
                    displayText = "This evening",
                    context = "Perfect for evening activities"
                ))
            }
            currentHour < 17 -> {
                // Afternoon - suggest evening
                suggestions.add(TimeSuggestion(
                    timeMillis = getTimeMillis(18, 0), // 6 PM
                    displayText = "This evening",
                    context = "Great for winding down"
                ))
                suggestions.add(TimeSuggestion(
                    timeMillis = getTimeMillis(21, 0), // 9 PM
                    displayText = "Tonight",
                    context = "Before bedtime"
                ))
            }
            else -> {
                // Evening/Night - suggest tomorrow
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                }
                suggestions.add(TimeSuggestion(
                    timeMillis = tomorrow.timeInMillis,
                    displayText = "Tomorrow morning",
                    context = "Fresh start tomorrow"
                ))
            }
        }

        // Add some quick options
        suggestions.add(TimeSuggestion(
            timeMillis = System.currentTimeMillis() + (30 * 60 * 1000), // 30 minutes
            displayText = "In 30 minutes",
            context = "Quick reminder"
        ))

        suggestions.add(TimeSuggestion(
            timeMillis = System.currentTimeMillis() + (60 * 60 * 1000), // 1 hour
            displayText = "In 1 hour",
            context = "Short-term reminder"
        ))

        return suggestions.take(4) // Return top 4 suggestions
    }

    fun getSmartDefaults(userId: String): SmartDefaults {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Suggest time based on current time
        val suggestedTime = when {
            currentHour < 9 -> getTimeMillis(9, 0) // Morning
            currentHour < 12 -> getTimeMillis(14, 0) // Afternoon
            currentHour < 17 -> getTimeMillis(18, 0) // Evening
            else -> {
                // Tomorrow morning
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
            }
        }

        // Most common category (could be learned from user history)
        val suggestedCategory = "personal"

        // Common phrases
        val commonPhrases = listOf(
            "Don't forget to",
            "Remember to",
            "Make sure to",
            "Important reminder:",
            "Follow up on"
        )

        return SmartDefaults(
            suggestedTime = suggestedTime,
            suggestedCategory = suggestedCategory,
            commonPhrases = commonPhrases
        )
    }

    private fun generateTitleSuggestions(input: String): List<String> {
        val suggestions = mutableListOf<String>()

        // If input is very short, suggest completions
        if (input.length < 3) {
            suggestions.addAll(commonTitles.filter { it.startsWith(input, ignoreCase = true) })
        }

        // Suggest based on common patterns
        if (input.contains("call", ignoreCase = true)) {
            suggestions.add("Call $input".replace("call", "", ignoreCase = true).trim())
        }
        if (input.contains("meeting", ignoreCase = true)) {
            suggestions.add("Meeting: $input".replace("meeting", "", ignoreCase = true).trim())
        }
        if (input.contains("buy", ignoreCase = true) || input.contains("shopping", ignoreCase = true)) {
            suggestions.add("Shopping: $input")
        }

        return suggestions.distinct().take(3)
    }

    private fun generateDescriptionSuggestions(input: String): List<String> {
        return commonDescriptions.map { "$it $input" }.take(2)
    }

    private fun detectCategories(text: String): List<CategorySuggestion> {
        val categoryScores = mutableMapOf<String, Float>()

        // Calculate confidence scores for each category
        categoryKeywords.forEach { (category, keywords) ->
            var score = 0f
            var matches = 0

            keywords.forEach { keyword ->
                val occurrences = text.countOccurrences(keyword)
                if (occurrences > 0) {
                    score += occurrences * (keyword.length.toFloat() / 10f) // Longer keywords = higher weight
                    matches++
                }
            }

            if (matches > 0) {
                // Boost score based on number of matches and text relevance
                val finalScore = score * (1 + matches * 0.2f)
                categoryScores[category] = finalScore
            }
        }

        // Convert to suggestions with icons
        return categoryScores.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (category, score) ->
                CategorySuggestion(
                    categoryName = category,
                    confidence = minOf(score / 5f, 1f), // Normalize to 0-1
                    icon = getCategoryIcon(category)
                )
            }
    }

    private fun generateTimeSuggestions(): List<TimeSuggestion> {
        val suggestions = mutableListOf<TimeSuggestion>()

        timePatterns.forEach { (label, timeMillis) ->
            suggestions.add(TimeSuggestion(
                timeMillis = timeMillis,
                displayText = "Today $label",
                context = "Standard $label time"
            ))
        }

        return suggestions.take(3)
    }

    private fun getTimeMillis(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time has passed today, move to tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis
    }

    private fun String.countOccurrences(substring: String): Int {
        var count = 0
        var index = 0
        while (true) {
            index = this.indexOf(substring, index, ignoreCase = true)
            if (index == -1) break
            count++
            index += substring.length
        }
        return count
    }
}

data class TimeContext(
    val currentTime: Long = System.currentTimeMillis(),
    val userPreferences: Map<String, Any> = emptyMap()
)
