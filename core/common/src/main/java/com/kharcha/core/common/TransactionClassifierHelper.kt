package com.kharcha.core.common

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.TransactionType

object TransactionClassifierHelper {
    private val ACCOUNT_OR_CARD_NOISE_REGEX =
        Regex(
            "(?i)^(?:(?:(?:your|[A-Za-z]+)\\s+)?(?:[A-Za-z]+\\s+)*(?:credit\\s*card|debit\\s*card|card|cc)(?:\\s+ending|\\s+no\\.?|\\s+num|\\s*[*xX0-9]+|\\s*bil+)?.*|" +
                "(?:card|cc)\\s*[*xX0-9]+.*|" +
                "(?:a/c|acc|acct|account)\\s*[*xX0-9]+.*|" +
                "to\\s+(?:a/c|acc|acct|account)\\s*[*xX0-9]+.*|" +
                "[*xX]{1,12}\\d{3,4}.*|" +
                ".*(?:autopay|mandate|billpay|credit card payment|credit card bil).*|" +
                ".*(?:mobile number|secure digital banking|to know your balance|subject to clearing).*|" +
                ".*-[Uu][Pp][Ii].*|" +
                "(?:HDFC|ICICI|SBI|AXIS|KOTAK|IDFC|PNB|BOB|UCO|YES|AMEX|CITI|RBL)\\s+(?:CARD|UPI)|" +
                "your|transaction|dispute|clearing|making a successful transaction|the amount to be|your end|" +
                "an\\s+outstanding\\s+of.*|" +
                "(?:rs\\.?|inr\\.?|₹)\\s*[\\d,.]*|" +
                "(?:m/s\\.?|mr\\.?|mrs\\.?|ms\\.?|md\\.?|shri|smt)\\.?)$",
        )

    private val DATE_PREFIX_REGEX =
        Regex(
            """(?i)^(?:date\s+)?(?:\d{1,2}[A-Za-z]{3}\d{2,4}|\d{1,2}[-/][A-Za-z0-9]{3}[-/]\d{2,4}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\d{1,2}-[A-Za-z0-9]{3})\s+(?:transfer\s+to|trf\s+to|transfer\s+from|trf\s+from|transfer|trf|at|towards|for|to|from|by)\s*(.*)$""",
        )

    fun cleanMerchantDisplay(raw: String?, bank: String?, channel: String?): String {
        if (raw.isNullOrBlank()) {
            return if (!bank.isNullOrBlank()) "$bank ${channel ?: "Txn"}" else "Transaction"
        }
        val trimmed = raw.trim()

        val directAccountPattern =
            Regex("""(?i)^(?:to\s+)?(?:a/c|acc|acct|account)\s*[*xX]{1,12}(\d{3,4})$""")
        val directAccountMatch = directAccountPattern.find(trimmed)
        if (directAccountMatch != null) {
            return "Account ••${directAccountMatch.groupValues[1]}"
        }

        if (trimmed.matches(ACCOUNT_OR_CARD_NOISE_REGEX)) {
            return if (!bank.isNullOrBlank()) "$bank ${channel ?: "Card"}" else "Transaction"
        }

        var candidate = trimmed
        val dateMatch = DATE_PREFIX_REGEX.find(candidate)
        if (dateMatch != null && dateMatch.groupValues[1].isNotBlank()) {
            candidate = dateMatch.groupValues[1].trim()
        }

        val withoutHandle = candidate.substringBefore("@")
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
                """(?i)(?:towards|for|to|of)\s+(?:your\s+)?(?:[a-zA-Z]+\s+){0,3}(?:credit\s*card|card|cc)(?:\s+(?:account|a/c|acc))?\s*(?:ending(?:\s+with|\s+in)?|no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b""",
            )
        val match = pattern.find(body)
        if (match != null) return match.groupValues[1]

        val autopayPattern =
            Regex(
                """(?i)\b(?:credit\s*card|card|cc)(?:\s+(?:account|a/c|acc))?\s*(?:ending(?:\s+with|\s+in)?|no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b.*?(?:payment|bill|autodebit|auto-debit|autopay|paid)""",
            )
        val autopayMatch = autopayPattern.find(body)
        if (autopayMatch != null) return autopayMatch.groupValues[1]

        val fallbackPattern =
            Regex(
                """(?i)(?:credit\s*card|card)(?:\s+(?:account|a/c|acc))?\s*(?:ending(?:\s+with|\s+in)?|no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b""",
            )
        return fallbackPattern.find(body)?.groupValues?.get(1)
    }

    fun extractSourceAccountLast4(body: String? = null): String? {
        if (body.isNullOrBlank()) return null
        val pattern =
            Regex(
                """(?i)(?:from|through|via|in)\s+(?:[a-zA-Z]+\s+){0,3}(?:a/c|acc|acct|account)\s*(?:no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b""",
            )
        val match = pattern.find(body)
        if (match != null) return match.groupValues[1]

        val debitedPattern =
            Regex(
                """(?i)(?:a/c|acc|acct|account)\s*(?:no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b\s*(?:has\s+been|is|was)?\s*debited""",
            )
        val debitedMatch = debitedPattern.find(body)
        if (debitedMatch != null) return debitedMatch.groupValues[1]

        val debitedFromPattern =
            Regex(
                """(?i)debited\s+from\s+(?:a/c|acc|acct|account)?\s*[*xX.\s\-]*([0-9]{3,4})\b""",
            )
        return debitedFromPattern.find(body)?.groupValues?.get(1)
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
                m.contains("self transfer") || m.equals("transfer", ignoreCase = true)

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
            m.contains("wells fargo") || m == "wells" || m.contains("qualcomm") ||
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
                m.contains("annapurna") || m.contains("foods") -> "Food & Dining"

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
                m.contains("waterwala") || m.contains("auto debit") || m.contains("bill payment") ||
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

            m.contains("atm") || m.contains("cash wdl") || m.contains("cash withdrawal") ||
                m.contains("other") || m.contains("misc") -> "Other"

            else -> "Unknown Category"
        }
    }
}
