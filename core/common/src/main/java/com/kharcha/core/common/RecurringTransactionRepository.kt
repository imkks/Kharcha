package com.kharcha.core.common

import com.kharcha.core.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecurringTransactionRepository {
    fun getRecurringTransactions(): Flow<List<RecurringTransaction>>
    fun getRecurringTransactionsSync(): List<RecurringTransaction>
    suspend fun saveRecurringTransaction(item: RecurringTransaction): Long
    suspend fun deleteRecurringTransaction(id: Long)
    suspend fun setActive(id: Long, isActive: Boolean)
    suspend fun materializeDue(today: LocalDate = LocalDate.now()): Int
}
