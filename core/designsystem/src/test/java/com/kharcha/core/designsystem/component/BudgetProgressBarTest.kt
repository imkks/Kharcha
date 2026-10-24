package com.kharcha.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetProgressBarTest {

    @Test
    fun testCalculateBudgetStatusOnTrack() {
        // 50% spent
        val status = calculateBudgetStatus(spentMinor = 5_000_00L, budgetMinor = 10_000_00L)
        assertEquals(BudgetTrafficLightStatus.ON_TRACK, status)

        // 79.9% spent
        val status79 = calculateBudgetStatus(spentMinor = 7_990_00L, budgetMinor = 10_000_00L)
        assertEquals(BudgetTrafficLightStatus.ON_TRACK, status79)
    }

    @Test
    fun testCalculateBudgetStatusWarning() {
        // Exactly 80% spent
        val status80 = calculateBudgetStatus(spentMinor = 8_000_00L, budgetMinor = 10_000_00L)
        assertEquals(BudgetTrafficLightStatus.WARNING, status80)

        // 99% spent
        val status99 = calculateBudgetStatus(spentMinor = 9_900_00L, budgetMinor = 10_000_00L)
        assertEquals(BudgetTrafficLightStatus.WARNING, status99)
    }

    @Test
    fun testCalculateBudgetStatusExceeded() {
        // Exactly 100% spent
        val status100 = calculateBudgetStatus(spentMinor = 10_000_00L, budgetMinor = 10_000_00L)
        assertEquals(BudgetTrafficLightStatus.EXCEEDED, status100)

        // Over budget
        val statusOver = calculateBudgetStatus(spentMinor = 12_500_00L, budgetMinor = 10_000_00L)
        assertEquals(BudgetTrafficLightStatus.EXCEEDED, statusOver)

        // Zero budget
        val statusZeroBudget = calculateBudgetStatus(spentMinor = 1_00L, budgetMinor = 0L)
        assertEquals(BudgetTrafficLightStatus.EXCEEDED, statusZeroBudget)
    }
}
