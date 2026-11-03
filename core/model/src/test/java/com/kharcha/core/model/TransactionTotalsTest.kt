package com.kharcha.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTotalsTest {

    private fun txn(
        id: Long,
        amount: Long,
        type: TransactionType = TransactionType.Expense,
        isCounted: Boolean = true,
        linkedId: Long? = null,
        category: String? = "Shopping",
    ) = Transaction(
        id = id,
        accountId = 1L,
        type = type,
        amount = amount,
        dateTime = 1729000000000L,
        categorySource = category,
        isCounted = isCounted,
        linkedTransactionId = linkedId,
    )

    @Test
    fun uncountedTransactionCountedAmountIsZero() {
        val expense = txn(id = 1L, amount = 50000L, isCounted = false)
        val income = txn(id = 2L, amount = 20000L, type = TransactionType.Income, isCounted = false)

        assertEquals(0L, expense.countedAmount())
        assertEquals(0L, income.countedAmount())
        assertNull(expense.forTotals())
        assertNull(income.forTotals())
    }

    @Test
    fun regularTransactionsCountedAmountIsFullAmount() {
        val expense = txn(id = 1L, amount = 50000L)
        val income = txn(id = 2L, amount = 20000L, type = TransactionType.Income)

        assertEquals(50000L, expense.countedAmount())
        assertEquals(20000L, income.countedAmount())
        assertEquals(50000L, expense.forTotals()?.amount)
        assertEquals(20000L, income.forTotals()?.amount)
    }

    @Test
    fun refundDeductionReducesExpenseCountedAmount() {
        val expense = txn(id = 1L, amount = 100000L) // ₹1,000

        // Partial refund ₹300
        assertEquals(70000L, expense.countedAmount(refundDeduction = 30000L))
        assertEquals(70000L, expense.forTotals(refundDeduction = 30000L)?.amount)

        // Exact refund ₹1,000 -> 0L
        assertEquals(0L, expense.countedAmount(refundDeduction = 100000L))
        assertNull(expense.forTotals(refundDeduction = 100000L))

        // Over refund ₹1,200 -> coerced to 0L
        assertEquals(0L, expense.countedAmount(refundDeduction = 120000L))
        assertNull(expense.forTotals(refundDeduction = 120000L))
    }

    @Test
    fun buildRefundMapAndForTotalsCollection() {
        val expense1 = txn(id = 10L, amount = 150000L) // ₹1,500
        val refundIncome1 = txn(id = 11L, amount = 50000L, type = TransactionType.Income, isCounted = false, linkedId = 10L) // ₹500 refund
        val expense2 = txn(id = 20L, amount = 40000L, isCounted = false) // ₹400 uncounted
        val regularIncome = txn(id = 30L, amount = 5000000L, type = TransactionType.Income, isCounted = true) // ₹50,000 salary

        val all = listOf(expense1, refundIncome1, expense2, regularIncome)
        val refundMap = all.buildRefundMap()

        assertEquals(1, refundMap.size)
        assertEquals(50000L, refundMap[10L])

        val totals = all.forTotals()
        assertEquals(2, totals.size)

        val countedExpense = totals.find { it.id == 10L }
        assertEquals(100000L, countedExpense?.amount) // ₹1,500 - ₹500 = ₹1,000

        val countedIncome = totals.find { it.id == 30L }
        assertEquals(5000000L, countedIncome?.amount)
    }

    @Test
    fun multipleRefundsAggregateForSameExpense() {
        val expense = txn(id = 100L, amount = 100000L)
        val refund1 = txn(id = 101L, amount = 20000L, type = TransactionType.Income, isCounted = false, linkedId = 100L)
        val refund2 = txn(id = 102L, amount = 30000L, type = TransactionType.Income, isCounted = false, linkedId = 100L)

        val list = listOf(expense, refund1, refund2)
        val refundMap = list.buildRefundMap()

        assertEquals(50000L, refundMap[100L])
        val totals = list.forTotals()
        assertEquals(1, totals.size)
        assertEquals(50000L, totals.first().amount)
    }
}
