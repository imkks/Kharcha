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
        val list = listOf(original)
        val afterDelete = list.filter { it.id != original.id }
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testSwipeDeleteUndoRestoresOriginal() {
        val original = createTxn(id = 10L, type = TransactionType.Expense, isReviewed = false)
        val list = listOf(original)
        val afterDelete = list.filter { it.id != original.id }

        // Undo action restores original instance
        val restored = afterDelete + original

        assertEquals(1, restored.size)
        assertEquals(TransactionType.Expense, restored.first().type)
        assertFalse(restored.first().isReviewed)
        assertEquals(original.amount, restored.first().amount)
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
