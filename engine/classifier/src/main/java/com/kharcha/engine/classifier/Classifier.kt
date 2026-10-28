package com.kharcha.engine.classifier

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.ParsedTxn
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import kotlin.math.abs

class Classifier {
    fun classify(
        transaction: ParsedTxn,
        accountId: Long,
        recentTransactions: List<Transaction> = emptyList(),
        transferWindowMs: Long = 10 * 60 * 1000,
        refundWindowMs: Long = 90L * 24 * 60 * 60 * 1000,
        cardPaymentWindowMs: Long = 7L * 24 * 60 * 60 * 1000,
    ): Pair<TransactionType, Long?> {
        var type =
            if (transaction.direction == Direction.DEBIT) {
                TransactionType.Expense
            } else {
                TransactionType.Income
            }

        var linkedTxnId: Long? = null
        val txnDateTime = transaction.dateTime
        val merchantRawLower = transaction.merchantRaw?.lowercase() ?: ""

        if (recentTransactions.isNotEmpty() && txnDateTime != null) {
            // Note: we can expand transferWindowMs for credit card payments since those take days
            val matchingTransfer =
                recentTransactions.find {
                    val windowToUse =
                        if (it.channel == Channel.CARD || transaction.channel == Channel.CARD) {
                            cardPaymentWindowMs
                        } else {
                            transferWindowMs
                        }

                    it.amount == transaction.amountMinor &&
                        (
                            (transaction.direction == Direction.DEBIT && it.type == TransactionType.Income) ||
                                (transaction.direction == Direction.CREDIT && it.type == TransactionType.Expense)
                        ) &&
                        it.accountId != accountId &&
                        abs(it.dateTime - txnDateTime) <= windowToUse
                }
            if (matchingTransfer != null) {
                val finalType =
                    if (transaction.direction == Direction.DEBIT) TransactionType.Expense else TransactionType.Income
                return Pair(finalType, matchingTransfer.id)
            }

            if (transaction.direction == Direction.CREDIT) {
                val matchingRefund =
                    recentTransactions.find {
                        it.type == TransactionType.Expense &&
                            it.accountId == accountId &&
                            it.amount >= transaction.amountMinor &&
                            (txnDateTime - it.dateTime) <= refundWindowMs &&
                            (txnDateTime - it.dateTime) >= 0 &&

                            (
                                it.merchantRaw?.lowercase() == merchantRawLower ||
                                    it.merchantNormalised?.lowercase() == merchantRawLower
                            )
                    }

                if (matchingRefund != null) {
                    return Pair(TransactionType.Income, matchingRefund.id)
                }
            }
        }

        // According to docs, salary detection just flags it as income but we will be mapping it in Categorizer layer
        // However, we want to ensure we don't accidentally match "salon" or "universal".

        if (transaction.direction == Direction.CREDIT) {
            if (Regex("\\b(salary|payroll)\\b").containsMatchIn(merchantRawLower)) {
                type = TransactionType.Income // Explicitly set, even though default is Income
            }
        }

        return Pair(type, linkedTxnId)
    }
}
