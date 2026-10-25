package com.kharcha.core.common

import com.kharcha.core.model.AmountRangeFilter
import com.kharcha.core.model.DateRangeSelection
import com.kharcha.core.model.ReviewStatusFilter
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Evaluates whether a transaction matches a composite TransactionFilter.
 */
object TransactionFilterMatcher {
    fun matches(
        transaction: Transaction,
        filter: TransactionFilter,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
    ): Boolean {
        // 1. Text Search Query
        if (!TransactionSearchHelper.matches(transaction, filter.query)) {
            return false
        }

        // 2. Transaction Type
        if (filter.types.isNotEmpty() && !filter.types.contains(transaction.type)) {
            return false
        }

        // 3. Category
        if (filter.categories.isNotEmpty()) {
            val cat = transaction.categorySource
            if (cat == null || !filter.categories.any { it.equals(cat, ignoreCase = true) }) {
                return false
            }
        }

        // 4. Channel
        if (filter.channels.isNotEmpty()) {
            val ch = transaction.channel
            if (ch == null || !filter.channels.contains(ch)) {
                return false
            }
        }

        // 5. Account ID
        if (filter.accountIds.isNotEmpty() && !filter.accountIds.contains(transaction.accountId)) {
            return false
        }

        // 6. Review Status
        when (filter.reviewStatus) {
            ReviewStatusFilter.All -> Unit
            ReviewStatusFilter.ReviewedOnly -> if (!transaction.isReviewed) return false
            ReviewStatusFilter.NeedsReviewOnly -> if (transaction.isReviewed) return false
        }

        // 7. Amount Range
        if (!matchesAmount(transaction.amount, filter.amountRange)) {
            return false
        }

        // 8. Date Range
        if (!matchesDate(transaction.dateTime, filter.dateRange, zoneId, today)) {
            return false
        }

        return true
    }

    private fun matchesAmount(amountPaise: Long, range: AmountRangeFilter): Boolean {
        return when (range) {
            AmountRangeFilter.Any -> true
            AmountRangeFilter.Under100 -> amountPaise < 100_00L
            AmountRangeFilter.Between100And500 -> amountPaise in 100_00L..500_00L
            AmountRangeFilter.Between500And2000 -> amountPaise in 500_00L..2_000_00L
            AmountRangeFilter.Between2000And10000 -> amountPaise in 2_000_00L..10_000_00L
            AmountRangeFilter.Above10000 -> amountPaise > 10_000_00L
            is AmountRangeFilter.Custom -> {
                val minPass = range.minPaise?.let { amountPaise >= it } ?: true
                val maxPass = range.maxPaise?.let { amountPaise <= it } ?: true
                minPass && maxPass
            }
        }
    }

    private fun matchesDate(
        epochMillis: Long,
        range: DateRangeSelection,
        zoneId: ZoneId,
        today: LocalDate,
    ): Boolean {
        if (range is DateRangeSelection.AllTime) return true
        if (epochMillis <= 0L) return false

        val txnDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

        return when (range) {
            DateRangeSelection.AllTime -> true
            DateRangeSelection.Today -> txnDate == today
            DateRangeSelection.Yesterday -> txnDate == today.minusDays(1)
            DateRangeSelection.ThisWeek -> {
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                !txnDate.isBefore(startOfWeek) && !txnDate.isAfter(today)
            }
            DateRangeSelection.ThisMonth -> {
                txnDate.year == today.year && txnDate.month == today.month
            }
            DateRangeSelection.LastMonth -> {
                val lastMonthDate = today.minusMonths(1)
                txnDate.year == lastMonthDate.year && txnDate.month == lastMonthDate.month
            }
            is DateRangeSelection.LastNDays -> {
                val threshold = today.minusDays(range.days.toLong())
                !txnDate.isBefore(threshold) && !txnDate.isAfter(today)
            }
            is DateRangeSelection.Custom -> {
                epochMillis in range.startEpochMs..range.endEpochMs
            }
        }
    }
}
