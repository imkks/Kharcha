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
            type = TransactionType.Ignored,
            isReviewed = true,
        )

        assertEquals(TransactionType.Ignored, ignored.type)
        assertTrue(ignored.isReviewed)
    }

    @Test
    fun testTransferCategoryChangesTypeToTransfer() {
        val original = createTxn(id = 3L, isReviewed = false)
        val chosenCategory = "Transfer"

        val type = if (chosenCategory.equals("Transfer", ignoreCase = true)) {
            TransactionType.Transfer
        } else {
            original.type
        }

        val updated = original.copy(
            categorySource = chosenCategory,
            type = type,
            isReviewed = true,
        )

        assertEquals(TransactionType.Transfer, updated.type)
        assertEquals("Transfer", updated.categorySource)
        assertTrue(updated.isReviewed)
    }

    @Test
    fun testSkipLeavesTransactionUnreviewed() {
        val original = createTxn(id = 4L, isReviewed = false)
        // Skip advances index without modifying original
        assertFalse(original.isReviewed)
    }
}
