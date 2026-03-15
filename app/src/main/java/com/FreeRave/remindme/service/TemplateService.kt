package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.ReminderTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log

class TemplateService @Inject constructor(private val db: FirebaseFirestore) {
    private val templatesCollection = db.collection("reminder_templates")

    suspend fun addTemplate(template: ReminderTemplate): ReminderTemplate {
        val docRef = templatesCollection.add(template.toMap()).await()
        val savedTemplate = template.copy(id = docRef.id)
        docRef.update("id", docRef.id).await()
        return savedTemplate
    }

    suspend fun getTemplate(id: String): ReminderTemplate? {
        return try {
            val doc = templatesCollection.document(id).get().await()
            if (doc.exists()) {
                ReminderTemplate.fromMap(doc.data ?: emptyMap(), doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("TemplateService", "Error getting template $id", e)
            null
        }
    }

    suspend fun updateTemplate(template: ReminderTemplate): Boolean {
        return try {
            templatesCollection.document(template.id)
                .update(mapOf(
                    "name" to template.name,
                    "title" to template.title,
                    "description" to template.description,
                    "category" to template.category,
                    "isRecurring" to template.isRecurring,
                    "recurrenceIntervalMillis" to template.recurrenceIntervalMillis,
                    "isLocationBased" to template.isLocationBased,
                    "updatedAt" to template.updatedAt
                )).await()
            true
        } catch (e: Exception) {
            Log.e("TemplateService", "Error updating template $template", e)
            false
        }
    }

    suspend fun deleteTemplate(templateId: String): Boolean {
        return try {
            templatesCollection.document(templateId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("TemplateService", "Error deleting template $templateId", e)
            false
        }
    }

    fun getTemplates(userId: String): Flow<List<ReminderTemplate>> = callbackFlow {
        val listener = templatesCollection
            .whereEqualTo("userId", userId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val templates = snapshot.documents.map { doc ->
                        ReminderTemplate.fromMap(doc.data ?: emptyMap(), doc.id)
                    }
                    trySend(templates)
                }
            }
        awaitClose { listener.remove() }
    }
}
