package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorySource
import com.kharcha.core.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class LayersTest {
    @Test
    fun testDictionaryLayerMatchesExactAndPartial() =
        runBlocking {
            val json =
                """
                {
                    "SWIGGY": "Food Delivery",
                    "AMAZON": "Online"
                }
                """.trimIndent()
            val provider = { ByteArrayInputStream(json.toByteArray()) }
            val layer = DictionaryLayer(provider)

            val input1 = CategorizationInput(45000L, "SWIGGY", "UPI", null, TransactionType.Expense)
            val result1 = layer.categorize(input1)
            assertNotNull(result1)
            assertEquals("Food Delivery", result1?.categoryId)
            assertEquals(CategorySource.Dictionary, result1?.source)
            assertEquals(0.85f, result1?.confidence ?: 0f, 0.01f)

            val input2 = CategorizationInput(45000L, "UPI/SWIGGY INSTAMART", "UPI", null, TransactionType.Expense)
            val result2 = layer.categorize(input2)
            assertNotNull(result2)
            assertEquals("Food Delivery", result2?.categoryId)
            assertEquals(0.8f, result2?.confidence ?: 0f, 0.01f)

            val input3 = CategorizationInput(45000L, "UNKNOWN", "UPI", null, TransactionType.Expense)
            val result3 = layer.categorize(input3)
            assertNull(result3)
        }

    @Test
    fun testHeuristicLayerMatchesChannel() =
        runBlocking {
            val layer = HeuristicLayer()
            val input = CategorizationInput(200000L, null, "ATM", null, TransactionType.Expense)
            val result = layer.categorize(input)
            assertNotNull(result)
            assertEquals("Cash Withdrawal", result?.categoryId)
            assertEquals(CategorySource.Heuristics, result?.source)
            assertEquals(0.7f, result?.confidence ?: 0f, 0.01f)
        }

    @Test
    fun testHeuristicLayerMatchesKeywords() =
        runBlocking {
            val layer = HeuristicLayer()
            val input = CategorizationInput(100000L, "BAJAJ FINANCE EMI", "NACH", null, TransactionType.Expense)
            val result = layer.categorize(input)
            assertNotNull(result)
            assertEquals("Loans and EMI", result?.categoryId)
        }

    @Test
    fun testHeuristicLayerDoesNotMatchFalsePositiveSubstrings() =
        runBlocking {
            val layer = HeuristicLayer()
            // "VI" inside "PROVISION" or "SERVICE" should NOT trigger Bills and Utilities
            val input1 = CategorizationInput(50000L, "PROVISION STORE", "UPI", null, TransactionType.Expense)
            assertNull(layer.categorize(input1))

            val input2 = CategorizationInput(50000L, "CAR SERVICE CENTER", "CARD", null, TransactionType.Expense)
            assertNull(layer.categorize(input2))

            // "EMI" inside "PREMIUM" should NOT trigger Loans and EMI
            val input3 = CategorizationInput(50000L, "PREMIUM DRY FRUITS", "UPI", null, TransactionType.Expense)
            assertNull(layer.categorize(input3))
        }

    @Test
    fun testFallbackLayerAlwaysReturnsUncategorised() =
        runBlocking {
            val layer = FallbackLayer()
            val input = CategorizationInput(100L, "WEIRD MERCH", null, null, TransactionType.Expense)
            val result = layer.categorize(input)
            assertEquals("Uncategorised", result.categoryId)
            assertEquals(CategorySource.Fallback, result.source)
            assertEquals(0.0f, result.confidence, 0.01f)
        }
}
