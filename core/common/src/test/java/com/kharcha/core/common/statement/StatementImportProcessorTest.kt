package com.kharcha.core.common.statement

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatementImportProcessorTest {

    private val baseEpoch = LocalDate.of(2025, 9, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private class FakeTransactionStore(initial: List<Transaction> = emptyList()) : StatementImportProcessor.TransactionStore {
        val transactions = initial.toMutableList()
        val updated = mutableListOf<Transaction>()
        val inserted = mutableListOf<Transaction>()

        override suspend fun getTransactionsByHashes(hashes: List<String>): List<Transaction> {
            val hashSet = hashes.toSet()
            return transactions.filter { it.smsHash in hashSet }
        }

        override suspend fun getTransactionsBetween(startEpochMs: Long, endEpochMs: Long): List<Transaction> {
            return transactions.filter { it.dateTime in startEpochMs..endEpochMs }
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            val index = transactions.indexOfFirst { it.id == transaction.id }
            if (index >= 0) transactions[index] = transaction
            updated.add(transaction)
        }

        override suspend fun insertTransactions(transactions: List<Transaction>) {
            this.transactions.addAll(transactions)
            inserted.addAll(transactions)
        }
    }

    private fun transaction(
        id: Long = 1,
        merchant: String = "Specific Merchant",
        reference: String? = null,
        amountPaise: Long = 500_00L,
        type: TransactionType = TransactionType.Expense,
        dateTime: Long = baseEpoch,
        hash: String? = "hash_123",
    ): Transaction = Transaction(
        id = id,
        merchantRaw = merchant,
        merchantNormalised = merchant,
        refNumber = reference,
        amount = amountPaise,
        type = type,
        dateTime = dateTime,
        smsHash = hash,
    )

    @Test
    fun amountDateFallbackEnrichesGenericTransactionInsteadOfInsertingDuplicate() = runBlocking {
        val existingSmsWithoutReference = transaction(
            id = 8,
            merchant = "UPI Transaction",
            reference = null,
            hash = "initial_hash",
        )
        val store = FakeTransactionStore(listOf(existingSmsWithoutReference))
        val processor = StatementImportProcessor(store)

        val statementTxn = transaction(
            id = 0,
            merchant = "Swiggy",
            reference = "190200588907",
            hash = "statement_hash",
        )

        val result = processor.process(listOf(statementTxn))

        assertEquals(0, result.imported)
        assertEquals(1, result.enriched)
        assertEquals(0, result.skippedByAmountDate)
        assertEquals("Swiggy", store.updated.single().merchantNormalised)
        assertEquals("190200588907", store.updated.single().refNumber)
        assertTrue(store.inserted.isEmpty())
    }

    @Test
    fun referenceMatchEnrichesExistingTransaction() = runBlocking {
        val existing = transaction(
            id = 10,
            merchant = "Unknown",
            reference = "512345678901",
            hash = "existing_hash",
        )
        val store = FakeTransactionStore(listOf(existing))
        val processor = StatementImportProcessor(store)

        val statementTxn = transaction(
            id = 0,
            merchant = "Zomato",
            reference = "512345678901",
            hash = "statement_hash",
        )

        val result = processor.process(listOf(statementTxn))

        assertEquals(0, result.imported)
        assertEquals(1, result.enriched)
        assertEquals("Zomato", store.updated.single().merchantNormalised)
        assertTrue(store.inserted.isEmpty())
    }

    @Test
    fun newTransactionIsInsertedWhenNoMatchFound() = runBlocking {
        val store = FakeTransactionStore()
        val processor = StatementImportProcessor(store)

        val statementTxn = transaction(
            id = 0,
            merchant = "Amazon",
            reference = "999888777666",
            hash = "new_hash",
        )

        val result = processor.process(listOf(statementTxn))

        assertEquals(1, result.imported)
        assertEquals(0, result.enriched)
        assertEquals(0, result.skippedDuplicates)
        assertEquals(1, store.inserted.size)
        assertEquals("Amazon", store.inserted.first().merchantNormalised)
    }
}
