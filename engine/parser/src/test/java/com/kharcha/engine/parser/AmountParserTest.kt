package com.kharcha.engine.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {
    @Test
    fun parseWholeAmounts() {
        assertEquals(50000L, AmountParser.parseMinorUnits("500"))
        assertEquals(123400L, AmountParser.parseMinorUnits("1,234"))
        assertEquals(8500000L, AmountParser.parseMinorUnits("85,000"))
    }

    @Test
    fun parseDecimalAmounts() {
        assertEquals(45000L, AmountParser.parseMinorUnits("450.00"))
        assertEquals(45050L, AmountParser.parseMinorUnits("450.50"))
        assertEquals(45050L, AmountParser.parseMinorUnits("450.5"))
        // Floating point accuracy test (4.29 must not become 428)
        assertEquals(429L, AmountParser.parseMinorUnits("4.29"))
        assertEquals(129900L, AmountParser.parseMinorUnits("1,299.00"))
    }

    @Test
    fun rejectInvalidAmounts() {
        assertNull(AmountParser.parseMinorUnits(null))
        assertNull(AmountParser.parseMinorUnits(""))
        assertNull(AmountParser.parseMinorUnits("   "))
        assertNull(AmountParser.parseMinorUnits("0"))
        assertNull(AmountParser.parseMinorUnits("0.00"))
        assertNull(AmountParser.parseMinorUnits("-100"))
        assertNull(AmountParser.parseMinorUnits("1.2L"))
        assertNull(AmountParser.parseMinorUnits("5k"))
        assertNull(AmountParser.parseMinorUnits("abc"))
    }
}
