package com.kharcha.core.database.repository

import com.kharcha.core.database.dao.TransactionDao
import com.kharcha.core.database.dao.TransactionEntity
import com.kharcha.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
    ) : TransactionRepository {
        override fun getUnreviewedTransactions(): Flow<List<Transaction>> {
            return transactionDao.getUnreviewedTransactions().map { entities ->
                entities.map { it.toModel() }
            }
        }

        override suspend fun updateTransactionCategory(
            transactionId: String,
            categoryId: String,
        ) {
            transactionDao.updateTransactionCategory(transactionId, categoryId)
        }

        override suspend fun updateTransactionsByMerchant(
            merchant: String,
            categoryId: String,
        ) {
            transactionDao.updateTransactionsByMerchant(merchant, categoryId)
        }

        override suspend fun markTransactionAsIgnored(transactionId: String) {
            transactionDao.markTransactionAsIgnored(transactionId)
        }

        private fun TransactionEntity.toModel(): Transaction {
            return Transaction(
                id = id,
                merchant = merchant,
                amount = amount,
                date = date,
                categoryId = categoryId,
                originalSms = originalSms,
                confidence = confidence,
                isIgnored = isIgnored,
                isReviewed = isReviewed,
                accountId = accountId,
            )
        }
    }
