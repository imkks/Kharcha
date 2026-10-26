package com.kharcha.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class SpendRingTest {
    @Test
    fun testProgressFractionCalculation() {
        val spentMinor = 15_000_00L
        val budgetMinor = 30_000_00L
        val fraction = (spentMinor.toFloat() / budgetMinor.toFloat()).coerceIn(0f, 1f)
        assertEquals(0.5f, fraction, 0.001f)
    }

    @Test
    fun testProgressFractionCappedAtOne() {
        val spentMinor = 45_000_00L
        val budgetMinor = 30_000_00L
        val fraction = (spentMinor.toFloat() / budgetMinor.toFloat()).coerceIn(0f, 1f)
        assertEquals(1.0f, fraction, 0.001f)
    }

    @Test
    fun testZeroBudgetFraction() {
        val spentMinor = 10_000_00L
        val budgetMinor = 0L
        val fraction = if (budgetMinor > 0L) (spentMinor.toFloat() / budgetMinor.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f) else 0f
        assertEquals(0f, fraction, 0.001f)
    }
}
