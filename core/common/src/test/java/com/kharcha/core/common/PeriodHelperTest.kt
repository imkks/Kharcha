package com.kharcha.core.common

import com.kharcha.core.model.TimePeriod
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PeriodHelperTest {

    private val zoneId = ZoneId.of("UTC")
    private val refDate = LocalDate.of(2026, 10, 25)

    private fun createTxn(id: Long, dateTime: Long): Transaction =
        Transaction(
            id = id,
            accountId = 1L,
            type = TransactionType.Expense,
            amount = 100_00L,
            dateTime = dateTime,
            createdAt = dateTime,
            updatedAt = dateTime,
        )

    @Test
    fun testAllTimeReturnsNullRange() {
        val range = PeriodHelper.getPeriodDateRange(TimePeriod.ALL_TIME, refDate, zoneId)
        assertNull(range)
    }

    @Test
    fun testThisMonthDateRange() {
        val range = PeriodHelper.getPeriodDateRange(TimePeriod.THIS_MONTH, refDate, zoneId)
        assertNotNull(range)

        // 2026-10-01T00:00:00Z = 1790812800000
        val expectedStart = LocalDate.of(2026, 10, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val expectedEnd =
            LocalDate.of(2026, 10, 31).atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

        assertEquals(expectedStart, range!!.first)
        assertEquals(expectedEnd, range.second)
    }

    @Test
    fun testLastMonthDateRange() {
        val range = PeriodHelper.getPeriodDateRange(TimePeriod.LAST_MONTH, refDate, zoneId)
        assertNotNull(range)

        val expectedStart = LocalDate.of(2026, 9, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val expectedEnd =
            LocalDate.of(2026, 9, 30).atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

        assertEquals(expectedStart, range!!.first)
        assertEquals(expectedEnd, range.second)
    }

    @Test
    fun testFilterTransactionsByPeriod() {
        val midOct = LocalDate.of(2026, 10, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val midSep = LocalDate.of(2026, 9, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val midAug = LocalDate.of(2026, 8, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val txns = listOf(
            createTxn(1L, midOct),
            createTxn(2L, midSep),
            createTxn(3L, midAug),
        )

        val thisMonth = PeriodHelper.filterTransactionsByPeriod(txns, TimePeriod.THIS_MONTH, refDate, zoneId)
        assertEquals(1, thisMonth.size)
        assertEquals(1L, thisMonth[0].id)

        val lastMonth = PeriodHelper.filterTransactionsByPeriod(txns, TimePeriod.LAST_MONTH, refDate, zoneId)
        assertEquals(1, lastMonth.size)
        assertEquals(2L, lastMonth[0].id)

        val allTime = PeriodHelper.filterTransactionsByPeriod(txns, TimePeriod.ALL_TIME, refDate, zoneId)
        assertEquals(3, allTime.size)
    }
}
