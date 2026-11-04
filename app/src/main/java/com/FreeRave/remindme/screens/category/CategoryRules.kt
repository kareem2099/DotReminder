package com.FreeRave.remindme.screens.category

import com.FreeRave.remindme.model.Category
import java.util.Locale

internal const val DEFAULT_CATEGORY_COLOR = "#FF42A5F5"
internal const val DEFAULT_CATEGORY_ICON = "📁"

internal val builtInCategories = listOf(
    Category(name = "Work", colorHex = "#FF42A5F5", icon = "💼", isPredefined = true, createdAt = 0, updatedAt = 0),
    Category(name = "Personal", colorHex = "#FF66BB6A", icon = "😊", isPredefined = true, createdAt = 0, updatedAt = 0),
    Category(name = "Health", colorHex = "#FFFF7043", icon = "🏥", isPredefined = true, createdAt = 0, updatedAt = 0),
    Category(name = "Shopping", colorHex = "#FFAB47BC", icon = "🛒", isPredefined = true, createdAt = 0, updatedAt = 0)
)

internal fun categoryNameKey(name: String): String = name.trim().lowercase(Locale.ROOT)

internal fun mergeCategories(custom: List<Category>): List<Category> {
    val names = custom.map { categoryNameKey(it.name) }.toSet()
    return custom.sortedWith(compareByDescending<Category> { it.updatedAt }.thenBy { categoryNameKey(it.name) }.thenBy { it.id }) +
        builtInCategories.filter { categoryNameKey(it.name) !in names }
}

internal enum class CategoryFilter { ALL, CUSTOM, BUILT_IN }

internal fun filterCategories(categories: List<Category>, query: String, filter: CategoryFilter): List<Category> {
    val search = query.trim()
    return categories.filter { category ->
        val matchesType = when (filter) {
            CategoryFilter.ALL -> true
            CategoryFilter.CUSTOM -> !category.isPredefined
            CategoryFilter.BUILT_IN -> category.isPredefined
        }
        matchesType && (search.isEmpty() || category.name.contains(search, ignoreCase = true) ||
            category.description.contains(search, ignoreCase = true))
    }
}

internal data class CategoryDraft(
    val name: String = "",
    val description: String = "",
    val colorHex: String = DEFAULT_CATEGORY_COLOR,
    val icon: String = DEFAULT_CATEGORY_ICON
)

internal fun Category.toDraft() = CategoryDraft(name, description, colorHex, icon)

internal enum class CategoryFieldError { NAME_REQUIRED, NAME_DUPLICATE, COLOR }

internal fun normalizeCategoryColor(value: String): String? {
    val hex = value.trim().removePrefix("#")
    if (!hex.matches(Regex("[a-fA-F0-9]{6}|[a-fA-F0-9]{8}"))) return null
    return "#" + (if (hex.length == 6) "FF$hex" else hex).uppercase(Locale.ROOT)
}

internal fun categoryColorArgb(value: String): Int? = normalizeCategoryColor(value)?.drop(1)?.toLongOrNull(16)?.toInt()

internal fun CategoryDraft.validate(categories: List<Category>, original: Category?): Set<CategoryFieldError> = buildSet {
    if (name.isBlank()) add(CategoryFieldError.NAME_REQUIRED)
    else if (categories.any { category ->
        val sameRecord = original != null && if (original.isPredefined) {
            category.isPredefined && categoryNameKey(category.name) == categoryNameKey(original.name)
        } else !category.isPredefined && category.id == original.id
        !sameRecord && categoryNameKey(category.name) == categoryNameKey(name)
    }) add(CategoryFieldError.NAME_DUPLICATE)
    if (normalizeCategoryColor(colorHex) == null) add(CategoryFieldError.COLOR)
}

internal fun CategoryDraft.toCategory(original: Category?, userId: String, now: Long): Category {
    val values = original?.takeUnless { it.isPredefined } ?: Category(userId = userId, createdAt = now)
    return values.copy(
        name = name.trim(), description = description.trim(), colorHex = requireNotNull(normalizeCategoryColor(colorHex)),
        icon = icon.trim().ifEmpty { DEFAULT_CATEGORY_ICON }, isPredefined = false, updatedAt = now
    )
}
