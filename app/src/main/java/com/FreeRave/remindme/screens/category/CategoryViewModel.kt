package com.FreeRave.remindme.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FreeRave.remindme.service.CategoryService
import com.FreeRave.remindme.service.FirebaseAuthServiceInterface
import com.FreeRave.remindme.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color as ComposeColor
import android.graphics.Color as AndroidColor

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryService: CategoryService,
    private val authService: FirebaseAuthServiceInterface
) : ViewModel() {

    private val predefinedCategories = listOf(
        Category(name = "Work", colorHex = "#FF42A5F5", icon = "💼", isPredefined = true),
        Category(name = "Personal", colorHex = "#FF66BB6A", icon = "😊", isPredefined = true),
        Category(name = "Health", colorHex = "#FFFF7043", icon = "🏥", isPredefined = true),
        Category(name = "Shopping", colorHex = "#FFAB47BC", icon = "🛒", isPredefined = true)
    )

    private val _userCategories = MutableStateFlow<List<Category>>(emptyList())

    private val _allCategories = MutableStateFlow<List<Category>>(predefinedCategories)
    val allCategories: StateFlow<List<Category>> = _allCategories

    init {
        viewModelScope.launch {
            val userId = authService.getCurrentUserId()
            if (userId.isNotBlank()) {
                try {
                    categoryService.getCategories(userId).collect { categories ->
                        val customMap = categories.groupBy { it.name.lowercase() }
                        val result = mutableListOf<Category>()

                        // First add custom categories (which may override predefined names)
                        result.addAll(categories.sortedByDescending { it.updatedAt })

                        // Then add predefined categories that aren't overridden by custom ones
                        predefinedCategories.forEach { predefinedCat ->
                            if (!customMap.containsKey(predefinedCat.name.lowercase())) {
                                result.add(predefinedCat)
                            }
                        }

                        _allCategories.value = result.sortedByDescending { it.updatedAt }
                    }
                } catch (e: Exception) {
                    // Permission errors or authentication issues, just show predefined categories
                    _allCategories.value = predefinedCategories
                }
            } else {
                // No authenticated user, show only predefined categories
                _allCategories.value = predefinedCategories
            }
        }
    }

    val categoryNames: List<String> get() = allCategories.value.map { it.name }
    val categoryColors: Map<String, ComposeColor> get() = allCategories.value.associate { cat ->
        cat.name to try {
            ComposeColor(AndroidColor.parseColor(cat.colorHex))
        } catch (e: Exception) {
            ComposeColor.Gray
        }
    }

    suspend fun addCategory(name: String, colorHex: String, description: String = "", icon: String = "📁"): Boolean {
        val userId = authService.getCurrentUserId()
        if (userId.isNotBlank()) {
            val category = Category(
                userId = userId,
                name = name,
                colorHex = colorHex,
                description = description,
                icon = icon,
                updatedAt = System.currentTimeMillis()
            )
            categoryService.addCategory(category)
            return true
        }
        return false
    }

    suspend fun updateCategory(category: Category): Boolean {
        return categoryService.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category): Boolean {
        return categoryService.deleteCategory(category.id)
    }

    suspend fun validateCategoryName(name: String, excludeCategoryId: String? = null): Boolean {
        val userId = authService.getCurrentUserId()
        if (userId.isBlank() || name.isBlank()) return false
        
        val allCategories = predefinedCategories + _userCategories.value
        return allCategories.none { 
            it.name.equals(name, ignoreCase = true) && (excludeCategoryId == null || it.id != excludeCategoryId)
        }
    }
}
