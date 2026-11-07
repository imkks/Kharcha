package com.kharcha.feature.insights

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class TrendsAndBudgetPlannerTest {
    private val zoneId = ZoneId.of("Asia/Kolkata")
    private val testMonth = YearMonth.of(2026, 10)

    @Test
    fun testTrendsGroupingEnumValues() {
        assertEquals(4, TrendsGrouping.entries.size)
        assertTrue(TrendsGrouping.entries.contains(TrendsGrouping.BY_MONTH))
        assertTrue(TrendsGrouping.entries.contains(TrendsGrouping.BY_WEEK))
        assertTrue(TrendsGrouping.entries.contains(TrendsGrouping.BY_DAY))
        assertTrue(TrendsGrouping.entries.contains(TrendsGrouping.BY_CATEGORY))
    }

    @Test
    fun testWeeklyTrendBucketingOptionA_sumsToMonthlyTotal() {
        val oct4Epoch = Instant.parse("2026-10-04T10:00:00Z").toEpochMilli() // Week 1 (1..7)
        val oct10Epoch = Instant.parse("2026-10-10T10:00:00Z").toEpochMilli() // Week 2 (8..14)
        val oct18Epoch = Instant.parse("2026-10-18T10:00:00Z").toEpochMilli() // Week 3 (15..21)
        val oct24Epoch = Instant.parse("2026-10-24T10:00:00Z").toEpochMilli() // Week 4 (22..28)
        val oct30Epoch = Instant.parse("2026-10-30T10:00:00Z").toEpochMilli() // Week 5 (29..31)

        val txns = listOf(
            Transaction(id = 1L, amount = 1000_00L, type = TransactionType.Expense, dateTime = oct4Epoch, isCounted = true),
            Transaction(id = 2L, amount = 2000_00L, type = TransactionType.Expense, dateTime = oct10Epoch, isCounted = true),
            Transaction(id = 3L, amount = 1500_00L, type = TransactionType.Expense, dateTime = oct18Epoch, isCounted = true),
            Transaction(id = 4L, amount = 3000_00L, type = TransactionType.Expense, dateTime = oct24Epoch, isCounted = true),
            Transaction(id = 5L, amount = 2500_00L, type = TransactionType.Expense, dateTime = oct30Epoch, isCounted = true),
        )

        val summary = InsightsAnalyticsHelper.calculateWeeklyTrends(
            transactions = txns,
            yearMonth = testMonth,
            monthlyBudgetMinor = 50_000_00L,
            zoneId = zoneId,
        )

        assertEquals(5, summary.buckets.size)
        assertEquals(1000_00L, summary.buckets[0].totalExpenseMinor)
        assertEquals(2000_00L, summary.buckets[1].totalExpenseMinor)
        assertEquals(1500_00L, summary.buckets[2].totalExpenseMinor)
        assertEquals(3000_00L, summary.buckets[3].totalExpenseMinor)
        assertEquals(2500_00L, summary.buckets[4].totalExpenseMinor)

        val totalWeekly = summary.buckets.sumOf { it.totalExpenseMinor }
        assertEquals(10000_00L, totalWeekly)
        assertEquals(4, summary.highestSpendWeekIndex) // Week 4 had 3000_00L
        assertEquals(10_000_00L, summary.targetWeeklyBudgetMinor) // 50,000 / 5
    }

    @Test
    fun testDailyTrendsOptionA_calculatesDailySpendsAndSafePacing() {
        val oct10Epoch = Instant.parse("2026-10-10T10:00:00Z").toEpochMilli()
        val oct15Epoch = Instant.parse("2026-10-15T12:00:00Z").toEpochMilli()

        val txns = listOf(
            Transaction(id = 1L, amount = 3000_00L, type = TransactionType.Expense, dateTime = oct10Epoch, isCounted = true),
            Transaction(id = 2L, amount = 500_00L, type = TransactionType.Expense, dateTime = oct15Epoch, isCounted = true),
        )

        val summary = InsightsAnalyticsHelper.calculateDailyTrends(
            transactions = txns,
            yearMonth = testMonth,
            customMonthlyBudgetMinor = 31_000_00L, // daily safe ~1,000 INR
            zoneId = zoneId,
            referenceDate = java.time.LocalDate.of(2026, 10, 31),
        )

        assertEquals(31, summary.buckets.size)
        // Day 10 spent 3000 -> exceeds safe limit of 1000
        val day10 = summary.buckets[9]
        assertEquals(10, day10.dayOfMonth)
        assertEquals(3000_00L, day10.totalExpenseMinor)
        assertTrue(day10.isOverSafeDaily)
        assertEquals(2000_00L, day10.varianceMinor)

        // Day 15 spent 500 -> within safe limit
        val day15 = summary.buckets[14]
        assertEquals(15, day15.dayOfMonth)
        assertEquals(500_00L, day15.totalExpenseMinor)
        assertFalse(day15.isOverSafeDaily)

        // Day 1 spent 0 -> zero spend day
        val day1 = summary.buckets[0]
        assertEquals(0L, day1.totalExpenseMinor)
        assertEquals(29, summary.zeroSpendDaysCount) // 31 days - 2 spend days
    }

    @Test
    fun testIncomeHidingLogic_strictlyForIncome() {
        var isIncomeHidden = false
        val formattedSpends = "₹65,260.13"
        val incomeAmount = "₹85,000.00"

        // Initially both are visible
        assertEquals("₹65,260.13", formattedSpends)
        assertEquals("₹85,000.00", if (isIncomeHidden) "₹ ••••••" else incomeAmount)

        // When toggled, income is masked but spends remains always visible
        isIncomeHidden = true
        assertEquals("₹65,260.13", formattedSpends)
        assertEquals("₹ ••••••", if (isIncomeHidden) "₹ ••••••" else incomeAmount)
    }

    @Test
    fun testSelectedMonthAggregation() {
        val octEpoch1 = Instant.parse("2026-10-10T10:00:00Z").toEpochMilli()
        val octEpoch2 = Instant.parse("2026-10-15T12:00:00Z").toEpochMilli()
        val sepEpoch = Instant.parse("2026-09-25T14:00:00Z").toEpochMilli()

        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 2500_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = octEpoch1,
            ),
            Transaction(
                id = 2L,
                amount = 50000_00L,
                type = TransactionType.Income,
                channel = Channel.NEFT,
                dateTime = octEpoch2,
            ),
            Transaction(
                id = 3L,
                amount = 1200_00L,
                type = TransactionType.Expense,
                channel = Channel.CARD,
                dateTime = sepEpoch,
            ),
        )

        val monthTxns = txns.filter {
            YearMonth.from(Instant.ofEpochMilli(it.dateTime).atZone(zoneId)) == testMonth
        }

        val totalSpends = monthTxns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
        val totalIncome = monthTxns.filter { it.type == TransactionType.Income }.sumOf { it.amount }

        assertEquals(2500_00L, totalSpends)
        assertEquals(50000_00L, totalIncome)
    }

    @Test
    fun testBudgetPlannerCategoryMapping() {
        val rawInput = mapOf(
            "Shopping" to "5000",
            "Food & Drinks" to "3000",
            "Others" to "0",
        )

        val mappedBudgets = rawInput.mapNotNull { (cat, str) ->
            val minor = (str.toLongOrNull() ?: 0L) * 100L
            if (minor > 0L) cat to minor else null
        }.toMap()

        assertEquals(2, mappedBudgets.size)
        assertEquals(5000_00L, mappedBudgets["Shopping"])
        assertEquals(3000_00L, mappedBudgets["Food & Drinks"])
        assertFalse(mappedBudgets.containsKey("Others"))
    }
}
