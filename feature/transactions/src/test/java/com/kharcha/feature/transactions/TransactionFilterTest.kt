package com.kharcha.feature.transactions

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.ui.TransactionFilterType
import com.kharcha.feature.transactions.ui.filterTransactions
import com.kharcha.feature.transactions.ui.formatDate
import com.kharcha.feature.transactions.ui.groupTransactionsByDay
import com.kharcha.feature.transactions.ui.groupTransactionsByMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class TransactionFilterTest {
    private val testTxns =
        listOf(
            Transaction(
                id = 1L,
                accountId = 101L,
                type = TransactionType.EXPENSE,
                amount = 45000L,
                dateTime = System.currentTimeMillis(),
                merchantRaw = "Swiggy",
                merchantNormalised = "Swiggy",
                channel = Channel.UPI,
                isReviewed = true,
            ),
            Transaction(
                id = 2L,
                accountId = 101L,
                type = TransactionType.INCOME,
                amount = 15000000L,
                dateTime = System.currentTimeMillis() - 86400000L,
                merchantRaw = "Salary Employer",
                merchantNormalised = "Employer Inc",
                channel = Channel.NEFT,
                isReviewed = true,
            ),
            Transaction(
                id = 3L,
                accountId = 101L,
                type = TransactionType.EXPENSE,
                amount = 12000L,
                dateTime = System.currentTimeMillis(),
                merchantRaw = "Uber India",
                merchantNormalised = "Uber",
                channel = Channel.UPI,
                isReviewed = false,
            ),
            Transaction(
                id = 4L,
                accountId = 101L,
                type = TransactionType.EXPENSE,
                amount = 500000L,
                dateTime = System.currentTimeMillis() - 172800000L,
                merchantRaw = "Self Transfer",
                merchantNormalised = "Self Transfer",
                categorySource = "Transfer",
                channel = Channel.IMPS,
                isReviewed = true,
            ),
        )

    @Test
    fun testFilterAll() {
        val result = filterTransactions(testTxns, "", TransactionFilterType.ALL)
        assertEquals(4, result.size)
    }

    @Test
    fun testFilterExpense() {
        val result = filterTransactions(testTxns, "", TransactionFilterType.EXPENSE)
        assertEquals(3, result.size)
        assertTrue(result.all { it.type == TransactionType.EXPENSE })
    }

    @Test
    fun testFilterIncome() {
        val result = filterTransactions(testTxns, "", TransactionFilterType.INCOME)
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
    }

    @Test
    fun testFilterTransfer() {
        val result = filterTransactions(testTxns, "", TransactionFilterType.TRANSFER)
        assertEquals(1, result.size)
        assertEquals(4L, result.first().id)
    }

    @Test
    fun testFilterNeedsReview() {
        val result = filterTransactions(testTxns, "", TransactionFilterType.NEEDS_REVIEW)
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    @Test
    fun testSearchQueryFilter() {
        val result = filterTransactions(testTxns, "swiggy", TransactionFilterType.ALL)
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun testGroupTransactionsByDay() {
        val grouped = groupTransactionsByDay(testTxns)
        assertTrue(grouped.isNotEmpty())
    }

    @Test
    fun testFormatDateTodayAndYesterday() {
        val now = System.currentTimeMillis()
        val todayStr = formatDate(now, "Today", "Yesterday")
        assertEquals("Today", todayStr)

        val yesterdayEpoch =
            LocalDate.now()
                .minusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val yesterdayStr = formatDate(yesterdayEpoch, "Today", "Yesterday")
        assertEquals("Yesterday", yesterdayStr)
    }

    @Test
    fun testGroupTransactionsByMonth() {
        val grouped = groupTransactionsByMonth(testTxns)
        assertTrue(grouped.isNotEmpty())
        val currentYm = YearMonth.now()
        assertTrue(grouped.containsKey(currentYm))
    }
}
