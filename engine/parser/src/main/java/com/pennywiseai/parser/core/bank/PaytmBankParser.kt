package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType

/**
 * Parser for Paytm Payments Bank & Wallet SMS messages.
 */
class PaytmBankParser : BaseIndianBankParser() {

    override fun getBankName() = "Paytm Payments Bank"

    override fun isMobileWallet() = true

    override fun canHandle(sender: String): Boolean {
        val upper = sender.uppercase()
        return upper.contains("PAYTM") ||
            upper.matches(Regex("""^[A-Z]{2}-PAYTM.*$"""))
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (isNonTransactionMessage(message)) return false
        val keywords = listOf(
            "paid", "debited", "credited", "received", "sent",
            "transferred", "added", "spent", "refund"
        )
        return keywords.any { lower.contains(it) }
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("paid") || lower.contains("sent") || lower.contains("debited") -> TransactionType.EXPENSE
            lower.contains("credited") || lower.contains("received") || lower.contains("refund") -> TransactionType.INCOME
            else -> super.extractTransactionType(message)
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val pattern = Regex("""(?:to|paid\s+to)\s+([A-Za-z0-9\s._&-]+?)\s+from\s+""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            val candidate = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(candidate)) return candidate
        }
        return super.extractMerchant(message, sender)
    }
}
