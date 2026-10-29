package com.kharcha.core.designsystem.component

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DonutChartTest {

    private fun createTxn(
        id: Long,
        amount: Long,
        type: TransactionType = TransactionType.Expense,
        category: String? = "Food",
    ): Transaction =
        Transaction(
            id = id,
            accountId = 1L,
            type = type,
            amount = amount,
            dateTime = 1729000000000L,
            categorySource = category,
            createdAt = 1729000000000L,
            updatedAt = 1729000000000L,
        )

    @Test
    fun buildCategorySlicesEmptyListReturnsEmpty() {
        val slices = buildCategorySlices(emptyList())
        assertTrue(slices.isEmpty())
    }

    @Test
    fun buildCategorySlicesOnlyIncomeReturnsEmpty() {
        val txns = listOf(
            createTxn(1L, 50000_00L, TransactionType.Income, "Salary"),
            createTxn(2L, 10000_00L, TransactionType.Income, "Transfer"),
        )
        val slices = buildCategorySlices(txns)
        assertTrue(slices.isEmpty())
    }

    @Test
    fun buildCategorySlicesTop5AndOthersGrouping() {
        val txns = listOf(
            createTxn(1L, 1000_00L, category = "Food"),
            createTxn(2L, 900_00L, category = "Rent"),
            createTxn(3L, 800_00L, category = "Shopping"),
            createTxn(4L, 700_00L, category = "Travel"),
            createTxn(5L, 600_00L, category = "Bills"),
            createTxn(6L, 500_00L, category = "Entertainment"),
            createTxn(7L, 400_00L, category = "Health"),
        )

        val slices = buildCategorySlices(txns)
        // 5 top + 1 Others = 6 slices
        assertEquals(6, slices.size)
        assertEquals("Food", slices[0].categoryName)
        assertEquals(1000_00L, slices[0].amountMinor)
        assertEquals("Rent", slices[1].categoryName)
        assertEquals("Shopping", slices[2].categoryName)
        assertEquals("Travel", slices[3].categoryName)
        assertEquals("Bills", slices[4].categoryName)
        assertEquals("Others", slices[5].categoryName)
        assertEquals(900_00L, slices[5].amountMinor) // 500 + 400
    }

    @Test
    fun buildCategorySlicesPercentagesSumTo100() {
        val txns = listOf(
            createTxn(1L, 3000_00L, category = "Food"),
            createTxn(2L, 2000_00L, category = "Travel"),
            createTxn(3L, 5000_00L, category = "Rent"),
        )

        val slices = buildCategorySlices(txns)
        assertEquals(3, slices.size)
        val sumPct = slices.sumOf { it.percentage.toDouble() }
        assertTrue("Sum of percentages should be close to 100: $sumPct", sumPct in 99.9..100.1)
    }

    @Test
    fun buildCategorySlicesHandlesNullOrBlankCategory() {
        val txns = listOf(
            createTxn(1L, 1000_00L, category = null),
            createTxn(2L, 2000_00L, category = "   "),
            createTxn(3L, 3000_00L, category = "Food"),
        )

        val slices = buildCategorySlices(txns, otherLabel = "Uncategorized")
        assertEquals(2, slices.size)
        assertEquals("Food", slices[0].categoryName)
        assertEquals(3000_00L, slices[0].amountMinor)
        assertEquals("Uncategorized", slices[1].categoryName)
        assertEquals(3000_00L, slices[1].amountMinor)
    }

    @Test
    fun buildCategorySlicesCustomMaxCategories() {
        val txns = listOf(
            createTxn(1L, 1000_00L, category = "Food"),
            createTxn(2L, 900_00L, category = "Rent"),
            createTxn(3L, 800_00L, category = "Shopping"),
            createTxn(4L, 700_00L, category = "Travel"),
            createTxn(5L, 600_00L, category = "Bills"),
            createTxn(6L, 500_00L, category = "Entertainment"),
            createTxn(7L, 400_00L, category = "Health"),
        )

        val slices = buildCategorySlices(txns, maxCategories = 3)
        // 3 top + 1 Others = 4 slices
        assertEquals(4, slices.size)
        assertEquals("Food", slices[0].categoryName)
        assertEquals(1000_00L, slices[0].amountMinor)
        assertEquals("Rent", slices[1].categoryName)
        assertEquals("Shopping", slices[2].categoryName)
        assertEquals("Others", slices[3].categoryName)
        assertEquals(2200_00L, slices[3].amountMinor) // 700 + 600 + 500 + 400
    }

    @Test
    fun buildCategorySlicesMaxCategoriesLargerThanCount() {
        val txns = listOf(
            createTxn(1L, 1000_00L, category = "Food"),
            createTxn(2L, 900_00L, category = "Rent"),
        )

        val slices = buildCategorySlices(txns, maxCategories = 8)
        assertEquals(2, slices.size)
        assertEquals("Food", slices[0].categoryName)
        assertEquals("Rent", slices[1].categoryName)
    }
}
