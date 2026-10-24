package com.kharcha.feature.insights

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class PacingStatus {
    ON_TRACK,
    WARNING,
    EXCEEDED,
}

data class SafeToSpendData(
    val dailySafeMinor: Long,
    val currentDailyBurnMinor: Long,
    val totalSpentMinor: Long,
    val monthlyBudgetMinor: Long,
    val daysRemaining: Int,
    val totalDaysInMonth: Int,
    val pacingStatus: PacingStatus,
)

data class CategorySpendShare(
    val categoryName: String,
    val totalMinor: Long,
    val percentage: Float,
    val transactionCount: Int,
)

data class ChannelSpendShare(
    val channelName: String,
    val totalMinor: Long,
    val percentage: Float,
)

data class MerchantSpendShare(
    val merchantName: String,
    val totalMinor: Long,
    val count: Int,
)

object InsightsAnalyticsHelper {
    fun calculateSafeToSpend(
        transactions: List<Transaction>,
        referenceDate: LocalDate = LocalDate.now(),
        customMonthlyBudgetMinor: Long? = null,
    ): SafeToSpendData {
        val totalDaysInMonth = referenceDate.lengthOfMonth()
        val currentDay = referenceDate.dayOfMonth
        val daysElapsed = maxOf(1, currentDay)
        val daysRemaining = maxOf(1, totalDaysInMonth - currentDay + 1)

        val monthStart =
            referenceDate
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val monthEnd =
            referenceDate
                .with(TemporalAdjusters.lastDayOfMonth())
                .atTime(23, 59, 59, 999_000_000)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        val monthTxns = transactions.filter { it.dateTime in monthStart..monthEnd }
        val expenses = monthTxns.filter { it.type == TransactionType.Expense }
        val totalSpent = expenses.sumOf { it.amount }
        val totalIncome = monthTxns.filter { it.type == TransactionType.Income }.sumOf { it.amount }

        val monthlyBudget =
            customMonthlyBudgetMinor
                ?: if (totalIncome > 0) {
                    // Default to 70% of current month's income
                    (totalIncome * 70) / 100
                } else {
                    // Default baseline: 30,000 INR
                    maxOf(totalSpent, 30_000_00L)
                }

        val remainingBudget = maxOf(0L, monthlyBudget - totalSpent)
        val safeDaily = remainingBudget / daysRemaining
        val dailyBurn = totalSpent / daysElapsed

        val status =
            when {
                totalSpent > monthlyBudget -> PacingStatus.EXCEEDED
                dailyBurn > (safeDaily * 125) / 100 -> PacingStatus.WARNING
                else -> PacingStatus.ON_TRACK
            }

        return SafeToSpendData(
            dailySafeMinor = safeDaily,
            currentDailyBurnMinor = dailyBurn,
            totalSpentMinor = totalSpent,
            monthlyBudgetMinor = monthlyBudget,
            daysRemaining = daysRemaining,
            totalDaysInMonth = totalDaysInMonth,
            pacingStatus = status,
        )
    }

    fun calculateCategoryShares(expenses: List<Transaction>): List<CategorySpendShare> {
        val total = expenses.sumOf { it.amount }
        if (total <= 0) return emptyList()

        return expenses
            .groupBy { it.categorySource ?: "Other" }
            .map { (cat, list) ->
                val catSum = list.sumOf { it.amount }
                val pct = catSum.toFloat() / total.toFloat()
                CategorySpendShare(
                    categoryName = cat,
                    totalMinor = catSum,
                    percentage = pct,
                    transactionCount = list.size,
                )
            }.sortedByDescending { it.totalMinor }
    }

    fun calculateChannelShares(expenses: List<Transaction>): List<ChannelSpendShare> {
        val total = expenses.sumOf { it.amount }
        if (total <= 0) return emptyList()

        return expenses
            .groupBy { it.channel?.name ?: "Other" }
            .map { (channel, list) ->
                val channelSum = list.sumOf { it.amount }
                val pct = channelSum.toFloat() / total.toFloat()
                ChannelSpendShare(
                    channelName = channel,
                    totalMinor = channelSum,
                    percentage = pct,
                )
            }.sortedByDescending { it.totalMinor }
    }

    fun calculateTopMerchants(
        expenses: List<Transaction>,
        limit: Int = 5,
    ): List<MerchantSpendShare> {
        return expenses
            .groupBy { it.merchantNormalised ?: it.merchantRaw ?: "Unknown" }
            .map { (merchant, list) ->
                MerchantSpendShare(
                    merchantName = merchant,
                    totalMinor = list.sumOf { it.amount },
                    count = list.size,
                )
            }.sortedByDescending { it.totalMinor }
            .take(limit)
    }

    fun filterTransactionsByPeriod(
        transactions: List<Transaction>,
        period: TimePeriod,
        referenceDate: LocalDate = LocalDate.now(),
    ): List<Transaction> {
        return when (period) {
            TimePeriod.THIS_MONTH -> {
                val start =
                    referenceDate
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                transactions.filter { it.dateTime >= start }
            }
            TimePeriod.LAST_MONTH -> {
                val lastMonthDate = referenceDate.minusMonths(1)
                val start =
                    lastMonthDate
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                val end =
                    lastMonthDate
                        .with(TemporalAdjusters.lastDayOfMonth())
                        .atTime(23, 59, 59, 999_000_000)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                transactions.filter { it.dateTime in start..end }
            }
            TimePeriod.ALL_TIME -> transactions
        }
    }
}

enum class TimePeriod {
    THIS_MONTH,
    LAST_MONTH,
    ALL_TIME,
}
