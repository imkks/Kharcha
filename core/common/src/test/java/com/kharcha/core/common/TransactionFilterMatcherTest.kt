package com.kharcha.core.common

import com.kharcha.core.model.AmountRangeFilter
import com.kharcha.core.model.Channel
import com.kharcha.core.model.DateRangeSelection
import com.kharcha.core.model.ReviewStatusFilter
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TransactionFilterMatcherTest {
    private val zoneId = ZoneId.of("Asia/Kolkata")
    private val fixedToday = LocalDate.of(2026, 10, 25)
    private val todayEpoch = ZonedDateTime.of(2026, 10, 25, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()
    private val yesterdayEpoch = ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()
    private val lastMonthEpoch = ZonedDateTime.of(2026, 9, 15, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()

    private val baseTxn =
        Transaction(
            id = 1L,
            accountId = 100L,
            type = TransactionType.EXPENSE,
            amount = 450_00L,
            dateTime = todayEpoch,
            merchantNormalised = "Swiggy",
            categorySource = "Food & Dining",
            channel = Channel.UPI,
            isReviewed = false,
        )

    @Test
    fun testDefaultFilterMatchesEverything() {
        val filter = TransactionFilter()
        assertTrue(TransactionFilterMatcher.matches(baseTxn, filter, zoneId, fixedToday))
    }

    @Test
    fun testTypeFilter() {
        val expenseFilter = TransactionFilter(types = setOf(TransactionType.EXPENSE))
        val incomeFilter = TransactionFilter(types = setOf(TransactionType.INCOME))

        assertTrue(TransactionFilterMatcher.matches(baseTxn, expenseFilter, zoneId, fixedToday))
        assertFalse(TransactionFilterMatcher.matches(baseTxn, incomeFilter, zoneId, fixedToday))
    }

    @Test
    fun testCategoryFilter() {
        val matchCat = TransactionFilter(categories = setOf("Food & Dining", "Shopping"))
        val diffCat = TransactionFilter(categories = setOf("Transport"))

        assertTrue(TransactionFilterMatcher.matches(baseTxn, matchCat, zoneId, fixedToday))
        assertFalse(TransactionFilterMatcher.matches(baseTxn, diffCat, zoneId, fixedToday))
    }

    @Test
    fun testChannelFilter() {
        val matchChannel = TransactionFilter(channels = setOf(Channel.UPI))
        val diffChannel = TransactionFilter(channels = setOf(Channel.CARD, Channel.ATM))

        assertTrue(TransactionFilterMatcher.matches(baseTxn, matchChannel, zoneId, fixedToday))
        assertFalse(TransactionFilterMatcher.matches(baseTxn, diffChannel, zoneId, fixedToday))
    }

    @Test
    fun testReviewStatusFilter() {
        val needsReviewFilter = TransactionFilter(reviewStatus = ReviewStatusFilter.NeedsReviewOnly)
        val reviewedFilter = TransactionFilter(reviewStatus = ReviewStatusFilter.ReviewedOnly)

        assertTrue(TransactionFilterMatcher.matches(baseTxn, needsReviewFilter, zoneId, fixedToday))
        assertFalse(TransactionFilterMatcher.matches(baseTxn, reviewedFilter, zoneId, fixedToday))
    }

    @Test
    fun testAmountRangeFilter() {
        val under100 = TransactionFilter(amountRange = AmountRangeFilter.Under100)
        val between100and500 = TransactionFilter(amountRange = AmountRangeFilter.Between100And500)
        val above10000 = TransactionFilter(amountRange = AmountRangeFilter.Above10000)

        assertFalse(TransactionFilterMatcher.matches(baseTxn, under100, zoneId, fixedToday))
        assertTrue(TransactionFilterMatcher.matches(baseTxn, between100and500, zoneId, fixedToday))
        assertFalse(TransactionFilterMatcher.matches(baseTxn, above10000, zoneId, fixedToday))
    }

    @Test
    fun testDateRangeFilter() {
        val todayFilter = TransactionFilter(dateRange = DateRangeSelection.Today)
        val yesterdayFilter = TransactionFilter(dateRange = DateRangeSelection.Yesterday)
        val lastMonthFilter = TransactionFilter(dateRange = DateRangeSelection.LastMonth)

        assertTrue(TransactionFilterMatcher.matches(baseTxn, todayFilter, zoneId, fixedToday))
        assertFalse(TransactionFilterMatcher.matches(baseTxn, yesterdayFilter, zoneId, fixedToday))

        val pastTxn = baseTxn.copy(dateTime = lastMonthEpoch)
        assertTrue(TransactionFilterMatcher.matches(pastTxn, lastMonthFilter, zoneId, fixedToday))
        assertFalse(TransactionFilterMatcher.matches(pastTxn, todayFilter, zoneId, fixedToday))
    }
}
