package com.kharcha.core.designsystem.component

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class IncomeExpenseBarChartTest {

    private val zoneId = ZoneId.of("UTC")
    private val refDate = LocalDate.of(2026, 10, 25)

    private fun createTxn(
        id: Long,
        amount: Long,
        type: TransactionType,
        dateTime: Long,
    ): Transaction =
        Transaction(
            id = id,
            accountId = 1L,
            type = type,
            amount = amount,
            dateTime = dateTime,
            createdAt = dateTime,
            updatedAt = dateTime,
        )

    @Test
    fun calculateMonthlyComparisonEmptyReturnsZeroes() {
        val data = calculateMonthlyComparisonData(
            transactions = emptyList(),
            monthsCount = 6,
            referenceDate = refDate,
            zoneId = zoneId,
        )

        assertEquals(6, data.size)
        data.forEach {
            assertEquals(0L, it.incomeMinor)
            assertEquals(0L, it.expenseMinor)
        }
        assertEquals("Oct", data.last().monthLabel)
        assertEquals(YearMonth.of(2026, 10), data.last().yearMonth)
    }

    @Test
    fun calculateMonthlyComparisonAggregatesCorrectMonths() {
        val octTxn = createTxn(
            1L,
            5000_00L,
            TransactionType.EXPENSE,
            LocalDate.of(2026, 10, 10).atStartOfDay(zoneId).toInstant().toEpochMilli(),
        )
        val sepTxn1 = createTxn(
            2L,
            10000_00L,
            TransactionType.INCOME,
            LocalDate.of(2026, 9, 5).atStartOfDay(zoneId).toInstant().toEpochMilli(),
        )
        val sepTxn2 = createTxn(
            3L,
            2000_00L,
            TransactionType.EXPENSE,
            LocalDate.of(2026, 9, 20).atStartOfDay(zoneId).toInstant().toEpochMilli(),
        )
        val oldTxn = createTxn(
            4L,
            1000_00L,
            TransactionType.EXPENSE,
            LocalDate.of(2025, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
        )

        val data = calculateMonthlyComparisonData(
            transactions = listOf(octTxn, sepTxn1, sepTxn2, oldTxn),
            monthsCount = 6,
            referenceDate = refDate,
            zoneId = zoneId,
        )

        assertEquals(6, data.size)
        // October is index 5
        assertEquals(YearMonth.of(2026, 10), data[5].yearMonth)
        assertEquals(0L, data[5].incomeMinor)
        assertEquals(5000_00L, data[5].expenseMinor)

        // September is index 4
        assertEquals(YearMonth.of(2026, 9), data[4].yearMonth)
        assertEquals(10000_00L, data[4].incomeMinor)
        assertEquals(2000_00L, data[4].expenseMinor)

        // August is index 3 (empty)
        assertEquals(YearMonth.of(2026, 8), data[3].yearMonth)
        assertEquals(0L, data[3].incomeMinor)
        assertEquals(0L, data[3].expenseMinor)
    }

    @Test
    fun calculateMonthlyComparisonZeroMonthsReturnsEmpty() {
        val data = calculateMonthlyComparisonData(
            transactions = emptyList(),
            monthsCount = 0,
            referenceDate = refDate,
            zoneId = zoneId,
        )
        assertTrue(data.isEmpty())
    }
}
