package com.kharcha.core.common

import com.kharcha.core.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>

    fun getTransactionsSync(): List<Transaction> = emptyList()

    suspend fun insertTransactions(transactions: List<Transaction>)

    suspend fun insertTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun updateTransactions(transactions: List<Transaction>)

    suspend fun updateTransactionAndPropagateMerchant(
        transaction: Transaction,
        newCategory: String,
        newType: com.kharcha.core.model.TransactionType? = null,
        userPreferences: UserPreferences? = null,
    ): Int

    suspend fun linkTransactions(primaryId: Long, secondaryId: Long, isRefund: Boolean = false)

    suspend fun unlinkTransactions(transactionId: Long)

    suspend fun clearTransactions()

    suspend fun deleteTransaction(id: Long)

    suspend fun deleteTransactions(ids: Collection<Long>)

    suspend fun reassignAccountTransactions(fromAccountId: Long, toAccountId: Long)
}
