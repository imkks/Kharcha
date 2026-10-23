package com.kharcha.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun formatsSimpleAmounts() {
        assertEquals("₹450.00", MoneyFormatter.formatMinorUnits(45000L))
        assertEquals("₹0.00", MoneyFormatter.formatMinorUnits(0L))
        assertEquals("₹12.50", MoneyFormatter.formatMinorUnits(1250L))
    }

    @Test
    fun formatsIndianGrouping() {
        assertEquals("₹1,299.00", MoneyFormatter.formatMinorUnits(129900L))
        assertEquals("₹85,000.00", MoneyFormatter.formatMinorUnits(8500000L))
        assertEquals("₹1,23,456.50", MoneyFormatter.formatMinorUnits(12345650L))
    }

    @Test
    fun formatsSigns() {
        assertEquals("+₹85,000.00", MoneyFormatter.formatMinorUnits(8500000L, showSign = true, isCredit = true))
        assertEquals("-₹450.00", MoneyFormatter.formatMinorUnits(45000L, showSign = true, isCredit = false))
    }
}
