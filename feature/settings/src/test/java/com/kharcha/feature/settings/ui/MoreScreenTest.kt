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
            type = TransactionType.EXPENSE,
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

    @Test
    fun testAccountLinkingGroupingLogic() {
        val primaryAccountId = 100L
        val linkedSubAccountIds = setOf(101L, 102L)
        val allAssociatedIds = setOf(primaryAccountId) + linkedSubAccountIds

        val txns = listOf(
            createTxn(id = 1L).copy(accountId = 101L),
            createTxn(id = 2L).copy(accountId = 102L),
            createTxn(id = 3L).copy(accountId = 999L),
        )

        val associatedTxns = txns.filter { it.accountId in allAssociatedIds }
        assertEquals(2, associatedTxns.size)
        assertTrue(associatedTxns.any { it.accountId == 101L })
        assertTrue(associatedTxns.any { it.accountId == 102L })

        // When unlinking 102L:
        val remainingLinkedIds = linkedSubAccountIds - 102L
        val updatedAssociatedIds = setOf(primaryAccountId) + remainingLinkedIds
        val updatedTxns = txns.filter { it.accountId in updatedAssociatedIds }
        assertEquals(1, updatedTxns.size)
        assertEquals(101L, updatedTxns[0].accountId)
    }

    @Test
    fun testAccountExpensePacingFilter() {
        val accounts = listOf(
            com.kharcha.core.model.Account(
                id = 1L,
                name = "HDFC",
                bankName = "HDFC",
                last4 = "1234",
                type = com.kharcha.core.model.AccountType.Savings,
                isCountedAsExpense = true,
            ),
            com.kharcha.core.model.Account(
                id = 2L,
                name = "Office Reimbursement Card",
                bankName = "Axis",
                last4 = "5678",
                type = com.kharcha.core.model.AccountType.CreditCard,
                isCountedAsExpense = false,
            ),
        )

        val expenseAccountIds = accounts.filter { it.isCountedAsExpense }.map { it.id }.toSet()
        assertEquals(1, expenseAccountIds.size)
        assertTrue(expenseAccountIds.contains(1L))
        assertFalse(expenseAccountIds.contains(2L))
    }

    @Test
    fun testEyeIconToggleIsCountedAsExpense() {
        val account = com.kharcha.core.model.Account(
            id = 10L,
            name = "ICICI Bank",
            bankName = "ICICI",
            last4 = "4321",
            type = com.kharcha.core.model.AccountType.Savings,
            isCountedAsExpense = true,
        )

        val toggledOff = account.copy(isCountedAsExpense = !account.isCountedAsExpense)
        assertFalse(toggledOff.isCountedAsExpense)

        val toggledBackOn = toggledOff.copy(isCountedAsExpense = !toggledOff.isCountedAsExpense)
        assertTrue(toggledBackOn.isCountedAsExpense)
    }

    @Test
    fun testLinkedAccountsPageFiltersOnlyPrimaryWithSubAccounts() {
        val primaryWithSub = com.kharcha.core.model.Account(
            id = 1L,
            name = "HDFC Savings",
            bankName = "HDFC",
            last4 = "1999",
            type = com.kharcha.core.model.AccountType.Savings,
        )
        val linkedCard1 = com.kharcha.core.model.Account(
            id = 2L,
            name = "HDFC Regalia",
            bankName = "HDFC",
            last4 = "8888",
            type = com.kharcha.core.model.AccountType.CreditCard,
            linkedPrimaryAccountId = 1L,
        )
        val linkedCard2 = com.kharcha.core.model.Account(
            id = 3L,
            name = "HDFC Millennia",
            bankName = "HDFC",
            last4 = "7777",
            type = com.kharcha.core.model.AccountType.CreditCard,
            linkedPrimaryAccountId = 1L,
        )
        val standalonePrimary = com.kharcha.core.model.Account(
            id = 4L,
            name = "SBI Savings",
            bankName = "SBI",
            last4 = "3333",
            type = com.kharcha.core.model.AccountType.Savings,
        )

        val allAccounts = listOf(primaryWithSub, linkedCard1, linkedCard2, standalonePrimary)

        // Filter only primary accounts that have linked sub accounts
        val linkedPrimaries = allAccounts.filter { primary ->
            allAccounts.any { it.linkedPrimaryAccountId == primary.id }
        }

        assertEquals(1, linkedPrimaries.size)
        assertEquals(1L, linkedPrimaries.first().id)

        // Sub accounts in tree structure
        val subAccounts = allAccounts.filter { it.linkedPrimaryAccountId == linkedPrimaries.first().id }
        assertEquals(2, subAccounts.size)
        assertTrue(subAccounts.any { it.id == 2L })
        assertTrue(subAccounts.any { it.id == 3L })

        // When unlinking linkedCard2:
        val remainingSubIds = subAccounts.filter { it.id != 3L }.map { it.id }.toSet()
        assertEquals(setOf(2L), remainingSubIds)
    }
}
