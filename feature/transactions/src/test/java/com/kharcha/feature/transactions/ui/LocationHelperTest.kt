package com.kharcha.feature.transactions.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationHelperTest {
    @Test
    fun extractLocation_withStoredCoordinates_returnsTransactionLocation() {
        val loc =
            LocationHelper.extractLocation(
                latitude = 12.9716,
                longitude = 77.5946,
                locationLabel = "Indiranagar, Bengaluru",
                merchantName = "Swiggy",
            )

        assertNotNull(loc)
        assertEquals(12.9716, loc!!.latitude!!, 0.0001)
        assertEquals(77.5946, loc.longitude!!, 0.0001)
        assertTrue(loc.hasCoordinates)
        assertEquals("Indiranagar, Bengaluru", loc.displayLabel)
    }

    @Test
    fun extractLocation_withStoredLocationLabelOnly_returnsLocationWithoutCoordinates() {
        val loc =
            LocationHelper.extractLocation(
                latitude = null,
                longitude = null,
                locationLabel = "Hyderabad Airport",
                merchantName = "Cafe Coffee Day",
            )

        assertNotNull(loc)
        assertNull(loc!!.latitude)
        assertNull(loc.longitude)
        assertEquals("Hyderabad Airport", loc.displayLabel)
    }

    @Test
    fun extractLocation_whenNoLocationPresent_returnsNullForHistoricalScan() {
        val loc =
            LocationHelper.extractLocation(
                latitude = null,
                longitude = null,
                locationLabel = null,
                merchantName = "Starbucks",
            )

        // For historical scan transactions without geo info, location should be null so row is omitted
        assertNull(loc)
    }

    @Test
    fun extractLocation_returnsNullWhenNoData() {
        val loc =
            LocationHelper.extractLocation(
                latitude = null,
                longitude = null,
                locationLabel = null,
                merchantName = null,
            )
        assertNull(loc)
    }
}
