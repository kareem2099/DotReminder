package com.FreeRave.remindme.screens.category

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.remindme.data.ThemeSetting
import com.FreeRave.remindme.model.Category
import com.FreeRave.remindme.screens.category.components.CategoryEditorSheet
import com.FreeRave.remindme.ui.theme.GlassTheme
import com.FreeRave.remindme.ui.theme.RemindMeAppTheme

@Preview(name = "Categories · Light", widthDp = 393, heightDp = 852)
@Composable
private fun CategoriesLightPreview() = CategoriesPreview(false)

@Preview(name = "Categories · Dark", widthDp = 393, heightDp = 852)
@Composable
private fun CategoriesDarkPreview() = CategoriesPreview(true)

@Preview(name = "Categories · Small / large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun CategoriesCompactPreview() = CategoriesPreview(false)

@Preview(name = "Categories · Tablet", widthDp = 840, heightDp = 1000)
@Composable
private fun CategoriesTabletPreview() = CategoriesPreview(false)

@Preview(name = "Categories · Empty custom", widthDp = 393, heightDp = 852)
@Composable
private fun CategoriesEmptyPreview() = CategoriesPreview(false, empty = true)

@Preview(name = "Categories · Load error", widthDp = 393, heightDp = 852)
@Composable
private fun CategoriesErrorPreview() = CategoriesPreview(true, failed = true)

@Preview(name = "Categories · Editor", widthDp = 393, heightDp = 852)
@Composable
private fun CategoriesEditorPreview() = CategoriesPreview(true, editor = true)

@Composable
private fun CategoriesPreview(dark: Boolean, empty: Boolean = false, failed: Boolean = false, editor: Boolean = false) {
    val custom = if (empty) emptyList() else listOf(
        Category(id = "travel", userId = "preview", name = "Travel", description = "Flights, tickets and places to explore",
            colorHex = "#FFAB47BC", icon = "✈️", createdAt = 0, updatedAt = 2),
        Category(id = "learning", userId = "preview", name = "Learning", colorHex = "#FF61C6D5", icon = "📚", createdAt = 0, updatedAt = 1)
    )
    val state = CategoriesUiState(categories = mergeCategories(custom), isLoading = false, hasError = failed,
        filter = if (empty) CategoryFilter.CUSTOM else CategoryFilter.ALL)
    val snackbar = remember { SnackbarHostState() }
    RemindMeAppTheme(if (dark) ThemeSetting.DARK else ThemeSetting.LIGHT, useDarkTheme = dark) {
        GlassTheme {
            CategoriesScreenContent(state, snackbar, onBack = {}, onCreate = {}, onEdit = {}, onDelete = {},
                onQueryChange = {}, onFilterChange = {}, onReset = {}, onRetry = {})
            if (editor) CategoryEditorSheet(CategoryEditorState(draft = CategoryDraft(name = "Learning", icon = "📚")),
                canSave = true, onChange = {}, onSave = {}, onDismiss = {})
        }
    }
}
