package com.kharcha.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kharcha.core.database.entity.TransactionEntity
import com.kharcha.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val categoryId: String?,
    val totalAmount: Long,
)

data class MonthlyTotal(
    val monthYear: String,
    val totalIncome: Long,
    val totalExpense: Long,
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query(
        "SELECT SUM(amountMinor) FROM transactions " +
            "WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime",
    )
    fun getTotalForPeriod(
        type: TransactionType,
        startTime: Long,
        endTime: Long,
    ): Flow<Long?>

    @Query(
        """
        SELECT categoryId, SUM(amountMinor) as totalAmount
        FROM transactions
        WHERE type = 'EXPENSE' AND timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY categoryId
        ORDER BY totalAmount DESC
    """,
    )
    fun getExpensesGroupedByCategory(
        startTime: Long,
        endTime: Long,
    ): Flow<List<CategoryTotal>>

    @Query(
        """
        SELECT
            strftime('%Y-%m', timestamp / 1000, 'unixepoch', 'localtime') as monthYear,
            SUM(CASE WHEN type = 'INCOME' THEN amountMinor ELSE 0 END) as totalIncome,
            SUM(CASE WHEN type = 'EXPENSE' THEN amountMinor ELSE 0 END) as totalExpense
        FROM transactions
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY monthYear
        ORDER BY monthYear ASC
    """,
    )
    fun getIncomeExpenseGroupedByMonth(
        startTime: Long,
        endTime: Long,
    ): Flow<List<MonthlyTotal>>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC LIMIT :limit",
    )
    fun getRecentTransactions(
        startTime: Long,
        endTime: Long,
        limit: Int = 5,
    ): Flow<List<TransactionEntity>>
}
