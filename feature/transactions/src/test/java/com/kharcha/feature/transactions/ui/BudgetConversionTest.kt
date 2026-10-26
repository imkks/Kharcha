package com.kharcha.feature.transactions.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetConversionTest {
    @Test
    fun testParseInputToMinor_plainRupees() {
        assertEquals(5_000_00L, BudgetConversionHelper.parseInputToMinor("5000"))
        assertEquals(10_000_00L, BudgetConversionHelper.parseInputToMinor("10000"))
        assertEquals(1_00L, BudgetConversionHelper.parseInputToMinor("1"))
    }

    @Test
    fun testParseInputToMinor_formattedRupeesWithCommas() {
        assertEquals(15_000_00L, BudgetConversionHelper.parseInputToMinor("15,000"))
        assertEquals(1_50_000_00L, BudgetConversionHelper.parseInputToMinor("1,50,000"))
        assertEquals(20_000_00L, BudgetConversionHelper.parseInputToMinor("₹20,000"))
    }

    @Test
    fun testParseInputToMinor_withPaise() {
        assertEquals(1250_50L, BudgetConversionHelper.parseInputToMinor("1250.50"))
        assertEquals(99_90L, BudgetConversionHelper.parseInputToMinor("99.9"))
    }

    @Test
    fun testParseInputToMinor_emptyOrInvalid() {
        assertEquals(0L, BudgetConversionHelper.parseInputToMinor(""))
        assertEquals(0L, BudgetConversionHelper.parseInputToMinor("   "))
        assertEquals(0L, BudgetConversionHelper.parseInputToMinor("invalid"))
    }

    @Test
    fun testFormatMinorToInput_validAmounts() {
        assertEquals("5000", BudgetConversionHelper.formatMinorToInput(5_000_00L))
        assertEquals("20000", BudgetConversionHelper.formatMinorToInput(20_000_00L))
        assertEquals("35000", BudgetConversionHelper.formatMinorToInput(35_000_00L))
    }

    @Test
    fun testFormatMinorToInput_zeroAndNegative() {
        assertEquals("", BudgetConversionHelper.formatMinorToInput(0L))
        assertEquals("", BudgetConversionHelper.formatMinorToInput(-5_000_00L))
    }

    @Test
    fun testPresetBudgets_areStrictlyMinorUnits() {
        val expected = listOf(5_000_00L, 10_000_00L, 20_000_00L, 30_000_00L)
        assertEquals(expected, BudgetConversionHelper.PRESET_BUDGETS_MINOR)
    }
}
