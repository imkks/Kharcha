package com.kharcha.core.common

import com.kharcha.core.model.Direction
import com.kharcha.core.model.TransactionType

object TransactionClassifierHelper {
    fun cleanMerchantDisplay(raw: String?, bank: String?, channel: String?): String {
        if (raw.isNullOrBlank()) {
            return if (!bank.isNullOrBlank()) "$bank ${channel ?: "Txn"}" else "Transaction"
        }
        val withoutHandle = raw.substringBefore("@")
        val cleaned = withoutHandle.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered =
            words.filter { word ->
                word.uppercase() !in setOf("PVT", "LTD", "LIMITED", "PRIVATE", "INC", "CORP", "VPA", "UPI", "POS")
            }
        val finalWords = if (filtered.isNotEmpty()) filtered else words
        return finalWords.take(4).joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun isCreditCardBillPayment(body: String, merchant: String? = null): Boolean {
        val b = body.lowercase()
        val m = merchant?.lowercase() ?: ""

        val cardPhrases =
            listOf(
                "credit card", "card payment", "towards card", "to card ending",
                "towards credit card", "credit card payment", "cc payment", "cc bill",
                "card bill", "credit card bill", "payment to card", "payment for card",
                "payment for your card", "payment towards your card", "payment towards your credit card",
                "payment received for card", "payment received towards your credit card",
                "received towards your credit card", "received towards your card",
                "received towards credit card", "credited to your credit card",
                "credited to card ending", "in your credit card account",
                "towards sbi card", "towards hdfc card", "towards icici card",
                "towards axis card", "towards onecard", "towards rbl card",
                "towards kotak card", "sbi card payment", "hdfc card payment",
            )

        val appPhrases = listOf("cred", "cheq", "cred club", "cred.club", "cred@axis", "cred@icici")

        val hasCardPhrase = cardPhrases.any { b.contains(it) || m.contains(it) }
        val hasAppPhrase = appPhrases.any { b.contains(it) || m.contains(it) }

        val isAutoDebitForCard =
            (b.contains("auto debit") || b.contains("autodebit") || b.contains("auto-debit") ||
                b.contains("standing instruction") || b.contains("nach") || b.contains("e-mandate")) &&
                (b.contains("card") || b.contains("cc") || b.contains("credit"))

        return hasCardPhrase || hasAppPhrase || isAutoDebitForCard
    }

    fun isAutoDebit(body: String): Boolean {
        val b = body.lowercase()
        return b.contains("auto debit") || b.contains("autodebit") || b.contains("auto-debit") ||
            b.contains("standing instruction") || b.contains("nach") || b.contains("e-mandate")
    }

    fun extractTargetCardLast4(body: String): String? {
        val pattern =
            Regex(
                """(?:towards|for|to)\s+(?:your\s+)?(?:[a-zA-Z]+\s+)?""" +
                    """(?:credit\s+card|card|cc)\s*(?:ending|no\.?|number|ending with|ending in)?""" +
                    """\s*(?:xx|x|\*)*(\d{4})""",
                RegexOption.IGNORE_CASE,
            )
        val match = pattern.find(body)
        if (match != null) return match.groupValues[1]

        val fallbackPattern =
            Regex(
                """(?:credit\s+card|card)\s*(?:ending|no\.?|number|ending with|ending in)?\s*(?:xx|x|\*)*(\d{4})""",
                RegexOption.IGNORE_CASE,
            )
        return fallbackPattern.find(body)?.groupValues?.get(1)
    }

    fun extractSourceAccountLast4(body: String): String? {
        val pattern =
            Regex(
                """(?:from\s+)?(?:a/c|acct|account)\s*(?:no\.?|number)?\s*(?:xx|x|\*)*(\d{4})""",
                RegexOption.IGNORE_CASE,
            )
        return pattern.find(body)?.groupValues?.get(1)
    }

    fun isTransferNarrative(body: String, merchant: String? = null): Boolean {
        val b = body.lowercase()
        val m = merchant?.lowercase() ?: ""
        val isSelf =
            b.contains("self transfer") || b.contains("transfer to self") ||
                b.contains("own account") || b.contains("own a/c") ||
                b.contains("transfer between accounts") ||
                m.contains("self transfer") || m.equals("transfer", ignoreCase = true)

        val isCardBill = isCreditCardBillPayment(body, merchant)

        val isGeneralAutoDebit =
            isAutoDebit(body) && (b.contains("card") || b.contains("loan") || b.contains("bill") || b.contains("emi"))

        return isSelf || isCardBill || isGeneralAutoDebit
    }

    fun classifyType(direction: Direction, body: String, merchant: String? = null): TransactionType {
        if (isTransferNarrative(body, merchant)) {
            return TransactionType.Transfer
        }
        return if (direction == Direction.CREDIT) TransactionType.Income else TransactionType.Expense
    }

    fun guessCategory(
        merchant: String,
        isCredit: Boolean,
        isTransfer: Boolean = false,
        userRules: Map<String, String> = emptyMap(),
    ): String {
        if (isTransfer) return "Transfer"
        val m = merchant.trim().lowercase()

        val exactMatch = userRules[m]
        if (exactMatch != null) return exactMatch

        val partialMatch =
            userRules.entries.firstOrNull { (ruleKey, _) ->
                ruleKey.isNotBlank() && (m.contains(ruleKey) || ruleKey.contains(m))
            }?.value
        if (partialMatch != null) return partialMatch

        if (isCredit) return "Salary"
        return when {
            m.contains("swiggy") || m.contains("zomato") || m.contains("mcdonald") ||
                m.contains("kfc") || m.contains("domino") || m.contains("pizza") ||
                m.contains("starbucks") || m.contains("coffee") || m.contains("chai") ||
                m.contains("burger") || m.contains("subway") || m.contains("restaurant") -> "Food & Dining"
            m.contains("blinkit") || m.contains("zepto") || m.contains("instamart") ||
                m.contains("bigbasket") || m.contains("dmart") || m.contains("grocery") ||
                m.contains("supermarket") || m.contains("reliance fresh") -> "Groceries"
            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
                m.contains("ajio") || m.contains("meesho") || m.contains("tata cliq") ||
                m.contains("retail") || m.contains("store") -> "Shopping"
            m.contains("uber") || m.contains("ola") || m.contains("rapido") ||
                m.contains("metro") || m.contains("irctc") || m.contains("petrol") ||
                m.contains("fuel") || m.contains("hpcl") || m.contains("bpcl") ||
                m.contains("ioc") || m.contains("railway") -> "Transport"
            m.contains("airtel") || m.contains("jio") || m.contains("vi ") ||
                m.contains("bescom") || m.contains("electricity") || m.contains("water") ||
                m.contains("gas") || m.contains("broadband") || m.contains("bill") -> "Bills & Utilities"
            m.contains("netflix") || m.contains("spotify") || m.contains("prime") ||
                m.contains("hotstar") || m.contains("bookmyshow") || m.contains("cinema") ||
                m.contains("pvr") || m.contains("inox") -> "Entertainment"
            m.contains("apollo") || m.contains("pharmacy") || m.contains("1mg") ||
                m.contains("pharmeasy") || m.contains("hospital") || m.contains("clinic") ||
                m.contains("medplus") -> "Health"
            m.contains("zerodha") || m.contains("groww") || m.contains("mutual") ||
                m.contains("sip") || m.contains("indmoney") || m.contains("upstox") -> "Investments"
            else -> "Other"
        }
    }
}
