package com.kharcha.feature.transactions.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationHelperTest {
    @Test
    fun extractLocation_findsCoordinatesInSmsBody() {
        val sms = "Rs 450.00 debited from A/c XX1234 at Shell Fuel 17.4399, 78.3845 on 24-Oct-26."
        val loc = LocationHelper.extractLocation(rawSms = sms, note = null, merchantName = "Shell Fuel")

        assertNotNull(loc)
        assertEquals(17.4399, loc!!.latitude!!, 0.0001)
        assertEquals(78.3845, loc.longitude!!, 0.0001)
        assertTrue(loc.hasCoordinates)
        assertEquals("Shell Fuel", loc.displayLabel)
    }

    @Test
    fun extractLocation_findsCoordinatesInNote() {
        val note = "Dinner with team lat:12.9716, lng:77.5946"
        val loc = LocationHelper.extractLocation(rawSms = null, note = note, merchantName = "Toscano")

        assertNotNull(loc)
        assertEquals(12.9716, loc!!.latitude!!, 0.0001)
        assertEquals(77.5946, loc.longitude!!, 0.0001)
        assertTrue(loc.hasCoordinates)
    }

    @Test
    fun extractLocation_fallsBackToPlacePatternInNote() {
        val note = "Groceries shopping at Banjara Hills"
        val loc = LocationHelper.extractLocation(rawSms = null, note = note, merchantName = "Ratnadeep")

        assertNotNull(loc)
        assertNull(loc!!.latitude)
        assertNull(loc.longitude)
        assertEquals("Ratnadeep, Banjara Hills", loc.displayLabel)
    }

    @Test
    fun extractLocation_fallsBackToMerchantName() {
        val loc = LocationHelper.extractLocation(rawSms = "Debit Rs 100", note = null, merchantName = "Starbucks")

        assertNotNull(loc)
        assertNull(loc!!.latitude)
        assertEquals("Starbucks", loc.displayLabel)
    }

    @Test
    fun extractLocation_returnsNullWhenNoData() {
        val loc = LocationHelper.extractLocation(rawSms = null, note = null, merchantName = null)
        assertNull(loc)
    }
}
