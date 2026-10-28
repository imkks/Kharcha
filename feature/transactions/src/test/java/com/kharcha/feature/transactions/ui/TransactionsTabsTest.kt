package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionsTabsTest {
    @Test
    fun testTransactionsSubTabsCount() {
        assertEquals(3, TransactionsSubTab.entries.size)
        assertEquals(TransactionsSubTab.TRANSACTIONS, TransactionsSubTab.entries[0])
        assertEquals(TransactionsSubTab.CATEGORIES, TransactionsSubTab.entries[1])
        assertEquals(TransactionsSubTab.MERCHANTS, TransactionsSubTab.entries[2])
    }

    @Test
    fun testMerchantsRankedSorting() {
        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 450_00L,
                type = TransactionType.EXPENSE,
                channel = Channel.UPI,
                dateTime = 1_000L,
                merchantRaw = "Swiggy",
            ),
            Transaction(
                id = 2L,
                amount = 3_000_00L,
                type = TransactionType.EXPENSE,
                channel = Channel.CARD,
                dateTime = 2_000L,
                merchantRaw = "Vijetha",
            ),
            Transaction(
                id = 3L,
                amount = 1_500_00L,
                type = TransactionType.EXPENSE,
                channel = Channel.UPI,
                dateTime = 3_000L,
                merchantRaw = "Swiggy",
            ),
        )

        val ranked = txns
            .filter { it.type == TransactionType.EXPENSE && it.amount > 0 }
            .groupBy { it.merchantRaw ?: "Unknown" }
            .map { (merchant, list) ->
                Triple(merchant, list.sumOf { it.amount }, list.size)
            }
            .sortedByDescending { it.second }

        assertEquals(2, ranked.size)
        assertEquals("Vijetha", ranked[0].first)
        assertEquals(3_000_00L, ranked[0].second)
        assertEquals(1, ranked[0].third)

        assertEquals("Swiggy", ranked[1].first)
        assertEquals(1_950_00L, ranked[1].second)
        assertEquals(2, ranked[1].third)
    }
}
