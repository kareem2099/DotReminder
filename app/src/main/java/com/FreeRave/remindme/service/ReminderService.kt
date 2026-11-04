package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.Reminder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log

class ReminderService @Inject constructor(private val db: FirebaseFirestore, private val geofenceService: GeofenceService) {
    private val remindersCollection = db.collection("reminders")

    suspend fun addReminder(reminder: Reminder): Reminder {
        val docRef = remindersCollection.add(reminder.toMap()).await()
        // Update the document with the generated ID
        val savedReminder = reminder.copy(id = docRef.id)
        docRef.update("id", docRef.id).await()
        // Register geofence if location-based
        if (savedReminder.isLocationBased) {
            geofenceService.registerGeofence(savedReminder)
        }
        return savedReminder
    }

    suspend fun getReminder(id: String): Reminder? {
        return try {
            val doc = remindersCollection.document(id).get().await()
            if (doc.exists()) {
                val reminder = doc.toObject(Reminder::class.java)
                reminder?.copy(id = doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ReminderService", "Error getting reminder $id", e)
            null
        }
    }
    
    

    suspend fun updateReminder(reminder: Reminder): Boolean {
        return try {
            remindersCollection.document(reminder.id)
                .update(
                    "customId", reminder.customId,
                    "title", reminder.title,
                    "description", reminder.description,
                    "time", reminder.time,
                    "reminderTime", reminder.reminderTime,
                    "category", reminder.category,
                    "isRecurring", reminder.isRecurring,
                    "recurrenceIntervalMillis", reminder.recurrenceIntervalMillis,
                    "recurrenceEndTime", reminder.recurrenceEndTime,
                    "isLocationBased", reminder.isLocationBased,
                    "latitude", reminder.latitude,
                    "longitude", reminder.longitude,
                    "geofenceRadius", reminder.geofenceRadius,
                    "geofenceTransitionType", reminder.geofenceTransitionType,
                    "geofenceDwellTimeMillis", reminder.geofenceDwellTimeMillis,
                    "geofencePriority", reminder.geofencePriority
                ).await()
            // Unregister old geofence and register new if location-based
            geofenceService.unregisterGeofence(reminder.id)
            if (reminder.isLocationBased) {
                geofenceService.registerGeofence(reminder)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    

    suspend fun deleteReminder(reminderId: String) {
        // Unregister geofence before deleting
        geofenceService.unregisterGeofence(reminderId)
        remindersCollection.document(reminderId).delete().await()
        // TODO: Also cancel notification for this reminder
    }

    suspend fun deleteAllRemindersForUser(userId: String): Boolean {
        return try {
            val querySnapshot = remindersCollection.whereEqualTo("userId", userId).get().await()
            val batch = db.batch()
            querySnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
                // TODO: Also cancel notification for each deleted reminder
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("ReminderService", "Error deleting all reminders for user $userId", e)
            false
        }
    }

    fun getReminders(userId: String): Flow<List<Reminder>> = callbackFlow {
        val listener = remindersCollection
            .whereEqualTo("userId", userId)
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reminders = snapshot.documents.map { doc ->
                        Reminder.fromMap(doc.data ?: emptyMap(), doc.id)
                    }
                    trySend(reminders)
                }
            }
        awaitClose { listener.remove() }
    }

    fun searchReminders(userId: String, query: String): Flow<List<Reminder>> = callbackFlow {
        val listener = remindersCollection
            .whereEqualTo("userId", userId)
            .orderBy("title")
            .startAt(query)
            .endAt("$query~")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reminders = snapshot.documents.map { doc ->
                        Reminder.fromMap(doc.data ?: emptyMap(), doc.id)
                    }
                    trySend(reminders)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getRemindersOnce(userId: String): List<Reminder> {
        return try {
            val querySnapshot = remindersCollection.whereEqualTo("userId", userId).get().await()
            querySnapshot.documents.map { doc ->
                Reminder.fromMap(doc.data ?: emptyMap(), doc.id)
            }
        } catch (e: Exception) {
            Log.e("ReminderService", "Error getting reminders once for user $userId", e)
            emptyList()
        }
    }
}
