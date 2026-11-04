package com.FreeRave.remindme.service

import com.FreeRave.remindme.data.UserPreferencesRepository
import com.FreeRave.remindme.model.Session
import com.FreeRave.remindme.security.authNetworkCall
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Metadata can finish after the login screen is removed, without delaying authenticated navigation. */
@Singleton
class AuthSessionRecorder @Inject constructor(
    private val auth: FirebaseAuth,
    private val preferences: UserPreferencesRepository,
    private val sessions: SessionRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writes = Mutex()

    fun record(uid: String, rememberedEmail: String?) {
        scope.launch {
            writes.withLock {
                persistAuthSession(uid, { auth.currentUser?.uid },
                    saveEmail = {
                        if (rememberedEmail != null && preferences.rememberMeEnabled.first())
                            preferences.saveRememberedEmail(rememberedEmail)
                    },
                    saveSessionId = { id -> preferences.saveCurrentSessionId(id) },
                    saveRemote = { id -> sessions.saveSession(Session.createForCurrentDevice(id), uid) })
            }
        }
    }
}

internal suspend fun persistAuthSession(
    uid: String, currentUid: () -> String?, saveEmail: suspend () -> Unit,
    saveSessionId: suspend (String) -> Unit, saveRemote: suspend (String) -> Unit
) {
    if (currentUid() != uid) return
    try { authNetworkCall { saveEmail() } }
    catch (error: CancellationException) { throw error }
    catch (_: Exception) { /* Remembering an email is optional. */ }
    if (currentUid() != uid) return
    try {
        authNetworkCall {
            val sessionId = UUID.randomUUID().toString()
            saveSessionId(sessionId)
            if (currentUid() == uid) saveRemote(sessionId)
        }
    } catch (error: CancellationException) { throw error }
    catch (_: Exception) { /* Session metadata must not invalidate a successful sign-in. */ }
}
