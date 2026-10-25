package com.kharcha.core.common

import com.kharcha.core.model.Transaction
import java.util.Locale

/**
 * Parses smart search tokens (qualifiers such as `>500`, `#food`, `@upi`)
 * and matches against transactions.
 * Follows UI Spec Section 5.3 & Research Recommendation #5.
 */
object TransactionSearchHelper {
    fun matches(transaction: Transaction, rawQuery: String): Boolean {
        val query = rawQuery.trim().lowercase(Locale.getDefault())
        if (query.isEmpty()) return true

        val tokens = query.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true

        return tokens.all { token -> matchesToken(transaction, token) }
    }

    fun extractMerchantSuggestions(
        transactions: List<Transaction>,
        prefix: String,
        limit: Int = 5,
    ): List<String> {
        val clean = prefix.trim().lowercase(Locale.getDefault())
        if (clean.isEmpty()) return emptyList()

        return transactions
            .mapNotNull { it.merchantNormalised ?: it.merchantRaw }
            .filter { it.isNotBlank() && it.lowercase(Locale.getDefault()).startsWith(clean) }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .take(limit)
    }

    private fun matchesToken(transaction: Transaction, token: String): Boolean {
        // 1. Amount threshold comparison
        if (token.startsWith(">=") && token.length > 2) {
            val threshold = token.substring(2).toLongOrNull()?.times(100L) ?: return false
            return transaction.amount >= threshold
        }
        if (token.startsWith(">") && token.length > 1) {
            val threshold = token.substring(1).toLongOrNull()?.times(100L) ?: return false
            return transaction.amount > threshold
        }
        if (token.startsWith("<=") && token.length > 2) {
            val threshold = token.substring(2).toLongOrNull()?.times(100L) ?: return false
            return transaction.amount <= threshold
        }
        if (token.startsWith("<") && token.length > 1) {
            val threshold = token.substring(1).toLongOrNull()?.times(100L) ?: return false
            return transaction.amount < threshold
        }

        // 2. Category token: #food
        if (token.startsWith("#") && token.length > 1) {
            val catPrefix = token.substring(1)
            return transaction.categorySource?.lowercase(Locale.getDefault())?.contains(catPrefix) == true
        }

        // 3. Channel token: @upi, @card, @atm
        if (token.startsWith("@") && token.length > 1) {
            val chPrefix = token.substring(1)
            return transaction.channel?.name?.lowercase(Locale.getDefault())?.contains(chPrefix) == true
        }

        // 4. Exact amount match (e.g. 450 or 450.50)
        val numericAmount = token.toDoubleOrNull()
        if (numericAmount != null) {
            val paise = (numericAmount * 100).toLong()
            if (transaction.amount == paise) return true
        }

        // 5. Text field search across merchant, ref, upi, channel, category, note
        val merchantMatch =
            (transaction.merchantNormalised?.lowercase(Locale.getDefault())?.contains(token) == true) ||
                (transaction.merchantRaw?.lowercase(Locale.getDefault())?.contains(token) == true)
        val refMatch = transaction.refNumber?.lowercase(Locale.getDefault())?.contains(token) == true
        val upiMatch = transaction.upiId?.lowercase(Locale.getDefault())?.contains(token) == true
        val channelMatch = transaction.channel?.name?.lowercase(Locale.getDefault())?.contains(token) == true
        val catMatch = transaction.categorySource?.lowercase(Locale.getDefault())?.contains(token) == true
        val noteMatch = transaction.note?.lowercase(Locale.getDefault())?.contains(token) == true

        return merchantMatch || refMatch || upiMatch || channelMatch || catMatch || noteMatch
    }
}
