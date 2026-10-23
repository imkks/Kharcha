package com.kharcha.core.database.repository

import com.kharcha.core.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getUnreviewedTransactions(): Flow<List<Transaction>>

    suspend fun updateTransactionCategory(
        transactionId: String,
        categoryId: String,
    )

    suspend fun updateTransactionsByMerchant(
        merchant: String,
        categoryId: String,
    )

    suspend fun markTransactionAsIgnored(transactionId: String)
}
