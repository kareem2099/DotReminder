package com.FreeRave.remindme.security.lock

import com.FreeRave.remindme.data.BiometricLockPreference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** A failed preference read must not stop observing sign-out or the next account's policy. */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun observeAppLockPolicy(
    userIds: Flow<String>, preferences: Flow<BiometricLockPreference>, controller: AppLockController
) {
    userIds.distinctUntilChanged().flatMapLatest { id ->
        val uid = id.takeIf { it.isNotBlank() }
        if (uid == null) flow { controller.policy(null, false); emit(Unit) }
        else preferences.onStart { controller.checkingPolicy(uid) }
            .onEach { controller.policy(uid, it.protects(uid)) }.map { Unit }
            .catch { controller.preferenceFailure() }
    }.catch { controller.preferenceFailure() }.collect()
}
