package com.kharcha.feature.home.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class HomeScreenCalculationTest {
    private val zoneId = ZoneId.of("Asia/Kolkata")
    private val testYearMonth = YearMonth.of(2026, 10)

    @Test
    fun testMonthlySpendAndIncomeAggregation() {
        val octEpoch1 = Instant.parse("2026-10-15T10:00:00Z").toEpochMilli()
        val octEpoch2 = Instant.parse("2026-10-18T15:30:00Z").toEpochMilli()
        val octEpochIncome = Instant.parse("2026-10-01T09:00:00Z").toEpochMilli()
        val sepEpoch = Instant.parse("2026-09-20T12:00:00Z").toEpochMilli()

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = 450_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = octEpoch1,
                merchantRaw = "Swiggy",
            ),
            Transaction(
                id = 2L,
                amount = 1_200_00L,
                type = TransactionType.Expense,
                channel = Channel.CARD,
                dateTime = octEpoch2,
                merchantRaw = "BESCOM",
            ),
            Transaction(
                id = 3L,
                amount = 85_000_00L,
                type = TransactionType.Income,
                channel = Channel.NEFT,
                dateTime = octEpochIncome,
                merchantRaw = "Acme Corp",
            ),
            Transaction(
                id = 4L,
                amount = 999_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = sepEpoch,
                merchantRaw = "Amazon",
            ),
        )

        val octTransactions = transactions.filter {
            YearMonth.from(Instant.ofEpochMilli(it.dateTime).atZone(zoneId)) == testYearMonth
        }

        val totalSpent = octTransactions
            .filter { it.type == TransactionType.Expense }
            .sumOf { it.amount }

        val totalIncome = octTransactions
            .filter { it.type == TransactionType.Income }
            .sumOf { it.amount }

        assertEquals(1_650_00L, totalSpent)
        assertEquals(85_000_00L, totalIncome)
    }

    @Test
    fun testRemainingBudgetCalculation() {
        val totalSpent = 15_000_00L
        val budget = 30_000_00L
        val remaining = (budget - totalSpent).coerceAtLeast(0L)
        assertEquals(15_000_00L, remaining)
    }

    @Test
    fun testBudgetExceededRemainingClampedToZero() {
        val totalSpent = 35_000_00L
        val budget = 30_000_00L
        val remaining = (budget - totalSpent).coerceAtLeast(0L)
        assertEquals(0L, remaining)
    }
}
