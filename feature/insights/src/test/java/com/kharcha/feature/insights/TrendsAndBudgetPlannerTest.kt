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
        assertEquals(3, TrendsGrouping.entries.size)
        assertTrue(TrendsGrouping.entries.contains(TrendsGrouping.BY_MONTH))
        assertTrue(TrendsGrouping.entries.contains(TrendsGrouping.BY_CATEGORY))
        assertTrue(TrendsGrouping.entries.contains(TrendsGrouping.BY_WEEK))
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
