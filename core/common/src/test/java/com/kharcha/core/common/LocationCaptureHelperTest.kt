package com.kharcha.core.common

import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LocationCaptureHelperTest {
    @Test
    fun hasLocationPermission_returnsFalseWhenDenied() {
        val context = mockk<Context>()
        every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_DENIED

        val hasPermission = LocationCaptureHelper.hasLocationPermission(context)
        assertFalse(hasPermission)
    }

    @Test
    fun getLastKnownLocation_returnsNullWhenPermissionDenied() {
        val context = mockk<Context>()
        every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_DENIED

        val location = LocationCaptureHelper.getLastKnownLocation(context)
        assertNull(location)
    }

    @Test
    fun getLocationForLiveTransaction_returnsNullWhenPermissionDenied() = runTest {
        val context = mockk<Context>()
        every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_DENIED

        assertNull(LocationCaptureHelper.getLocationForLiveTransaction(context))
    }
}
