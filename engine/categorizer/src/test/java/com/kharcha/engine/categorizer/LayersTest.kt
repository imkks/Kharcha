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
            assertEquals(0.85f, result?.confidence ?: 0f, 0.01f)
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
    fun testHeuristicLayerSubwordsMatchesLocalMerchants() =
        runBlocking {
            val layer = HeuristicLayer()

            // 1. Meat / chicken butcher shops
            val resChicken = layer.categorize(CategorizationInput(12000L, "RAM REDDY CHICKE", "UPI", null, TransactionType.Expense))
            assertEquals("Groceries", resChicken?.categoryId)
            assertEquals(0.80f, resChicken?.confidence ?: 0f, 0.01f)

            // 2. Saloon and personal care
            val resSaloon = layer.categorize(CategorizationInput(25000L, "Nobul hair and beauty saloon", "UPI", null, TransactionType.Expense))
            assertEquals("Personal Care", resSaloon?.categoryId)

            // 3. Pharmacy and medicos
            val resPharmacy = layer.categorize(CategorizationInput(34000L, "LIFE MEDICO", "UPI", null, TransactionType.Expense))
            assertEquals("Pharmacy", resPharmacy?.categoryId)

            // 4. Stationery and education
            val resStationery = layer.categorize(CategorizationInput(6700L, "Dithya stationary", "UPI", null, TransactionType.Expense))
            assertEquals("Education", resStationery?.categoryId)

            // 5. Fuel and filling station
            val resFuel = layer.categorize(CategorizationInput(85000L, "Sandhya Fuel Station", "UPI", null, TransactionType.Expense))
            assertEquals("Fuel", resFuel?.categoryId)
        }

    @Test
    fun testUserRuleLayerMatchesPersonalNamesAndContextualAmounts() =
        runBlocking {
            val rules =
                listOf(
                    com.kharcha.core.model.LocalRule(
                        id = 1L,
                        merchantNormalised = "SATYAJYOTI PANDA",
                        minAmount = null,
                        maxAmount = null,
                        categoryId = "Split",
                        priority = 50,
                    ),
                    com.kharcha.core.model.LocalRule(
                        id = 2L,
                        merchantNormalised = "SATYAJYOTI PANDA",
                        minAmount = 1000000L, // >= Rs 10,000
                        maxAmount = null,
                        categoryId = "Rent",
                        priority = 100,
                    ),
                    com.kharcha.core.model.LocalRule(
                        id = 3L,
                        merchantNormalised = "SURESH REGATI",
                        categoryId = "Groceries",
                        priority = 50,
                    ),
                )

            val layer = UserRuleLayer(ruleProvider = { rules })

            // 1. High value payment to Satyajyoti -> Matches Rent
            val resRent =
                layer.categorize(
                    CategorizationInput(
                        amount = 1400000L,
                        merchantRaw = "SATYAJYOTI PANDA",
                        channel = "UPI",
                        upiId = null,
                        type = TransactionType.Expense,
                    ),
                )
            assertNotNull(resRent)
            assertEquals("Rent", resRent?.categoryId)
            assertEquals(CategorySource.UserRule, resRent?.source)
            assertEquals(1.0f, resRent?.confidence ?: 0f, 0.01f)

            // 2. Low value split to Satyajyoti -> Matches Split
            val resSplit =
                layer.categorize(
                    CategorizationInput(
                        amount = 26200L,
                        merchantRaw = "SATYAJYOTI PANDA",
                        channel = "UPI",
                        upiId = null,
                        type = TransactionType.Expense,
                    ),
                )
            assertNotNull(resSplit)
            assertEquals("Split", resSplit?.categoryId)

            // 3. Indian personal name used as merchant -> Matches User Rule
            val resMerchant =
                layer.categorize(
                    CategorizationInput(
                        amount = 8000L,
                        merchantRaw = "SURESH REGATI",
                        channel = "UPI",
                        upiId = null,
                        type = TransactionType.Expense,
                    ),
                )
            assertNotNull(resMerchant)
            assertEquals("Groceries", resMerchant?.categoryId)
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
