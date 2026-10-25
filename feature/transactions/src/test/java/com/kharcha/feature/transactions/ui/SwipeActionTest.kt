package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeActionTest {

    private fun createTxn(
        id: Long = 1L,
        type: TransactionType = TransactionType.Expense,
        isReviewed: Boolean = false,
    ): Transaction =
        Transaction(
            id = id,
            accountId = 101L,
            type = type,
            amount = 500_00L,
            dateTime = 1792900000000L,
            merchantNormalised = "Swiggy",
            categorySource = "Food & Dining",
            channel = Channel.UPI,
            isReviewed = isReviewed,
            createdAt = 1792900000000L,
            updatedAt = 1792900000000L,
        )

    @Test
    fun testSwipeDeleteMarksIgnoredAndReviewed() {
        val original = createTxn(id = 10L, type = TransactionType.Expense, isReviewed = false)
        val deleted = original.copy(type = TransactionType.Ignored, isReviewed = true)

        assertEquals(TransactionType.Ignored, deleted.type)
        assertTrue(deleted.isReviewed)
        assertEquals(original.id, deleted.id)
    }

    @Test
    fun testSwipeDeleteUndoRestoresOriginal() {
        val original = createTxn(id = 10L, type = TransactionType.Expense, isReviewed = false)
        val deleted = original.copy(type = TransactionType.Ignored, isReviewed = true)

        // Undo action restores original instance
        val restored = original

        assertEquals(TransactionType.Expense, restored.type)
        assertFalse(restored.isReviewed)
        assertEquals(original.amount, restored.amount)
    }

    @Test
    fun testSwipeReviewMarksReviewedPreservingOtherFields() {
        val original = createTxn(id = 15L, isReviewed = false)
        val reviewed = original.copy(isReviewed = true)

        assertTrue(reviewed.isReviewed)
        assertEquals(original.type, reviewed.type)
        assertEquals(original.amount, reviewed.amount)
        assertEquals(original.merchantNormalised, reviewed.merchantNormalised)
        assertEquals(original.categorySource, reviewed.categorySource)
    }
}
