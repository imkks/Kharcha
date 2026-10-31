package com.kharcha

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockResumePolicyTest {
    @Test
    fun relaunchesAuthenticationWhenLockedActivityResumesAfterStopping() {
        assertTrue(
            shouldRetryAppUnlock(
                appLockEnabled = true,
                isAppUnlocked = false,
                stoppedWhileLocked = true,
            ),
        )
    }

    @Test
    fun doesNotRelaunchWhenAuthenticationAlreadySucceeded() {
        assertFalse(
            shouldRetryAppUnlock(
                appLockEnabled = true,
                isAppUnlocked = true,
                stoppedWhileLocked = true,
            ),
        )
    }

    @Test
    fun doesNotRelaunchWhenAppLockWasTurnedOff() {
        assertFalse(
            shouldRetryAppUnlock(
                appLockEnabled = false,
                isAppUnlocked = false,
                stoppedWhileLocked = true,
            ),
        )
    }
}
