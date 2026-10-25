package com.kharcha.core.model

/**
 * Filter configuration for querying and filtering transactions in memory or via repository.
 * Follows SRS FR-6.2 and UI Spec Section 5.3.
 */
data class TransactionFilter(
    val query: String = "",
    val types: Set<TransactionType> = emptySet(),
    val dateRange: DateRangeSelection = DateRangeSelection.AllTime,
    val categories: Set<String> = emptySet(),
    val amountRange: AmountRangeFilter = AmountRangeFilter.Any,
    val channels: Set<Channel> = emptySet(),
    val accountIds: Set<Long> = emptySet(),
    val reviewStatus: ReviewStatusFilter = ReviewStatusFilter.All,
) {
    val isActive: Boolean
        get() =
            query.isNotBlank() ||
                types.isNotEmpty() ||
                dateRange !is DateRangeSelection.AllTime ||
                categories.isNotEmpty() ||
                amountRange !is AmountRangeFilter.Any ||
                channels.isNotEmpty() ||
                accountIds.isNotEmpty() ||
                reviewStatus != ReviewStatusFilter.All

    val activeFilterCount: Int
        get() {
            var count = 0
            if (types.isNotEmpty()) count++
            if (dateRange !is DateRangeSelection.AllTime) count++
            if (categories.isNotEmpty()) count += categories.size
            if (amountRange !is AmountRangeFilter.Any) count++
            if (channels.isNotEmpty()) count += channels.size
            if (accountIds.isNotEmpty()) count += accountIds.size
            if (reviewStatus != ReviewStatusFilter.All) count++
            return count
        }
}

sealed interface DateRangeSelection {
    object AllTime : DateRangeSelection
    object Today : DateRangeSelection
    object Yesterday : DateRangeSelection
    object ThisWeek : DateRangeSelection
    object ThisMonth : DateRangeSelection
    object LastMonth : DateRangeSelection
    data class LastNDays(val days: Int) : DateRangeSelection
    data class Custom(val startEpochMs: Long, val endEpochMs: Long) : DateRangeSelection
}

sealed interface AmountRangeFilter {
    object Any : AmountRangeFilter
    object Under100 : AmountRangeFilter
    object Between100And500 : AmountRangeFilter
    object Between500And2000 : AmountRangeFilter
    object Between2000And10000 : AmountRangeFilter
    object Above10000 : AmountRangeFilter
    data class Custom(val minPaise: Long?, val maxPaise: Long?) : AmountRangeFilter
}

enum class ReviewStatusFilter {
    All,
    ReviewedOnly,
    NeedsReviewOnly,
}
