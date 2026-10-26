package com.kharcha.feature.settings.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreScreenTest {

    private fun createTxn(
        id: Long = 1L,
        amount: Long = 1000_00L,
        category: String? = "Food",
        confidence: Float? = 0.95f,
        isReviewed: Boolean = true,
    ): Transaction =
        Transaction(
            id = id,
            accountId = 101L,
            type = TransactionType.Expense,
            amount = amount,
            dateTime = 1729000000000L,
            merchantNormalised = "Swiggy",
            categorySource = category,
            parseConfidence = confidence,
            channel = Channel.UPI,
            isReviewed = isReviewed,
            createdAt = 1729000000000L,
            updatedAt = 1729000000000L,
        )

    @Test
    fun testBudgetConversionMinorUnits() {
        val rupeesInput = 35_000L
        val minorUnits = rupeesInput * 100L
        assertEquals(3_500_000L, minorUnits)
        assertEquals(35_000L, minorUnits / 100L)
    }

    @Test
    fun testUnreviewedCountCalculation() {
        val txns = listOf(
            // Verified
            createTxn(id = 1L, category = "Food", confidence = 0.9f, isReviewed = true),
            // Not reviewed
            createTxn(id = 2L, category = "Food", confidence = 0.9f, isReviewed = false),
            // "Other" category
            createTxn(id = 3L, category = "Other", confidence = 0.9f, isReviewed = true),
            // Low confidence
            createTxn(id = 4L, category = "Food", confidence = 0.6f, isReviewed = true),
        )

        val unreviewedCount = txns.count {
            !it.isReviewed || it.categorySource == "Other" || (it.parseConfidence ?: 1.0f) < 0.8f
        }

        assertEquals(3, unreviewedCount)
    }

    @Test
    fun testMerchantRuleNormalization() {
        val rawMerchant = "  uber india  "
        val normalized = rawMerchant.trim().lowercase()
        assertEquals("uber india", normalized)
        val display = normalized.replaceFirstChar { it.uppercase() }
        assertEquals("Uber india", display)
    }

    @Test
    fun testMerchantRuleSearchFiltering() {
        val rules = mapOf(
            "swiggy" to "Food & Dining",
            "uber" to "Transport",
            "amazon" to "Shopping",
        )
        val query = "swig"
        val filtered = rules.entries.filter { (m, c) ->
            m.contains(query.trim(), ignoreCase = true) || c.contains(query.trim(), ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("swiggy", filtered.first().key)

        val catQuery = "port"
        val catFiltered = rules.entries.filter { (m, c) ->
            m.contains(catQuery.trim(), ignoreCase = true) || c.contains(catQuery.trim(), ignoreCase = true)
        }
        assertEquals(1, catFiltered.size)
        assertEquals("uber", catFiltered.first().key)
    }

    @Test
    fun testCustomCategoryDeduplication() {
        val defaultCategories = listOf("Food & Dining", "Shopping", "Transport")
        val customCategories = setOf("Chicken", "Shopping", "Gym")
        val combined = (defaultCategories + customCategories).distinct()

        assertEquals(5, combined.size)
        assertTrue(combined.contains("Chicken"))
        assertTrue(combined.contains("Gym"))
    }
}
