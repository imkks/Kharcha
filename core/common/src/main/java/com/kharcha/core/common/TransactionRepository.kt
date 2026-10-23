package com.kharcha.core.common

import com.kharcha.core.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>

    suspend fun insertTransactions(transactions: List<Transaction>)

    suspend fun insertTransaction(transaction: Transaction)

    suspend fun clearTransactions()
}
