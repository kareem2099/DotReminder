package com.FreeRave.remindme.service

import com.FreeRave.remindme.model.Session
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private fun sessionsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("sessions")

    suspend fun saveSession(session: Session, expectedUid: String): Boolean {
        val userId = firebaseAuth.currentUser?.uid ?: return false
        if (userId != expectedUid) return false
        return try {
            sessionsCollection(userId)
                .document(session.sessionId)
                .set(session.toMap())
                .await()
            true
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { false }
    }

    fun getSessions(userId: String): Flow<List<Session>> = callbackFlow {
        val listener = sessionsCollection(userId)
            .orderBy("lastActive", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Session(
                            sessionId = doc.id,
                            deviceName = doc.getString("deviceName") ?: "Unknown Device",
                            deviceModel = doc.getString("deviceModel") ?: "",
                            loginTime = doc.getTimestamp("loginTime") ?: Timestamp(0, 0),
                            lastActive = doc.getTimestamp("lastActive") ?: Timestamp(0, 0)
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteSession(sessionId: String): Boolean {
        val userId = firebaseAuth.currentUser?.uid ?: return false
        return try {
            sessionsCollection(userId).document(sessionId).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteAllSessionsExcept(currentSessionId: String): Boolean {
        val userId = firebaseAuth.currentUser?.uid ?: return false
        return try {
            val snapshot = sessionsCollection(userId).get().await()
            val batch = firestore.batch()
            snapshot.documents
                .filter { it.id != currentSessionId }
                .forEach { batch.delete(it.reference) }
            batch.commit().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateLastActive(sessionId: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        try {
            sessionsCollection(userId).document(sessionId)
                .update("lastActive", Timestamp.now()).await()
        } catch (e: Exception) { /* ignore */ }
    }
}
