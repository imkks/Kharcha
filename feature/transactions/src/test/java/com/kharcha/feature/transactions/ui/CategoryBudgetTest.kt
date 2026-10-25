package com.kharcha.feature.transactions.ui

import com.kharcha.core.designsystem.component.CategorySlice
import com.kharcha.core.designsystem.theme.CategoryColors
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBudgetTest {
    @Test
    fun testCategoryBudgets_lookupAndDefault() {
        val categoryBudgets =
            mapOf(
                "food & dining" to 15_000_00L,
                "shopping" to 8_000_00L,
            )

        val foodBudget = categoryBudgets["Food & Dining".lowercase()] ?: 0L
        val shoppingBudget = categoryBudgets["Shopping".lowercase()] ?: 0L
        val travelBudget = categoryBudgets["Travel".lowercase()] ?: 0L

        assertEquals(15_000_00L, foodBudget)
        assertEquals(8_000_00L, shoppingBudget)
        assertEquals(0L, travelBudget)
    }

    @Test
    fun testCategorySlices_partitionUnknownFromKnown() {
        val slices =
            listOf(
                CategorySlice(
                    categoryName = "Food & Dining",
                    amountMinor = 12_500_00L,
                    percentage = 50f,
                    color = CategoryColors.Food,
                ),
                CategorySlice(
                    categoryName = "Others",
                    amountMinor = 5_000_00L,
                    percentage = 20f,
                    color = CategoryColors.Other,
                ),
                CategorySlice(
                    categoryName = "Shopping",
                    amountMinor = 7_500_00L,
                    percentage = 30f,
                    color = CategoryColors.Shopping,
                ),
            )

        val (unknownSlices, knownSlices) =
            slices.partition {
                it.categoryName.equals("Others", ignoreCase = true) ||
                    it.categoryName.equals("Unknown", ignoreCase = true) ||
                    it.categoryName.equals("Uncategorized", ignoreCase = true)
            }

        assertEquals(1, unknownSlices.size)
        assertEquals("Others", unknownSlices.first().categoryName)
        assertEquals(2, knownSlices.size)
    }

    @Test
    fun testAccountOption_derivationFromTransactions() {
        val txns =
            listOf(
                Transaction(
                    id = 1L,
                    accountId = 101L,
                    type = TransactionType.Expense,
                    amount = 450_00L,
                    dateTime = System.currentTimeMillis(),
                    channel = Channel.CARD,
                    refNumber = "TXN45671999",
                ),
                Transaction(
                    id = 2L,
                    accountId = 101L,
                    type = TransactionType.Expense,
                    amount = 150_00L,
                    dateTime = System.currentTimeMillis(),
                    channel = Channel.CARD,
                    refNumber = "TXN45671999",
                ),
                Transaction(
                    id = 3L,
                    accountId = 102L,
                    type = TransactionType.Expense,
                    amount = 200_00L,
                    dateTime = System.currentTimeMillis(),
                    channel = Channel.UPI,
                ),
            )

        val groups =
            txns.groupBy { txn ->
                val last4 = txn.refNumber?.takeLast(4)?.trim()
                when {
                    !last4.isNullOrEmpty() -> "HDFC $last4"
                    txn.channel != null -> "${txn.channel.name} Account"
                    else -> "Account #${txn.accountId}"
                }
            }

        assertEquals(2, groups.size)
        assertTrue(groups.containsKey("HDFC 1999"))
        assertTrue(groups.containsKey("UPI Account"))

        val hdfcSpends = groups["HDFC 1999"]!!.sumOf { it.amount }
        assertEquals(600_00L, hdfcSpends)
    }
}
