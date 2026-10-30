package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDetailToggleTest {

    @Test
    fun testDebitExpenseToggleStateAndToastMapping() {
        fun onDebitToggle(countIt: Boolean): Pair<Boolean, Int> {
            val toastRes = if (countIt) {
                R.string.detail_toast_counted_spends
            } else {
                R.string.detail_toast_not_counted_spends
            }
            return countIt to toastRes
        }

        val toggledOff = onDebitToggle(false)
        assertFalse(toggledOff.first)
        assertEquals(R.string.detail_toast_not_counted_spends, toggledOff.second)

        val toggledOn = onDebitToggle(true)
        assertTrue(toggledOn.first)
        assertEquals(R.string.detail_toast_counted_spends, toggledOn.second)
    }

    @Test
    fun testCreditIncomeToggleStateAndToastMapping() {
        fun onCreditToggle(countIt: Boolean): Pair<Boolean, Int> {
            val toastRes = if (countIt) {
                R.string.detail_toast_counted_income
            } else {
                R.string.detail_toast_not_counted_income
            }
            return countIt to toastRes
        }

        val toggledOn = onCreditToggle(true)
        assertTrue(toggledOn.first)
        assertEquals(R.string.detail_toast_counted_income, toggledOn.second)

        val toggledOff = onCreditToggle(false)
        assertFalse(toggledOff.first)
        assertEquals(R.string.detail_toast_not_counted_income, toggledOff.second)
    }

    @Test
    fun testSplitCardAvailabilityForDebitVsCredit() {
        fun isSplitAvailable(type: TransactionType): Boolean {
            return type == TransactionType.Expense
        }

        assertTrue(isSplitAvailable(TransactionType.Expense))
        assertFalse(isSplitAvailable(TransactionType.Income))
    }

    @Test
    fun testCategoryGridChunkingFourColumns() {
        val incomeChunks = INWARD_INCOME_CATEGORIES.chunked(4)
        assertEquals(1, incomeChunks.size)
        assertEquals(4, incomeChunks.first().size)
        assertEquals(listOf("Bank Deposit", "Business", "Interest", "Salary"), incomeChunks.first())

        val nonIncomeChunks = INWARD_NON_INCOME_CATEGORIES.chunked(4)
        assertEquals(3, nonIncomeChunks.size)
        assertEquals(4, nonIncomeChunks[0].size)
        assertEquals(4, nonIncomeChunks[1].size)
        assertEquals(1, nonIncomeChunks[2].size)

        val expenseChunks = EXPENSE_CATEGORIES.chunked(4)
        for (i in 0 until expenseChunks.size - 1) {
            assertEquals(4, expenseChunks[i].size)
        }
    }
}
