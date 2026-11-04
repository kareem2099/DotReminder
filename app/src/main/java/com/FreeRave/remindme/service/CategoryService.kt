package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.Category
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log

class CategoryService @Inject constructor(private val db: FirebaseFirestore) {
    private val categoriesCollection = db.collection("categories")

    suspend fun addCategory(category: Category): Category {
        val docRef = categoriesCollection.add(category.toMap()).await()
        val savedCategory = category.copy(id = docRef.id)
        docRef.update("id", docRef.id).await()
        return savedCategory
    }

    suspend fun getCategory(id: String): Category? {
        return try {
            val doc = categoriesCollection.document(id).get().await()
            if (doc.exists()) {
                Category.fromMap(doc.data ?: emptyMap(), doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CategoryService", "Error getting category $id", e)
            null
        }
    }

    suspend fun updateCategory(category: Category): Boolean {
        return try {
            categoriesCollection.document(category.id)
                .update(mapOf(
                    "name" to category.name,
                    "colorHex" to category.colorHex,
                    "description" to category.description,
                    "icon" to category.icon,
                    "updatedAt" to category.updatedAt
                )).await()
            true
        } catch (e: Exception) {
            Log.e("CategoryService", "Error updating category $category", e)
            false
        }
    }

    suspend fun deleteCategory(categoryId: String): Boolean {
        return try {
            categoriesCollection.document(categoryId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("CategoryService", "Error deleting category $categoryId", e)
            false
        }
    }

    fun getCategories(userId: String): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val categories = snapshot.documents.map { doc ->
                        Category.fromMap(doc.data ?: emptyMap(), doc.id)
                    }
                    trySend(categories)
                }
            }
        awaitClose { listener.remove() }
    }
}
