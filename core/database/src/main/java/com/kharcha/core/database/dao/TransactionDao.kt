package com.kharcha.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE confidence < 0.6 AND isReviewed = 0")
    fun getUnreviewedTransactions(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET categoryId = :categoryId, isReviewed = 1 WHERE id = :transactionId")
    suspend fun updateTransactionCategory(
        transactionId: String,
        categoryId: String,
    )

    @Query("UPDATE transactions SET categoryId = :categoryId, isReviewed = 1 WHERE merchant = :merchant")
    suspend fun updateTransactionsByMerchant(
        merchant: String,
        categoryId: String,
    )

    @Query("UPDATE transactions SET isIgnored = 1, isReviewed = 1 WHERE id = :transactionId")
    suspend fun markTransactionAsIgnored(transactionId: String)
}
