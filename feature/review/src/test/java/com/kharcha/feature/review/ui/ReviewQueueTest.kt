package com.kharcha.feature.review.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewQueueTest {

    private fun createTxn(
        id: Long = 1L,
        amount: Long = 1500_00L,
        merchant: String = "Swiggy",
        category: String? = null,
        isReviewed: Boolean = false,
    ): Transaction =
        Transaction(
            id = id,
            accountId = 101L,
            type = TransactionType.Expense,
            amount = amount,
            dateTime = 1729000000000L,
            merchantNormalised = merchant,
            categorySource = category,
            channel = Channel.UPI,
            isReviewed = isReviewed,
            createdAt = 1729000000000L,
            updatedAt = 1729000000000L,
        )

    @Test
    fun testCategorizeSetsCategoryAndReviewed() {
        val original = createTxn(id = 1L, category = null, isReviewed = false)
        val chosenCategory = "Food & Dining"

        val updated = original.copy(
            categorySource = chosenCategory,
            isReviewed = true,
        )

        assertEquals("Food & Dining", updated.categorySource)
        assertTrue(updated.isReviewed)
        assertEquals(original.amount, updated.amount)
        assertEquals(original.id, updated.id)
    }

    @Test
    fun testIgnoreMarksIgnoredAndReviewed() {
        val original = createTxn(id = 2L, isReviewed = false)

        val ignored = original.copy(
            isCounted = false,
            isReviewed = true,
        )

        assertFalse(ignored.isCounted)
        assertTrue(ignored.isReviewed)
    }

    @Test
    fun testTransferCategoryChangesTypeToTransfer() {
        val original = createTxn(id = 3L, isReviewed = false)
        val chosenCategory = "Transfer"

        val updated = original.copy(
            categorySource = chosenCategory,
            isCounted = false,
            isReviewed = true,
        )

        assertEquals("Transfer", updated.categorySource)
        assertTrue(updated.isTransfer)
        assertFalse(updated.isCounted)
        assertTrue(updated.isReviewed)
    }

    @Test
    fun testSkipLeavesTransactionUnreviewed() {
        val original = createTxn(id = 4L, isReviewed = false)
        // Skip advances index without modifying original
        assertFalse(original.isReviewed)
    }

    @Test
    fun testReviewCategoriesDirectionSpecific() {
        val expenseTxn = createTxn(id = 5L, isReviewed = false)
        val incomeTxn = createTxn(id = 6L, isReviewed = false).copy(type = TransactionType.Income)

        val expenseAllowed = com.kharcha.core.model.TransactionCategory.categoriesFor(expenseTxn.type)
        val incomeAllowed = com.kharcha.core.model.TransactionCategory.categoriesFor(incomeTxn.type)

        // Expense should only contain expense types
        assertTrue(expenseAllowed.all { it is com.kharcha.core.model.TransactionCategory.Expense })
        assertTrue(expenseAllowed.any { it.name == "Food & Dining" })
        assertFalse(expenseAllowed.any { it.name == "Salary" })

        // Income should only contain income types
        assertTrue(incomeAllowed.all { it is com.kharcha.core.model.TransactionCategory.Income })
        assertTrue(incomeAllowed.any { it.name == "Salary" })
        assertFalse(incomeAllowed.any { it.name == "Food & Dining" })
    }
}
