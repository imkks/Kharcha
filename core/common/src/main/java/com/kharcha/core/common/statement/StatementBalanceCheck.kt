package com.kharcha.core.common.statement

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Statement's closing balance against Kharcha's balance for the same account.
 * All amounts are in Long minor units (paise).
 */
data class StatementBalanceCheck(
    val bankName: String,
    val accountLast4: String,
    val accountLabel: String,
    val currency: String,
    val asOf: LocalDate,
    val statementBalance: Long,
    val appBalance: Long?,
    val correction: Long? = null,
) {
    val difference: Long? get() = appBalance?.let { statementBalance - it }
    val matches: Boolean get() = difference == 0L

    companion object {
        fun correctionFor(
            statementBalance: Long,
            appBalance: Long?,
            currentBalance: Long?,
            recordedAfterStatement: Boolean,
        ): Long? {
            val diff = appBalance?.let { statementBalance - it } ?: return null
            if (diff == 0L) return null
            return if (recordedAfterStatement) {
                null
            } else {
                statementBalance
            }
        }

        fun closingRow(rows: List<Transaction>): Transaction? {
            val withBalance = rows.filter { it.balanceAfter != null }
            if (withBalance.isEmpty()) return null
            val first = withBalance.first()
            val last = withBalance.last()
            val newestFirst = if (first.dateTime != last.dateTime) {
                first.dateTime > last.dateTime
            } else if (withBalance.size >= 2) {
                val second = withBalance[1]
                val a = first.balanceAfter ?: 0L
                val b = second.balanceAfter ?: 0L
                val secondSigned = if (second.type == TransactionType.Income) second.amount else -second.amount
                val firstSigned = if (first.type == TransactionType.Income) first.amount else -first.amount
                (b - a) != secondSigned && (a - b) == firstSigned
            } else {
                false
            }
            return if (newestFirst) first else last
        }

        fun dateOf(row: Transaction): LocalDate =
            Instant.ofEpochMilli(row.dateTime).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
