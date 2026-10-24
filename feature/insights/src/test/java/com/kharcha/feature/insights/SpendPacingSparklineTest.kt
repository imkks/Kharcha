package com.kharcha.feature.insights

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SpendPacingSparklineTest {

    private val zoneId = ZoneId.of("UTC")
    private val testDate = LocalDate.of(2026, 10, 15)

    private fun createTxn(
        id: Long,
        amount: Long,
        dayOfMonth: Int,
        month: Int = 10,
        type: TransactionType = TransactionType.Expense,
    ): Transaction =
        Transaction(
            id = id,
            accountId = 1L,
            type = type,
            amount = amount,
            dateTime = LocalDate.of(2026, month, dayOfMonth).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            createdAt = 1792900000000L,
            updatedAt = 1792900000000L,
        )

    @Test
    fun testEmptyTransactionsReturnsZeroes() {
        val result = calculateCumulativeSpendByDay(
            transactions = emptyList(),
            referenceDate = testDate,
            zoneId = zoneId,
        )

        assertEquals(15, result.size)
        result.forEach { assertEquals(0L, it) }
    }

    @Test
    fun testCumulativeSumAccruesCorrectly() {
        val txns = listOf(
            createTxn(1L, 100_00L, dayOfMonth = 2),
            createTxn(2L, 150_00L, dayOfMonth = 2), // Same day
            createTxn(3L, 200_00L, dayOfMonth = 5),
            createTxn(4L, 500_00L, dayOfMonth = 10, type = TransactionType.Income), // Income ignored
            createTxn(5L, 300_00L, dayOfMonth = 20, month = 9), // Last month ignored
        )

        val result = calculateCumulativeSpendByDay(
            transactions = txns,
            referenceDate = testDate,
            zoneId = zoneId,
        )

        assertEquals(15, result.size)
        assertEquals(0L, result[0]) // Day 1
        assertEquals(250_00L, result[1]) // Day 2: 100 + 150
        assertEquals(250_00L, result[2]) // Day 3: no spend
        assertEquals(250_00L, result[3]) // Day 4: no spend
        assertEquals(450_00L, result[4]) // Day 5: 250 + 200
        assertEquals(450_00L, result[14]) // Day 15: unchanged
    }
}
