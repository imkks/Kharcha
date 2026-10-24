package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDetailTest {

    private fun createTxn(
        id: Long = 1L,
        merchantRaw: String? = null,
        merchantNorm: String? = null,
        type: TransactionType = TransactionType.Expense,
        amount: Long = 500_00L,
        category: String? = "Food & Dining",
        isReviewed: Boolean = false,
    ): Transaction =
        Transaction(
            id = id,
            accountId = 101L,
            type = type,
            amount = amount,
            dateTime = 1792900000000L,
            merchantRaw = merchantRaw,
            merchantNormalised = merchantNorm,
            categorySource = category,
            channel = Channel.UPI,
            isReviewed = isReviewed,
            createdAt = 1792900000000L,
            updatedAt = 1792900000000L,
        )

    @Test
    fun testMerchantDisplayFallback() {
        val txnWithBoth = createTxn(merchantRaw = "SWIGGY_BLR", merchantNorm = "Swiggy")
        assertEquals("Swiggy", txnWithBoth.merchantNormalised ?: txnWithBoth.merchantRaw)

        val txnWithRawOnly = createTxn(merchantRaw = "UBER_TRIP_123", merchantNorm = null)
        assertEquals("UBER_TRIP_123", txnWithRawOnly.merchantNormalised ?: txnWithRawOnly.merchantRaw)
    }

    @Test
    fun testUpdateTransactionPreservesIdAndMarksReviewed() {
        val original = createTxn(category = "Other", isReviewed = false)
        val updated = original.copy(
            categorySource = "Groceries",
            type = TransactionType.Expense,
            note = "Weekly groceries",
            isReviewed = true,
        )

        assertEquals(original.id, updated.id)
        assertEquals("Groceries", updated.categorySource)
        assertEquals("Weekly groceries", updated.note)
        assertTrue(updated.isReviewed)
        assertFalse(original.isReviewed)
    }

    @Test
    fun testTransferTypeSetsTransferCategory() {
        val original = createTxn(type = TransactionType.Expense, category = "Food & Dining")
        val asTransfer = original.copy(type = TransactionType.Transfer, categorySource = "Transfer")

        assertEquals(TransactionType.Transfer, asTransfer.type)
        assertEquals("Transfer", asTransfer.categorySource)
    }
}
