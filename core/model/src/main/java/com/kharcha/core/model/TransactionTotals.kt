package com.kharcha.core.model

/**
 * Builds a mapping of target expense transaction ID to total refund minor units (paise),
 * derived from income transactions linked to expenses (or vice versa).
 */
fun Iterable<Transaction>.buildRefundMap(): Map<Long, Long> {
    val map = mutableMapOf<Long, Long>()
    val txnsById = associateBy { it.id }
    for (txn in this) {
        if (txn.type == TransactionType.Income && txn.linkedTransactionId != null) {
            val linkedTxn = txnsById[txn.linkedTransactionId]
            if (linkedTxn == null || linkedTxn.type == TransactionType.Expense) {
                map[txn.linkedTransactionId] = (map[txn.linkedTransactionId] ?: 0L) + txn.amount
            }
        } else if (txn.type == TransactionType.Expense && txn.linkedTransactionId != null) {
            val linkedTxn = txnsById[txn.linkedTransactionId]
            if (linkedTxn != null && linkedTxn.type == TransactionType.Income) {
                if (!map.containsKey(txn.id)) {
                    map[txn.id] = linkedTxn.amount
                }
            }
        }
    }
    return map
}

/**
 * Single source of truth for the counted amount of a transaction toward totals and analytics.
 * - If not counted (!isCounted): returns 0L.
 * - If Expense: returns (amount - refundDeduction).coerceAtLeast(0L).
 * - If Income: returns amount.
 */
fun Transaction.countedAmount(refundDeduction: Long = 0L): Long {
    if (!isCounted) return 0L
    return when (type) {
        TransactionType.Expense -> (amount - refundDeduction).coerceAtLeast(0L)
        TransactionType.Income -> amount
    }
}

/**
 * Returns a copy of this transaction with [amount] set to [countedAmount], or null
 * when nothing of it counts (isCounted == false or fully refunded).
 * Intended ONLY for computing totals, graphs, slices, and statistics.
 */
fun Transaction.forTotals(refundDeduction: Long = 0L): Transaction? {
    val counted = countedAmount(refundDeduction)
    return when {
        counted <= 0L -> null
        counted == amount -> this
        else -> copy(amount = counted)
    }
}

/**
 * Filters and transforms a collection of transactions for totals and analytics:
 * - Drops transactions with isCounted == false or counted amount <= 0.
 * - Reduces expense amounts by any linked refunds in [refundMap].
 */
fun Iterable<Transaction>.forTotals(refundMap: Map<Long, Long> = buildRefundMap()): List<Transaction> {
    return mapNotNull { txn ->
        val refundDeduction = refundMap[txn.id] ?: 0L
        txn.forTotals(refundDeduction)
    }
}
