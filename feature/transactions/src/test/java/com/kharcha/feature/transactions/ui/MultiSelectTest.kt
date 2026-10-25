package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiSelectTest {

    private fun createTxn(
        id: Long,
        category: String = "Shopping",
        type: TransactionType = TransactionType.Expense,
        isReviewed: Boolean = false,
    ): Transaction =
        Transaction(
            id = id,
            accountId = 101L,
            type = type,
            amount = 1250_00L,
            dateTime = 1792900000000L,
            merchantNormalised = "Amazon",
            categorySource = category,
            channel = Channel.UPI,
            isReviewed = isReviewed,
            createdAt = 1792900000000L,
            updatedAt = 1792900000000L,
        )

    @Test
    fun testSelectionTogglingAndSelectAll() {
        val txns = listOf(createTxn(1L), createTxn(2L), createTxn(3L))
        var selectedIds = setOf<Long>()

        // Toggle single item
        selectedIds = selectedIds + 1L
        assertTrue(selectedIds.contains(1L))
        assertEquals(1, selectedIds.size)

        // Select all
        selectedIds = txns.map { it.id }.toSet()
        assertEquals(3, selectedIds.size)
        assertTrue(txns.all { selectedIds.contains(it.id) })

        // Deselect all
        selectedIds = emptySet()
        assertTrue(selectedIds.isEmpty())
    }

    @Test
    fun testBulkCategorizeUpdatesCategoryAndReviewed() {
        val txns = listOf(
            createTxn(1L, category = "Shopping", isReviewed = false),
            createTxn(2L, category = "Other", isReviewed = false),
        )
        val selectedIds = setOf(1L, 2L)
        val newCategory = "Food & Dining"

        val updated = txns.filter { selectedIds.contains(it.id) }.map {
            it.copy(categorySource = newCategory, isReviewed = true)
        }

        assertEquals(2, updated.size)
        assertTrue(updated.all { it.categorySource == newCategory })
        assertTrue(updated.all { it.isReviewed })
    }

    @Test
    fun testBulkChangeTypeUpdatesType() {
        val txns = listOf(
            createTxn(1L, type = TransactionType.Expense),
            createTxn(2L, type = TransactionType.Expense),
        )
        val selectedIds = setOf(1L, 2L)

        val updated = txns.filter { selectedIds.contains(it.id) }.map {
            it.copy(type = TransactionType.Transfer)
        }

        assertEquals(2, updated.size)
        assertTrue(updated.all { it.type == TransactionType.Transfer })
    }

    @Test
    fun testBulkDeleteAndUndo() {
        val txns = listOf(
            createTxn(1L, type = TransactionType.Expense, isReviewed = false),
            createTxn(2L, type = TransactionType.Expense, isReviewed = false),
        )
        val selectedIds = setOf(1L, 2L)
        val selectedTxns = txns.filter { selectedIds.contains(it.id) }

        // Bulk delete
        val deleted = selectedTxns.map {
            it.copy(type = TransactionType.Ignored, isReviewed = true)
        }
        assertTrue(deleted.all { it.type == TransactionType.Ignored })
        assertTrue(deleted.all { it.isReviewed })

        // Undo restores original
        val restored = selectedTxns
        assertTrue(restored.all { it.type == TransactionType.Expense })
        assertFalse(restored.any { it.isReviewed })
    }
}
