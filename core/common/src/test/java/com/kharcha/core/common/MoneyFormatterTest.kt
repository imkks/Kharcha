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

    @Test
    fun parsesMinorUnitsCorrectly() {
        assertEquals(45000L, MoneyFormatter.parseMinorUnits("450"))
        assertEquals(45050L, MoneyFormatter.parseMinorUnits("450.5"))
        assertEquals(45099L, MoneyFormatter.parseMinorUnits("450.99"))
        assertEquals(45099L, MoneyFormatter.parseMinorUnits("450.999"))
        assertEquals(0L, MoneyFormatter.parseMinorUnits("0"))
        assertEquals(null, MoneyFormatter.parseMinorUnits(""))
        assertEquals(null, MoneyFormatter.parseMinorUnits("   "))
    }

    @Test
    fun formatsCompactAmountsCorrectly() {
        assertEquals("0", MoneyFormatter.formatCompactAmount(0L))
        assertEquals("925", MoneyFormatter.formatCompactAmount(92500L))
        assertEquals("0.7K", MoneyFormatter.formatCompactAmount(70000L))
        assertEquals("1K", MoneyFormatter.formatCompactAmount(100000L))
        assertEquals("4.7K", MoneyFormatter.formatCompactAmount(470000L))
        assertEquals("6.8K", MoneyFormatter.formatCompactAmount(680000L))
        assertEquals("27.4K", MoneyFormatter.formatCompactAmount(2740000L))
        assertEquals("1L", MoneyFormatter.formatCompactAmount(10000000L))
        assertEquals("1.13L", MoneyFormatter.formatCompactAmount(11300000L))
        assertEquals("84.8K", MoneyFormatter.formatCompactAmount(8480000L))
    }
}
