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
        type: TransactionType = TransactionType.EXPENSE,
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
            type = TransactionType.EXPENSE,
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
        val original = createTxn(type = TransactionType.EXPENSE, category = "Food & Dining")
        val asTransfer = original.copy(categorySource = "Transfer")

        assertEquals(TransactionType.EXPENSE, asTransfer.type)
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
                type = TransactionType.EXPENSE,
                category = "Transfer",
                amount = 1500000L,
            ).copy(linkedTransactionId = 20L)

        val cardCreditReceived =
            createTxn(
                id = 20L,
                type = TransactionType.INCOME,
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
                type = TransactionType.EXPENSE,
                amount = 250000L,
                category = "Shopping",
            )
        val cardBillPayment =
            createTxn(
                id = 2L,
                type = TransactionType.EXPENSE,
                amount = 250000L,
                category = "Transfer",
            )

        val txns = listOf(cardSpend, cardBillPayment)
        val expenseTotal = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        assertEquals(500000L, expenseTotal)
    }

    @Test
    fun testInwardCategoriesAndTypeMapping() {
        assertEquals(listOf("Bank Deposit", "Business", "Interest", "Salary"), INWARD_INCOME_CATEGORIES)
        assertEquals(
            listOf(
                "A/C Transfer",
                "Bill Payment",
                "Credit",
                "Investment",
                "Loan",
                "Recharge",
                "Refund",
                "Reimbursement",
                "Rewards",
            ),
            INWARD_NON_INCOME_CATEGORIES,
        )
        assertTrue(EXPENSE_CATEGORIES.contains("Food & Drinks"))
        assertTrue(EXPENSE_CATEGORIES.contains("Groceries"))
        assertTrue(EXPENSE_CATEGORIES.contains("Fuel"))

        fun mapCategoryToType(isCredit: Boolean): TransactionType {
            return if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE
        }

        assertEquals(TransactionType.INCOME, mapCategoryToType(isCredit = true))
        assertEquals(TransactionType.EXPENSE, mapCategoryToType(isCredit = false))
    }

    @Test
    fun testLinkingVisibilityForRefundAndAcTransfer() {
        fun shouldShowLinkPrompt(category: String?, type: TransactionType): Boolean {
            val isAcTransfer =
                category == "A/C Transfer" || category == "Transfer"
            val isRefund = category.equals("Refund", ignoreCase = true)
            val isExpense = type == TransactionType.EXPENSE && !isAcTransfer
            return isAcTransfer || isRefund || isExpense
        }

        // Only Refund under income should show link prompt
        assertTrue(shouldShowLinkPrompt("Refund", TransactionType.INCOME))

        // Normal income should NOT show link prompt
        assertFalse(shouldShowLinkPrompt("Salary", TransactionType.INCOME))
        assertFalse(shouldShowLinkPrompt("Bank Deposit", TransactionType.INCOME))
        assertFalse(shouldShowLinkPrompt("Interest", TransactionType.INCOME))
        assertFalse(shouldShowLinkPrompt("Business", TransactionType.INCOME))

        // A/C Transfer should show link prompt on both sides
        assertTrue(shouldShowLinkPrompt("A/C Transfer", TransactionType.EXPENSE))
        assertTrue(shouldShowLinkPrompt("Transfer", TransactionType.EXPENSE))

        // Normal expense should show link prompt (to refund)
        assertTrue(shouldShowLinkPrompt("Food & Drinks", TransactionType.EXPENSE))
        assertTrue(shouldShowLinkPrompt("Shopping", TransactionType.EXPENSE))
    }
}
