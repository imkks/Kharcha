package com.kharcha.feature.insights

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class InsightsAnalyticsTest {
    private val zoneId = ZoneId.systemDefault()
    private val testDate = LocalDate.of(2026, 10, 15)

    private fun dateToMillis(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun testSafeToSpendCalculationOnTrack() {
        val txns =
            listOf(
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 10_000_00L, // ₹10,000
                    dateTime = dateToMillis(2026, 10, 10),
                    categorySource = "Shopping",
                ),
            )

        // 31 days in October, current day = 15, remaining = 17 days
        val result =
            InsightsAnalyticsHelper.calculateSafeToSpend(
                transactions = txns,
                referenceDate = testDate,
                customMonthlyBudgetMinor = 30_000_00L, // ₹30,000
            )

        assertEquals(PacingStatus.ON_TRACK, result.pacingStatus)
        assertEquals(10_000_00L, result.totalSpentMinor)
        assertEquals(30_000_00L, result.monthlyBudgetMinor)
        assertEquals(17, result.daysRemaining)

        // Remaining = ₹20,000 (2,000,000 paise). Safe daily = 2,000,000 / 17 = 117647 paise
        assertEquals(2_000_000L / 17, result.dailySafeMinor)
    }

    @Test
    fun testSafeToSpendCalculationExceeded() {
        val txns =
            listOf(
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 35_000_00L, // ₹35,000 (exceeds ₹30,000 budget)
                    dateTime = dateToMillis(2026, 10, 12),
                    categorySource = "Shopping",
                ),
            )

        val result =
            InsightsAnalyticsHelper.calculateSafeToSpend(
                transactions = txns,
                referenceDate = testDate,
                customMonthlyBudgetMinor = 30_000_00L,
            )

        assertEquals(PacingStatus.EXCEEDED, result.pacingStatus)
        assertEquals(35_000_00L, result.totalSpentMinor)
        assertEquals(0L, result.dailySafeMinor)
    }

    @Test
    fun testCategorySharesCalculation() {
        val txns =
            listOf(
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 6_000_00L,
                    dateTime = dateToMillis(2026, 10, 5),
                    categorySource = "Food & Dining",
                ),
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 4_000_00L,
                    dateTime = dateToMillis(2026, 10, 8),
                    categorySource = "Shopping",
                ),
            )

        val shares = InsightsAnalyticsHelper.calculateCategoryShares(txns)
        assertEquals(2, shares.size)

        val foodShare = shares[0]
        assertEquals("Food & Dining", foodShare.categoryName)
        assertEquals(6_000_00L, foodShare.totalMinor)
        assertEquals(0.6f, foodShare.percentage, 0.001f)

        val shoppingShare = shares[1]
        assertEquals("Shopping", shoppingShare.categoryName)
        assertEquals(4_000_00L, shoppingShare.totalMinor)
        assertEquals(0.4f, shoppingShare.percentage, 0.001f)
    }

    @Test
    fun testChannelSharesCalculation() {
        val txns =
            listOf(
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 7_000_00L,
                    dateTime = dateToMillis(2026, 10, 5),
                    channel = Channel.UPI,
                ),
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 3_000_00L,
                    dateTime = dateToMillis(2026, 10, 8),
                    channel = Channel.CARD,
                ),
            )

        val channels = InsightsAnalyticsHelper.calculateChannelShares(txns)
        assertEquals(2, channels.size)
        assertEquals("UPI", channels[0].channelName)
        assertEquals(0.7f, channels[0].percentage, 0.001f)
        assertEquals("CARD", channels[1].channelName)
        assertEquals(0.3f, channels[1].percentage, 0.001f)
    }

    @Test
    fun testTopMerchantsRanking() {
        val txns =
            listOf(
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 1_500_00L,
                    dateTime = dateToMillis(2026, 10, 2),
                    merchantNormalised = "Swiggy",
                ),
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 2_500_00L,
                    dateTime = dateToMillis(2026, 10, 4),
                    merchantNormalised = "Swiggy",
                ),
                Transaction(
                    id = 3L,
                    accountId = 1L,
                    type = TransactionType.Expense,
                    amount = 8_000_00L,
                    dateTime = dateToMillis(2026, 10, 6),
                    merchantNormalised = "Amazon",
                ),
            )

        val topMerchants = InsightsAnalyticsHelper.calculateTopMerchants(txns, limit = 2)
        assertEquals(2, topMerchants.size)
        assertEquals("Amazon", topMerchants[0].merchantName)
        assertEquals(8_000_00L, topMerchants[0].totalMinor)

        assertEquals("Swiggy", topMerchants[1].merchantName)
        assertEquals(4_000_00L, topMerchants[1].totalMinor)
        assertEquals(2, topMerchants[1].count)
    }

    @Test
    fun testEmptyTransactionsHandledGracefully() {
        val safeToSpend =
            InsightsAnalyticsHelper.calculateSafeToSpend(
                transactions = emptyList(),
                referenceDate = testDate,
            )
        assertEquals(PacingStatus.ON_TRACK, safeToSpend.pacingStatus)
        assertEquals(0L, safeToSpend.totalSpentMinor)

        val catShares = InsightsAnalyticsHelper.calculateCategoryShares(emptyList())
        assertTrue(catShares.isEmpty())

        val channelShares = InsightsAnalyticsHelper.calculateChannelShares(emptyList())
        assertTrue(channelShares.isEmpty())
    }
}
