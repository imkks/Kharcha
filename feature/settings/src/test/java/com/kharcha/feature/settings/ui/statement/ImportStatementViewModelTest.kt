package com.kharcha.feature.settings.ui.statement

import com.kharcha.core.common.statement.StatementColumnMapping
import com.kharcha.core.common.statement.StatementImportPlan
import com.kharcha.core.common.statement.StatementPlanRow
import com.kharcha.core.common.statement.StatementRowDecision
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportStatementViewModelTest {

    private fun sampleTxn(id: Long = 1L, amount: Long = 50000L): Transaction =
        Transaction(
            id = id,
            accountId = 10L,
            type = TransactionType.Expense,
            amount = amount,
            dateTime = 1700000000000L,
            merchantNormalised = "Store",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
        )

    @Test
    fun initialStateIsIdle() {
        val idle = ImportStatementUiState.Idle
        assertTrue(idle is ImportStatementUiState)
    }

    @Test
    fun previewStateToggleExclusion() {
        val plannedTx = sampleTxn()
        val plan = StatementImportPlan(
            rows = listOf(
                StatementPlanRow(
                    parsed = plannedTx,
                    decision = StatementRowDecision.Insert(plannedTx),
                ),
            ),
        )

        var state = ImportStatementUiState.Preview(
            plan = plan,
            excluded = emptySet(),
            accountLabel = "HDFC",
            fromSpreadsheet = true,
        )

        assertEquals(0, state.excluded.size)
        state = state.copy(excluded = setOf(0))
        assertEquals(1, state.excluded.size)
        assertTrue(state.excluded.contains(0))

        state = state.copy(excluded = emptySet())
        assertTrue(state.excluded.isEmpty())
    }

    @Test
    fun mappingStateHoldsStructure() {
        val mapping = StatementColumnMapping(
            headerRow = 0,
            dateColumn = 0,
            amountColumn = 1,
            dateFormat = "dd/MM/yyyy",
        )
        val state = ImportStatementUiState.Mapping(
            rows = listOf(listOf("Date", "Amount"), listOf("01/01/2024", "100")),
            mapping = mapping,
            accounts = emptyList(),
            sample = listOf(sampleTxn()),
            parsedCount = 1,
            skippedRows = 0,
            detectedDateFormat = "dd/MM/yyyy",
        )

        assertEquals(0, state.mapping.headerRow)
        assertEquals(1, state.sample.size)
        assertEquals("dd/MM/yyyy", state.detectedDateFormat)
    }
}
