package com.kharcha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationPermissionIssueTest {
    @Test
    fun missingForegroundLocationIsReportedFirst() {
        assertEquals(
            LocationPermissionIssue.FOREGROUND_ACCESS,
            locationPermissionIssue(sdkInt = 34, hasForegroundAccess = false, hasBackgroundAccess = false),
        )
    }

    @Test
    fun missingBackgroundLocationIsReportedOnAndroid10AndAbove() {
        assertEquals(
            LocationPermissionIssue.BACKGROUND_ACCESS,
            locationPermissionIssue(sdkInt = 34, hasForegroundAccess = true, hasBackgroundAccess = false),
        )
    }

    @Test
    fun locationAccessIsCompleteWhenRequiredPermissionsAreGranted() {
        assertNull(locationPermissionIssue(sdkInt = 34, hasForegroundAccess = true, hasBackgroundAccess = true))
    }

    @Test
    fun backgroundLocationIsNotRequiredBeforeAndroid10() {
        assertNull(locationPermissionIssue(sdkInt = 28, hasForegroundAccess = true, hasBackgroundAccess = false))
    }
}
