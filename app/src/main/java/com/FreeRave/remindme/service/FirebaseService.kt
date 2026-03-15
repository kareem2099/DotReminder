package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.Reminder
import com.FreeRave.remindme.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val remindersCollection = db.collection("reminders")

    // User functions
    fun saveUser(user: User, callback: (Boolean) -> Unit) {
        usersCollection.document(user.id)
            .set(user.toMap())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(false)
            }
    }

    // Reminder functions
    suspend fun saveReminder(reminder: Reminder): Boolean {
        return try {
            remindersCollection.document(reminder.id).set(reminder.toMap()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getReminders(): Flow<List<Reminder>> = flow {
        val snapshot = remindersCollection.get().await()
        val reminders = snapshot.documents.mapNotNull { it.toObject(Reminder::class.java) }
        emit(reminders)
    }

    suspend fun deleteReminder(id: String): Boolean {
        return try {
            remindersCollection.document(id).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateUserField(
        userId: String,
        field: String,
        value: Any,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            usersCollection.document(userId)
                .update(field, value)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e) }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
