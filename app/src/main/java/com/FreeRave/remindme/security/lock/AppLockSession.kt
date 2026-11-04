package com.FreeRave.remindme.security.lock

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockSession @Inject constructor() {
    internal val controller = AppLockController()
}
