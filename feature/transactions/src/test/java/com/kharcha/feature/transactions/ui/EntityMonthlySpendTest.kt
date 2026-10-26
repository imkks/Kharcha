package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMonthlySpendTest {
    private val zoneId = ZoneId.of("Asia/Kolkata")
    private val refDate = LocalDate.of(2026, 10, 25)

    @Test
    fun calculateEntityMonthlySpendReturnsCorrectMonthCount() {
        val results = calculateEntityMonthlySpend(
            transactions = emptyList(),
            referenceDate = refDate,
            monthsCount = 7,
            zoneId = zoneId,
        )
        assertEquals(7, results.size)
        assertEquals("Oct", results.last().monthLabel)
        assertEquals(YearMonth.of(2026, 10), results.last().yearMonth)
        assertEquals("Apr", results.first().monthLabel)
        assertEquals(YearMonth.of(2026, 4), results.first().yearMonth)
    }

    @Test
    fun calculateEntityMonthlySpendAggregatesExpensesCorrectly() {
        val augEpoch =
            LocalDate.of(2026, 8, 15)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        val sepEpoch =
            LocalDate.of(2026, 9, 20)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        val octEpoch =
            LocalDate.of(2026, 10, 5)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()

        val txns = listOf(
            Transaction(
                id = 1L,
                accountId = 101L,
                amount = 2740000L,
                dateTime = augEpoch,
                merchantRaw = "Myntra",
                merchantNormalised = "Myntra",
                categorySource = "Shopping",
                type = TransactionType.Expense,
                channel = Channel.UPI,
                isReviewed = true,
            ),
            Transaction(
                id = 2L,
                accountId = 101L,
                amount = 92500L,
                dateTime = sepEpoch,
                merchantRaw = "Myntra",
                merchantNormalised = "Myntra",
                categorySource = "Shopping",
                type = TransactionType.Expense,
                channel = Channel.UPI,
                isReviewed = true,
            ),
            Transaction(
                id = 3L,
                accountId = 101L,
                amount = 680000L,
                dateTime = octEpoch,
                merchantRaw = "Myntra",
                merchantNormalised = "Myntra",
                categorySource = "Shopping",
                type = TransactionType.Expense,
                channel = Channel.UPI,
                isReviewed = true,
            ),
            Transaction(
                id = 4L,
                accountId = 101L,
                amount = 500000L,
                dateTime = octEpoch,
                merchantRaw = "Myntra",
                merchantNormalised = "Myntra",
                categorySource = "Shopping",
                type = TransactionType.Income,
                channel = Channel.UPI,
                isReviewed = true,
            ),
        )

        val results = calculateEntityMonthlySpend(
            transactions = txns,
            referenceDate = refDate,
            monthsCount = 7,
            zoneId = zoneId,
        )

        val augData = results.first { it.yearMonth == YearMonth.of(2026, 8) }
        val sepData = results.first { it.yearMonth == YearMonth.of(2026, 9) }
        val octData = results.first { it.yearMonth == YearMonth.of(2026, 10) }
        val mayData = results.first { it.yearMonth == YearMonth.of(2026, 5) }

        assertEquals(2740000L, augData.spendMinor)
        assertEquals(92500L, sepData.spendMinor)
        assertEquals(680000L, octData.spendMinor)
        assertEquals(0L, mayData.spendMinor)
    }
}
