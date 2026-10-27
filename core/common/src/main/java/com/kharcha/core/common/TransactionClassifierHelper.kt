package com.kharcha.core.common

import com.kharcha.core.model.Channel
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

    fun isCreditCardBillPayment(body: String? = null, merchant: String? = null): Boolean {
        if (body.isNullOrBlank() && merchant.isNullOrBlank()) {
            return false
        }
        val b = body?.lowercase() ?: ""
        val m = merchant?.lowercase() ?: ""

        if (b.contains("spent") || b.contains("used for a transaction") ||
            b.contains("used at") || b.contains("used your") || b.contains("made on")
        ) {
            return false
        }

        val cardPaymentPattern =
            Regex(
                """(?:payment|received|credited|thank you for payment)\b.*(?:towards|for|to|in)\b.*""" +
                    """(?:credit\s+card|card|cc)\b|""" +
                    """\btowards\s+(?:your\s+)?(?:[a-zA-Z]+\s+)*(?:credit\s+card|card|cc)\b|""" +
                    """\b(?:credit\s+card|card|cc)\s+(?:bill|payment)\b|""" +
                    """\b(?:cred|cheq|cred club|cred\.club)\b|cred@|cheq@""",
                RegexOption.IGNORE_CASE,
            )

        val cardPhrases =
            listOf(
                "towards card", "to card ending",
                "towards credit card", "credit card payment", "cc payment", "cc bill",
                "card bill", "credit card bill", "payment to card", "payment for card",
                "payment for your card", "payment towards your card", "payment towards your credit card",
                "payment received for card", "payment received towards your credit card",
                "received towards your credit card", "received towards your card",
                "received towards credit card", "credited to your credit card",
                "credited to card ending", "in your credit card account",
                "towards sbi card", "towards hdfc card", "towards icici card",
                "towards axis card", "towards onecard", "towards rbl card",
                "towards kotak card", "sbi card payment", "hdfc card payment", "icici card payment",
                "received towards", "payment received towards",
                "paid towards card", "paid to card", "paid to credit card",
                "bill paid for card", "card bill paid", "cc bill paid",
                "auto debit towards card", "autodebit towards card",
            )

        val hasCardPhrase =
            (cardPhrases.any { b.contains(it) || m.contains(it) } && (b.contains("card") || b.contains("cc"))) ||
                cardPaymentPattern.containsMatchIn(b) || cardPaymentPattern.containsMatchIn(m)

        val isAutoDebitForCard =
            (b.contains("auto debit") || b.contains("autodebit") || b.contains("auto-debit") ||
                b.contains("standing instruction") || b.contains("nach") || b.contains("e-mandate")) &&
                (b.contains("card") || b.contains("cc") || b.contains("credit"))

        return hasCardPhrase || isAutoDebitForCard
    }

    fun isAutoDebit(body: String? = null): Boolean {
        if (body.isNullOrBlank()) return false
        val b = body.lowercase()
        return b.contains("auto debit") || b.contains("autodebit") || b.contains("auto-debit") ||
            b.contains("standing instruction") || b.contains("nach") || b.contains("e-mandate")
    }

    fun extractTargetCardLast4(body: String? = null): String? {
        if (body.isNullOrBlank()) return null
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

    fun extractSourceAccountLast4(body: String? = null): String? {
        if (body.isNullOrBlank()) return null
        val pattern =
            Regex(
                """(?:from\s+)?(?:a/c|acct|account)\s*(?:no\.?|number)?\s*(?:xx|x|\*)*(\d{4})""",
                RegexOption.IGNORE_CASE,
            )
        return pattern.find(body)?.groupValues?.get(1)
    }

    fun isTransferNarrative(
        body: String? = null,
        merchant: String? = null,
        amountMinor: Long = 0L,
        channel: Channel? = null,
    ): Boolean {
        if (body.isNullOrBlank() && merchant.isNullOrBlank()) return false
        val b = body?.lowercase() ?: ""
        val m = merchant?.lowercase() ?: ""

        // 1. Explicit self transfer keywords
        val isSelf =
            b.contains("self transfer") || b.contains("transfer to self") ||
                b.contains("own account") || b.contains("own a/c") ||
                b.contains("transfer between accounts") ||
                b.contains("upi funds transfer") ||
                m.contains("self transfer") || m.equals("transfer", ignoreCase = true) ||
                m.contains("krishnasingh") || m.contains("imkks")

        // 2. Inter-account transfer pattern (e.g. "debited from a/c *1999 ... to a/c **0034")
        val isInterAccount =
            Regex(
                "(?i)(?:debited\\s+from\\s+a/c|from\\s+[a-zA-Z]+\\s+bank\\s+a/c).*?\\bto\\s+a/c\\b",
            ).containsMatchIn(b)

        // 3. Investments (Mutual Funds, Zerodha, Groww, Upstox, etc.)
        val isInvestment =
            m.contains("mutual fund") || m.contains("zerodha") || m.contains("groww") ||
                m.contains("upstox") || m.contains("indmoney") ||
                b.contains("mutual funds") || b.contains("zerodha") || b.contains("equity a/c")

        // 4. Credit card bill payments & auto debits
        val isCardBill = isCreditCardBillPayment(body, merchant)
        val isGeneralAutoDebit =
            isAutoDebit(body) && (b.contains("card") || b.contains("loan") || b.contains("bill") || b.contains("emi"))

        // 5. Tax Challan payment
        val isTaxChallan = b.contains("challan payment")

        // 6. High-value transfer threshold (default >= Rs 20,000 for non-card debits)
        val isHighValueTransfer = amountMinor >= 20_000_00L &&
            (channel == Channel.UPI || channel == Channel.NEFT ||
                channel == Channel.IMPS || channel == Channel.OTHER) &&
            !isRetailMerchant(m)

        return isSelf || isInterAccount || isInvestment || isCardBill || isGeneralAutoDebit ||
            isTaxChallan || isHighValueTransfer
    }

    private fun isRetailMerchant(m: String): Boolean {
        val retailKeywords =
            listOf(
                "amazon", "flipkart", "myntra", "swiggy", "zomato", "blinkit", "zepto",
                "decathlon", "air india", "indigo", "makemytrip", "cleartrip", "bookmyshow",
                "netflix", "apple", "google store", "uber", "ola", "apollo", "vijetha",
                "urban company", "urbanclap",
            )
        return retailKeywords.any { m.contains(it) }
    }

    fun isRefundNarrative(body: String? = null, merchant: String? = null): Boolean {
        if (body.isNullOrBlank() && merchant.isNullOrBlank()) return false
        val b = body?.lowercase() ?: ""
        val m = merchant?.lowercase() ?: ""
        val refundPhrases =
            listOf(
                "refund", "refunded", "reversal", "reversed",
                "credited back", "initiated for refund", "refund of",
                "cashback", "cash back", "returned", "chargeback",
            )
        return refundPhrases.any { b.contains(it) || m.contains(it) }
    }

    fun classifyType(
        direction: Direction,
        body: String,
        merchant: String? = null,
        amountMinor: Long = 0L,
        channel: Channel? = null,
    ): TransactionType {
        if (isTransferNarrative(body, merchant, amountMinor, channel)) {
            return TransactionType.Transfer
        }
        if (isRefundNarrative(body, merchant)) {
            return TransactionType.Refund
        }
        return if (direction == Direction.CREDIT) TransactionType.Income else TransactionType.Expense
    }

    fun guessCategory(
        merchant: String,
        isCredit: Boolean,
        isTransfer: Boolean = false,
        isRefund: Boolean = false,
        body: String? = null,
        userRules: Map<String, String> = emptyMap(),
    ): String {
        if (isTransfer) return "Transfer"
        if (isRefund) return "Refund"
        val m = merchant.trim().lowercase()
        val b = body?.lowercase() ?: ""

        val exactMatch = userRules[m]
        if (exactMatch != null) return exactMatch

        val partialMatch =
            userRules.entries.firstOrNull { (ruleKey, _) ->
                ruleKey.isNotBlank() && (m.contains(ruleKey) || ruleKey.contains(m))
            }?.value
        if (partialMatch != null) return partialMatch

        if (isCredit) {
            return guessInwardCategory(m, b)
        }
        return guessExpenseCategory(m)
    }

    private fun guessInwardCategory(m: String, b: String): String {
        if (isRefundNarrative(b, m)) {
            return "Refund"
        }
        if (b.contains("salary") || b.contains("payroll") || b.contains("stipend") ||
            b.contains("wages") || b.contains("epfo") ||
            (b.contains("clearing") && !b.contains("zerodha"))
        ) {
            return "Salary"
        }
        if (b.contains("interest") || b.contains("int.pd") || m.contains("interest")) {
            return "Interest"
        }
        if (m.contains("zerodha") || m.contains("groww") || b.contains("zerodha") ||
            b.contains("settlement payout") || b.contains("dividend")
        ) {
            return "Investment"
        }
        if (isCreditCardBillPayment(b, m) || b.contains("bill payment") || b.contains("card bil")) {
            return "Bill Payment"
        }
        if (m.contains("reward") || m.contains("cashback") || m.contains("cliq cash") ||
            b.contains("cashback") || b.contains("cash back") || b.contains("reward")
        ) {
            return "Rewards"
        }
        if (m.contains("@") || b.contains("upi") || b.contains("credit alert") || b.contains("transfer")) {
            return "A/C Transfer"
        }
        return "Credit"
    }

    private fun guessExpenseCategory(m: String): String {
        return when {
            m.contains("swiggy") || m.contains("zomato") || m.contains("mcdonald") ||
                m.contains("kfc") || m.contains("domino") || m.contains("pizza") ||
                m.contains("starbucks") || m.contains("coffee") || m.contains("chai") ||
                m.contains("burger") || m.contains("subway") || m.contains("restaurant") ||
                m.contains("hungerbox") || m.contains("hunger box") || m.contains("eatclub") ||
                m.contains("district dining") || m.contains("nandini") ||
                m.contains("annapurna") || m.contains("dakshith") || m.contains("foods") -> "Food & Dining"

            m.contains("blinkit") || m.contains("zepto") || m.contains("instamart") ||
                m.contains("bigbasket") || m.contains("dmart") || m.contains("grocery") ||
                m.contains("supermarket") || m.contains("reliance fresh") ||
                m.contains("vijetha") -> "Groceries"

            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
                m.contains("ajio") || m.contains("meesho") || m.contains("tata cliq") ||
                m.contains("retail") || m.contains("store") || m.contains("decathlon") -> "Shopping"

            m.contains("petrol") || m.contains("fuel") || m.contains("hpcl") ||
                m.contains("bpcl") || m.contains("iocl") || m.contains("pump") -> "Fuel"

            m.contains("uber") || m.contains("ola") || m.contains("rapido") ||
                m.contains("metro") || m.contains("irctc") || m.contains("railway") ||
                m.contains("telangana state road") || m.contains("tsrtc") ||
                m.contains("air india") || m.contains("indigo") || m.contains("travel") -> "Travel"

            m.contains("airtel") || m.contains("jio") || m.contains("vi ") ||
                m.contains("bescom") || m.contains("electricity") || m.contains("water") ||
                m.contains("gas") || m.contains("broadband") || m.contains("bill") ||
                m.contains("urban company") || m.contains("urbanclap") ||
                m.contains("mygate") -> "Bills & Utilities"

            m.contains("seva") || m.contains("donation") || m.contains("foundation") ||
                m.contains("charity") -> "Donation"

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
