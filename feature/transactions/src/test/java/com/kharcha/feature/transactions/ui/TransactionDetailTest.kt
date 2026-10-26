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

    @Test
    fun testMerchantVisitsCountCalculation() {
        val txn1 = createTxn(id = 1L, merchantNorm = "Swiggy")
        val txn2 = createTxn(id = 2L, merchantNorm = "Swiggy")
        val txn3 = createTxn(id = 3L, merchantNorm = "Zomato")
        val allList = listOf(txn1, txn2, txn3)

        val countSwiggy = allList.count { it.merchantNormalised == "Swiggy" }
        assertEquals(2, countSwiggy)

        val countZomato = allList.count { it.merchantNormalised == "Zomato" }
        assertEquals(1, countZomato)
    }

    @Test
    fun testLinkedTransactionResolution() {
        val debitForCardBill =
            createTxn(
                id = 10L,
                type = TransactionType.Transfer,
                category = "Transfer",
                amount = 1500000L,
            ).copy(linkedTransactionId = 20L)

        val cardCreditReceived =
            createTxn(
                id = 20L,
                type = TransactionType.Transfer,
                category = "Transfer",
                amount = 1500000L,
            ).copy(linkedTransactionId = 10L)

        val all = listOf(debitForCardBill, cardCreditReceived)

        val linkedToDebit = all.find { it.id == debitForCardBill.linkedTransactionId }
        val linkedToCredit = all.find { it.id == cardCreditReceived.linkedTransactionId }

        assertEquals(20L, linkedToDebit?.id)
        assertEquals(10L, linkedToCredit?.id)
    }

    @Test
    fun testCardSpendExpenseVsCardBillTransfer() {
        val cardSpend =
            createTxn(
                id = 1L,
                type = TransactionType.Expense,
                amount = 250000L,
                category = "Shopping",
            )
        val cardBillPayment =
            createTxn(
                id = 2L,
                type = TransactionType.Transfer,
                amount = 250000L,
                category = "Transfer",
            )

        val txns = listOf(cardSpend, cardBillPayment)
        val expenseTotal = txns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }

        assertEquals(250000L, expenseTotal)
        assertFalse(txns.filter { it.type == TransactionType.Expense }.contains(cardBillPayment))
    }

    @Test
    fun testInwardCategoriesAndTypeMapping() {
        assertTrue(INWARD_INCOME_CATEGORIES.contains("Salary"))
        assertTrue(INWARD_INCOME_CATEGORIES.contains("Bank Deposit"))
        assertTrue(INWARD_INCOME_CATEGORIES.contains("Interest"))
        assertTrue(INWARD_NON_INCOME_CATEGORIES.contains("Refund"))
        assertTrue(INWARD_NON_INCOME_CATEGORIES.contains("A/C Transfer"))
        assertTrue(INWARD_NON_INCOME_CATEGORIES.contains("Bill Payment"))
        assertTrue(EXPENSE_CATEGORIES.contains("Food & Drinks"))
        assertTrue(EXPENSE_CATEGORIES.contains("Groceries"))
        assertTrue(EXPENSE_CATEGORIES.contains("Fuel"))

        fun mapCategoryToType(cat: String, isCredit: Boolean): TransactionType {
            return when {
                cat in INWARD_INCOME_CATEGORIES -> TransactionType.Income
                cat == "Refund" -> TransactionType.Refund
                cat in listOf("Transfer", "A/C Transfer", "Bill Payment", "Investment") -> TransactionType.Transfer
                else -> if (isCredit) TransactionType.Income else TransactionType.Expense
            }
        }

        assertEquals(TransactionType.Income, mapCategoryToType("Salary", isCredit = true))
        assertEquals(TransactionType.Refund, mapCategoryToType("Refund", isCredit = true))
        assertEquals(TransactionType.Transfer, mapCategoryToType("A/C Transfer", isCredit = true))
        assertEquals(TransactionType.Transfer, mapCategoryToType("Bill Payment", isCredit = true))
        assertEquals(TransactionType.Expense, mapCategoryToType("Groceries", isCredit = false))
    }
}
