package com.kharcha.feature.insights

import com.kharcha.core.common.PeriodHelper
import com.kharcha.core.model.TimePeriod
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.isCounted
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

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

data class WeeklyTrendBucket(
    val weekIndex: Int,
    val label: String,
    val dateRangeLabel: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val totalExpenseMinor: Long,
    val totalIncomeMinor: Long,
    val transactionCount: Int,
    val isCurrentWeek: Boolean = false,
)

data class WeeklyTrendsSummary(
    val buckets: List<WeeklyTrendBucket>,
    val averageWeeklyExpenseMinor: Long,
    val targetWeeklyBudgetMinor: Long,
    val highestSpendWeekIndex: Int?,
    val highestSpendMinor: Long,
)

data class DailyTrendBucket(
    val date: LocalDate,
    val dayOfMonth: Int,
    val dayOfWeekLabel: String,
    val totalExpenseMinor: Long,
    val totalIncomeMinor: Long,
    val transactionCount: Int,
    val isOverSafeDaily: Boolean,
    val varianceMinor: Long,
    val isToday: Boolean = false,
    val isFuture: Boolean = false,
)

data class DailyTrendsSummary(
    val buckets: List<DailyTrendBucket>,
    val dailySafeMinor: Long,
    val dailyBudgetPaceMinor: Long,
    val averageDailySpendMinor: Long,
    val highestSpendDay: LocalDate?,
    val highestSpendMinor: Long,
    val zeroSpendDaysCount: Int,
    val daysRemaining: Int,
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
    val transactionCount: Int = 0,
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
        val expenses = monthTxns.filter { it.type == TransactionType.Expense && it.isCounted }
        val totalSpent = expenses.sumOf { it.amount }
        val totalIncome = monthTxns.filter { it.type == TransactionType.Income && it.isCounted }.sumOf { it.amount }

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
                    transactionCount = list.size,
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
    ): List<Transaction> = PeriodHelper.filterTransactionsByPeriod(transactions, period, referenceDate)

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

    fun calculateWeeklyTrends(
        transactions: List<Transaction>,
        yearMonth: YearMonth = YearMonth.now(),
        monthlyBudgetMinor: Long = 0L,
        zoneId: ZoneId = ZoneId.systemDefault(),
        referenceDate: LocalDate = LocalDate.now(),
    ): WeeklyTrendsSummary {
        val daysInMonth = yearMonth.lengthOfMonth()
        val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
        val monthName = yearMonth.format(monthFormatter)

        val ranges = listOf(
            1 to minOf(7, daysInMonth),
            8 to minOf(14, daysInMonth),
            15 to minOf(21, daysInMonth),
            22 to minOf(28, daysInMonth),
            29 to daysInMonth,
        ).filter { it.first <= daysInMonth }

        val monthStart = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val monthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
        val countedTxns = transactions.filter { it.isCounted && it.dateTime in monthStart..monthEnd }

        val buckets = ranges.mapIndexed { index, (startDay, endDay) ->
            val weekIndex = index + 1
            val weekStartMs = yearMonth.atDay(startDay).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val weekEndMs = yearMonth.atDay(endDay).atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

            val weekTxns = countedTxns.filter { it.dateTime in weekStartMs..weekEndMs }
            val expenseTotal = weekTxns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
            val incomeTotal = weekTxns.filter { it.type == TransactionType.Income }.sumOf { it.amount }

            val isCurrentWeek = yearMonth == YearMonth.from(referenceDate) &&
                referenceDate.dayOfMonth in startDay..endDay

            WeeklyTrendBucket(
                weekIndex = weekIndex,
                label = "Week $weekIndex",
                dateRangeLabel = "$startDay - $endDay $monthName",
                startEpochMs = weekStartMs,
                endEpochMs = weekEndMs,
                totalExpenseMinor = expenseTotal,
                totalIncomeMinor = incomeTotal,
                transactionCount = weekTxns.size,
                isCurrentWeek = isCurrentWeek,
            )
        }

        val totalExpenses = buckets.sumOf { it.totalExpenseMinor }
        val activeCount = if (yearMonth == YearMonth.from(referenceDate)) {
            val curWeek = buckets.indexOfFirst { it.isCurrentWeek }
            if (curWeek >= 0) curWeek + 1 else buckets.size
        } else {
            buckets.size
        }
        val avgWeekly = if (activeCount > 0) totalExpenses / activeCount else 0L
        val targetWeekly = if (buckets.isNotEmpty()) monthlyBudgetMinor / buckets.size else 0L
        val highestBucket = buckets.maxByOrNull { it.totalExpenseMinor }

        return WeeklyTrendsSummary(
            buckets = buckets,
            averageWeeklyExpenseMinor = avgWeekly,
            targetWeeklyBudgetMinor = targetWeekly,
            highestSpendWeekIndex = highestBucket?.weekIndex,
            highestSpendMinor = highestBucket?.totalExpenseMinor ?: 0L,
        )
    }

    fun calculateDailyTrends(
        transactions: List<Transaction>,
        yearMonth: YearMonth = YearMonth.now(),
        customMonthlyBudgetMinor: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
        referenceDate: LocalDate = LocalDate.now(),
    ): DailyTrendsSummary {
        val daysInMonth = yearMonth.lengthOfMonth()
        val monthStart = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val monthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
        val countedTxns = transactions.filter { it.isCounted && it.dateTime in monthStart..monthEnd }

        val isCurrentMonth = (yearMonth == YearMonth.from(referenceDate))
        val safeData = calculateSafeToSpend(
            transactions = transactions,
            referenceDate = if (isCurrentMonth) referenceDate else yearMonth.atDay(1),
            customMonthlyBudgetMinor = customMonthlyBudgetMinor,
        )
        val dailySafeMinor = if (isCurrentMonth) safeData.dailySafeMinor else (safeData.monthlyBudgetMinor / daysInMonth)
        val baselineDailyBudgetMinor = if (safeData.monthlyBudgetMinor > 0L) {
            safeData.monthlyBudgetMinor / daysInMonth
        } else {
            0L
        }

        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        val buckets = (1..daysInMonth).map { day ->
            val date = yearMonth.atDay(day)
            val dayStartMs = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEndMs = date.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

            val dayTxns = countedTxns.filter { it.dateTime in dayStartMs..dayEndMs }
            val expenseTotal = dayTxns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
            val incomeTotal = dayTxns.filter { it.type == TransactionType.Income }.sumOf { it.amount }

            val isToday = isCurrentMonth && (date == referenceDate)
            val isFuture = isCurrentMonth && date.isAfter(referenceDate)
            val isOverSafe = baselineDailyBudgetMinor > 0L && expenseTotal > baselineDailyBudgetMinor
            val variance = if (baselineDailyBudgetMinor > 0L) expenseTotal - baselineDailyBudgetMinor else 0L

            DailyTrendBucket(
                date = date,
                dayOfMonth = day,
                dayOfWeekLabel = date.format(dayFormatter),
                totalExpenseMinor = expenseTotal,
                totalIncomeMinor = incomeTotal,
                transactionCount = dayTxns.size,
                isOverSafeDaily = isOverSafe,
                varianceMinor = variance,
                isToday = isToday,
                isFuture = isFuture,
            )
        }

        val evaluatedBuckets = if (isCurrentMonth) buckets.filter { !it.isFuture } else buckets
        val zeroSpendCount = evaluatedBuckets.count { it.totalExpenseMinor == 0L }
        val highestDayBucket = evaluatedBuckets.maxByOrNull { it.totalExpenseMinor }
        val totalSpentSoFar = evaluatedBuckets.sumOf { it.totalExpenseMinor }
        val avgDaily = if (evaluatedBuckets.isNotEmpty()) totalSpentSoFar / evaluatedBuckets.size else 0L

        return DailyTrendsSummary(
            buckets = buckets,
            dailySafeMinor = dailySafeMinor,
            dailyBudgetPaceMinor = baselineDailyBudgetMinor,
            averageDailySpendMinor = avgDaily,
            highestSpendDay = highestDayBucket?.date,
            highestSpendMinor = highestDayBucket?.totalExpenseMinor ?: 0L,
            zeroSpendDaysCount = zeroSpendCount,
            daysRemaining = if (isCurrentMonth) safeData.daysRemaining else 0,
        )
    }
}

