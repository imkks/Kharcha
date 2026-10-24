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

data class DaySpendIntensity(
    val date: LocalDate,
    val totalExpenseMinor: Long,
    val count: Int,
    val intensityLevel: Int,
)

data class SubscriptionItemUiModel(
    val id: Long,
    val merchantName: String,
    val categoryName: String,
    val amountMinor: Long,
    val cadenceLabel: String,
    val nextChargeDate: LocalDate,
    val priceDiffMinor: Long? = null,
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

    fun calculateSpendingHeatmap(
        transactions: List<Transaction>,
        referenceDate: LocalDate = LocalDate.now(),
        daysCount: Int = 35,
    ): List<DaySpendIntensity> {
        val expenses = transactions.filter { it.type == TransactionType.Expense }
        val zone = ZoneId.systemDefault()
        val expenseByDate =
            expenses.groupBy {
                Instant.ofEpochMilli(it.dateTime).atZone(zone).toLocalDate()
            }

        val startDate = referenceDate.minusDays((daysCount - 1).toLong())
        return (0 until daysCount).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val dayTxns = expenseByDate[date] ?: emptyList()
            val totalMinor = dayTxns.sumOf { it.amount }
            val level =
                when {
                    totalMinor == 0L -> 0
                    totalMinor <= 500_00L -> 1
                    totalMinor <= 2000_00L -> 2
                    totalMinor <= 5000_00L -> 3
                    else -> 4
                }
            DaySpendIntensity(
                date = date,
                totalExpenseMinor = totalMinor,
                count = dayTxns.size,
                intensityLevel = level,
            )
        }
    }

    fun detectRecurringSubscriptions(
        transactions: List<Transaction>,
    ): List<SubscriptionItemUiModel> {
        val expenses = transactions.filter { it.type == TransactionType.Expense }
        val zone = ZoneId.systemDefault()

        return expenses
            .groupBy { it.merchantNormalised ?: it.merchantRaw ?: "Unknown" }
            .filter { (merchant, list) ->
                merchant != "Unknown" && list.size >= 2
            }
            .mapNotNull { (merchant, list) ->
                val sorted = list.sortedBy { it.dateTime }
                val latest = sorted.last()
                val previous = sorted[sorted.size - 2]

                val latestDate = Instant.ofEpochMilli(latest.dateTime).atZone(zone).toLocalDate()
                val prevDate = Instant.ofEpochMilli(previous.dateTime).atZone(zone).toLocalDate()
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(prevDate, latestDate)

                val (isRecurring, cadence, nextDate) =
                    when (daysDiff) {
                        in 6..9 -> Triple(true, "Weekly", latestDate.plusWeeks(1))
                        in 25..35 -> Triple(true, "Monthly", latestDate.plusMonths(1))
                        in 85..95 -> Triple(true, "Quarterly", latestDate.plusMonths(3))
                        else -> Triple(false, "", latestDate)
                    }

                if (isRecurring) {
                    val priceDiff =
                        if (latest.amount != previous.amount) {
                            latest.amount - previous.amount
                        } else {
                            null
                        }

                    SubscriptionItemUiModel(
                        id = latest.id,
                        merchantName = merchant,
                        categoryName = latest.categorySource ?: "Bills & Utilities",
                        amountMinor = latest.amount,
                        cadenceLabel = cadence,
                        nextChargeDate = nextDate,
                        priceDiffMinor = priceDiff,
                    )
                } else {
                    null
                }
            }.sortedBy { it.nextChargeDate }
    }
}

enum class TimePeriod {
    THIS_MONTH,
    LAST_MONTH,
    ALL_TIME,
}
