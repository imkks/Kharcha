package com.kharcha.engine.parser

import com.kharcha.core.model.Channel

object CardAccountExtractionHelper {

    private val MASKED_ACCOUNT_REGEX = Regex("""(?i)(?:[xX*]{1,12}|\b\d{2,9}[xX*]+)(\d{3,4})\b""")
    private val CARD_OR_ACC_ENDING_REGEX =
        Regex("""(?i)(?:card|cc|acct?|account)?\s*(?:ending(?:\s+with|\s+in)?)\s*[*xX.\s\-]*(\d{3,4})\b""")
    private val CARD_OR_ACC_NUM_REGEX =
        Regex("""(?i)\b(?:credit\s*card|debit\s*card|card|cc|a/c|acc|acct|account)\s*(?:no\.?|num(?:ber)?|[xX*.\s\-])*?\b(\d{3,4})\b""")
    private val EXPLICIT_ENDING_REGEX = Regex("""(?i)\b(?:ending|ending in|ending with)\s*[*xX.\s\-]*(\d{3,4})\b""")
    private val CONSECUTIVE_MASK_REGEX = Regex("""(?:\*{1,}|x{2,}|xx)(\d{3,4})""", RegexOption.IGNORE_CASE)

    private val DATE_PREFIX_REGEX =
        Regex(
            """(?i)^(?:date\s+)?(?:\d{1,2}[A-Za-z]{3}\d{2,4}|\d{1,2}[-/][A-Za-z0-9]{3}[-/]\d{2,4}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\d{1,2}-[A-Za-z0-9]{3})\s*(?:transfer\s+to|trf\s+to|transfer\s+from|trf\s+from|transfer|trf|at|towards|for|to|from|by)?\s*(.*)$""",
        )
    private val CHANNEL_PREFIX_REGEX =
        Regex("""(?i)^(?:by|via)\s+(?:[A-Za-z]+-UPI|UPI|NEFT|IMPS|RTGS|CARD|POS)\s*(.*)$""")
    private val CARD_PREFIX_REGEX =
        Regex(
            """(?i)^(?:(?:[A-Za-z0-9]+\s+){0,3}(?:bank\s+)?(?:credit\s*card|debit\s*card|card|cc))\s*(?:ending(?:\s+with|\s+in)?|[xX*.\s\-])*?(\d{3,4})\s+(?:at|towards|for|to|on)\s+(.+)$""",
        )

    private val BANK_ATM_REGEX =
        Regex("""(?i)^(SBI|HDFC|HDF|ICICI|ICI|AXIS|AXS|KOTAK|PNB|BOB|UCO|YES|CANARA|UNION|INDUSIND)\s+ATM\b.*""")
    private val CASH_WDL_REGEX =
        Regex("""(?i)\b(?:NFS\*CASH\s+WDL|CASH\s+WDL|CASH\s+WITHDRAWAL)\b.*""")
    private val ATM_KEYWORD_REGEX = Regex("""(?i)\bATM\b.*""")

    private val ACCOUNT_OR_CARD_NOISE_REGEX =
        Regex(
            "(?i)^(?:(?:(?:your|[A-Za-z]+)\\s+)?(?:[A-Za-z]+\\s+)*(?:credit\\s*card|debit\\s*card|card|cc)(?:\\s+ending|\\s+no\\.?|\\s+num|\\s*[*xX0-9]+|\\s*bil+)?.*|" +
                "(?:card|cc)\\s*[*xX0-9]+.*|" +
                ".*(?:autopay|mandate|billpay|credit card payment|credit card bil).*|" +
                ".*(?:mobile number|secure digital banking|to know your balance|subject to clearing).*|" +
                ".*-[Uu][Pp][Ii].*|" +
                "(?:HDFC|ICICI|SBI|AXIS|KOTAK|IDFC|PNB|BOB|UCO|YES|AMEX|CITI|RBL)\\s+(?:CARD|UPI)|" +
                "your|transaction|dispute|clearing|making a successful transaction|the amount to be|your end|" +
                "an\\s+outstanding\\s+of.*|" +
                "(?:rs\\.?|inr\\.?|₹)\\s*[\\d,.]*|" +
                "(?:m/s\\.?|mr\\.?|mrs\\.?|ms\\.?|md\\.?|shri|smt)\\.?)$",
        )

    /**
     * Extracts normalized last 3 or 4 digits from an SMS body, token, or account string.
     */
    fun extractLast4(text: String?): String? {
        if (text.isNullOrBlank()) return null

        val masked = MASKED_ACCOUNT_REGEX.find(text)
        if (masked != null) return masked.groupValues[1]

        val ending = CARD_OR_ACC_ENDING_REGEX.find(text)
        if (ending != null) return ending.groupValues[1]

        val cardNum = CARD_OR_ACC_NUM_REGEX.find(text)
        if (cardNum != null) return cardNum.groupValues[1]

        val explicitEnding = EXPLICIT_ENDING_REGEX.find(text)
        if (explicitEnding != null) return explicitEnding.groupValues[1]

        val consecutiveMask = CONSECUTIVE_MASK_REGEX.find(text)
        if (consecutiveMask != null) return consecutiveMask.groupValues[1]

        val digits = text.filter { it.isDigit() }
        return when {
            digits.length in 3..4 -> digits
            digits.length > 4 -> digits.takeLast(4)
            else -> null
        }
    }

    enum class AccountKind {
        CARD,
        BANK_ACCOUNT,
        UNKNOWN,
    }

    /**
     * Extracts bank account digits specifically associated with account terms (e.g. "a/c *1999", "Account XX2321", "Acct XX321").
     */
    fun extractBankAccountLast4(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val patterns =
            listOf(
                Regex("""(?i)(?:from|through|via|in|debited\s+from|deposited\s+in)\s+(?:[a-zA-Z]+\s+){0,3}(?:a/c|acc|acct|account)\s*(?:no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b"""),
                Regex("""(?i)\b(?:a/c|acc|acct|account)\s*(?:no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b\s*(?:has\s+been|is|was)?\s*debited"""),
                Regex("""(?i)\b(?:a/c|acc|acct|account)\s*(?:no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b"""),
                Regex("""(?i)debited\s+from\s+(?:a/c|acc|acct|account)?\s*[*xX.\s\-]*([0-9]{3,4})\b"""),
            )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    /**
     * Extracts card digits specifically associated with card terms (e.g. "Credit Card XX5625", "Card ending 8302", "Credit Card Account XX1268").
     */
    fun extractCardLast4(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val patterns =
            listOf(
                Regex("""(?i)(?:credit\s*card|debit\s*card|card|cc)(?:\s+(?:account|a/c|acc))?\s*(?:ending(?:\s+with|\s+in)?|no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b"""),
                Regex("""(?i)(?:towards|for|to|of|on|using|from)\s+(?:your\s+)?(?:[a-zA-Z]+\s+){0,3}(?:credit\s*card|card|cc)(?:\s+(?:account|a/c|acc))?\s*(?:ending(?:\s+with|\s+in)?|no\.?|number)?\s*[*xX.\s\-]*(?:[0-9]{2,9}[*xX]+)?([0-9]{3,4})\b"""),
                Regex("""(?i)\b(?:cardmember|card)\b.*?\b(?:ending(?:\s+with|\s+in)?|ending\s+with|ending)\s*[*xX.\s\-]*([0-9]{3,4})\b"""),
            )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    /**
     * Detects whether the primary account used in the transaction is a card or a bank account.
     */
    fun detectAccountKind(body: String?, channel: Channel? = null): AccountKind {
        if (body.isNullOrBlank()) return if (channel == Channel.CARD) AccountKind.CARD else AccountKind.UNKNOWN
        val b = body.lowercase()

        val bankAccLast4 = extractBankAccountLast4(body)
        val cardLast4 = extractCardLast4(body)

        val isExplicitBankDebit =
            b.contains("debited from a/c") || b.contains("debited from account") ||
                b.contains("through auto debit from account") || b.contains("auto debit from account") ||
                (b.contains("acct ") && b.contains("debited")) ||
                (b.contains("deposited in") && b.contains("a/c"))

        if (isExplicitBankDebit && bankAccLast4 != null) {
            return AccountKind.BANK_ACCOUNT
        }

        val hasCardKeywords =
            b.contains("credit card") || b.contains("card ending") ||
                b.contains("cardmember") || b.contains("spent on card") ||
                (b.contains("paid using") && b.contains("card")) ||
                b.contains("debited from credit card") || b.contains("debited from card") ||
                b.contains("available limit") || b.contains("credit limit") ||
                (b.contains("card") && cardLast4 != null)

        if (hasCardKeywords && cardLast4 != null) {
            return AccountKind.CARD
        }

        if (channel == Channel.CARD) {
            return AccountKind.CARD
        }

        if (bankAccLast4 != null || b.contains("a/c") || b.contains("account")) {
            return AccountKind.BANK_ACCOUNT
        }

        return AccountKind.UNKNOWN
    }

    private val NEFT_NARRATIVE_REGEX =
        Regex(
            """(?i)\b(?:for\s+|by\s+)?(?:NEFT|RTGS|IMPS)(?:\s+(?:Cr|Credit))?[-/: ]+([A-Za-z0-9.\-_/&* ]+?)(?:(?:\.|\s+)Avl|\s+bal|\s+balance|\s+ref|[.,;]|$)""",
        )

    /**
     * Extracts sender/company name and UTR reference number from Indian banking NEFT/RTGS/IMPS credit narratives.
     * e.g. "NEFT Cr-BOFA0CN6215-QUALCOMM INDIA PVT LTD-KUMAR SINGH KRISHNA-BOFAH26272092252"
     * -> sender = "QUALCOMM INDIA PVT LTD", ref = "BOFAH26272092252"
     * e.g. "NEFT-HDFCH00825101407-WELLS" -> sender = "WELLS", ref = "HDFCH00825101407"
     */
    fun extractNeftSenderAndRef(text: String): Pair<String?, String?> {
        val match = NEFT_NARRATIVE_REGEX.find(text) ?: return Pair(null, null)
        val fullCr = match.groupValues[1].trim()
        val segments = fullCr.split(Regex("[-/]")).map { it.trim() }.filter { it.isNotBlank() }
        if (segments.isEmpty()) return Pair(null, null)

        val isCodeOrIfscOrUtr = { s: String ->
            s.matches(Regex("""^[A-Za-z]{4}0[A-Za-z0-9]{6}$""")) ||
                (s.matches(Regex("""^[A-Za-z0-9]{8,22}$""")) && s.any { it.isDigit() } && !s.contains(" "))
        }

        val sender: String?
        val ref: String?

        if (segments.size == 2) {
            if (isCodeOrIfscOrUtr(segments[0]) && !isCodeOrIfscOrUtr(segments[1])) {
                ref = segments[0]
                sender = segments[1]
            } else if (isCodeOrIfscOrUtr(segments[1]) && !isCodeOrIfscOrUtr(segments[0])) {
                sender = segments[0]
                ref = segments[1]
            } else {
                sender = segments[0]
                ref = null
            }
        } else if (segments.size >= 3) {
            sender = if (isCodeOrIfscOrUtr(segments[0])) segments[1] else segments[0]
            ref = if (isCodeOrIfscOrUtr(segments.last())) segments.last() else null
        } else {
            sender = if (!isCodeOrIfscOrUtr(segments[0])) segments[0] else null
            ref = if (isCodeOrIfscOrUtr(segments[0])) segments[0] else null
        }

        return Pair(sender, ref)
    }

    /**
     * Cleans ATM withdrawal narrative into a human-readable merchant name.
     */
    fun cleanAtmMerchant(text: String?, bankName: String? = null): String? {
        if (text.isNullOrBlank()) return null
        val stripped = text.trim()
            .replace(
                Regex("""(?i)^(?:date\s+)?(?:\d{1,2}[A-Za-z]{3}\d{2,4}|\d{1,2}[-/][A-Za-z0-9]{3}[-/]\d{2,4}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4})\s+"""),
                "",
            )
            .trim()
        val bankMatch = BANK_ATM_REGEX.find(stripped)
        if (bankMatch != null) {
            val rawBank = bankMatch.groupValues[1].uppercase()
            val normalizedBank = when (rawBank) {
                "HDF" -> "HDFC"
                "ICI" -> "ICICI"
                "AXS" -> "AXIS"
                else -> rawBank
            }
            return "$normalizedBank ATM"
        }
        if (CASH_WDL_REGEX.containsMatchIn(stripped)) {
            return "Cash Withdrawal"
        }
        if (ATM_KEYWORD_REGEX.containsMatchIn(stripped)) {
            return if (!bankName.isNullOrBlank()) "$bankName ATM" else "ATM Withdrawal"
        }
        return null
    }

    /**
     * Cleans a candidate merchant string by stripping leading date phrases and card prefixes,
     * and rejecting strings that are purely account numbers, cards, or system noise.
     */
    fun cleanCandidateMerchant(
        candidate: String?,
        onCardExtracted: (cardLast4: String) -> Unit = {},
    ): String? {
        if (candidate.isNullOrBlank()) return null
        var cleaned = candidate.trim().trim('.', ',', '-', ':', ';')

        // 1. Strip date prefix (e.g. "04-Oct-26 at GOOGLE STORE" -> "GOOGLE STORE",
        //    "date 21Feb24 trf to COUNTRYDELIGHT" -> "COUNTRYDELIGHT")
        val dateMatch = DATE_PREFIX_REGEX.find(cleaned)
        if (dateMatch != null) {
            cleaned = dateMatch.groupValues[1].trim()
        }

        // 2. Strip Channel prefix (e.g. "by UCO-UPI" -> "")
        val channelMatch = CHANNEL_PREFIX_REGEX.find(cleaned)
        if (channelMatch != null) {
            cleaned = channelMatch.groupValues[1].trim()
        }

        // 3. Strip Card prefix (e.g. "HDFC Bank Card 8302 At ASSPL" -> "ASSPL")
        val cardMatch = CARD_PREFIX_REGEX.find(cleaned)
        if (cardMatch != null) {
            val cardLast4 = cardMatch.groupValues[1]
            val realMerch = cardMatch.groupValues[2].trim()
            onCardExtracted(cardLast4)
            cleaned = realMerch
        }

        // 4. Clean ATM strings
        if (BANK_ATM_REGEX.containsMatchIn(cleaned) || CASH_WDL_REGEX.containsMatchIn(cleaned)) {
            cleaned = cleanAtmMerchant(cleaned) ?: cleaned
        }

        // 5. Clean HDFC info narratives
        cleaned = when {
            cleaned.contains("InfoATD*", ignoreCase = true) || cleaned.contains("Auto Debi", ignoreCase = true) -> "Auto Debit"
            cleaned.contains("InfoTrf to PPF", ignoreCase = true) -> "PPF Transfer"
            cleaned.contains("InfoBIL*", ignoreCase = true) -> "Bill Payment"
            cleaned.contains("InfoCLG*", ignoreCase = true) -> "Cheque Clearing"
            else -> cleaned
        }

        // 6. Reject noise
        if (isAccountOrCardOrSystemNoise(cleaned)) {
            return null
        }

        return if (cleaned.length >= 2) cleaned else null
    }

    /**
     * Returns true if the string represents an account/card indicator or bank noise rather than a merchant.
     */
    fun isAccountOrCardOrSystemNoise(text: String?): Boolean {
        if (text.isNullOrBlank()) return true
        val trimmed = text.trim()
        return trimmed.matches(ACCOUNT_OR_CARD_NOISE_REGEX)
    }

    /**
     * Extracts known bank name from sender ID or body text.
     */
    fun extractBankName(sender: String, body: String): String? {
        val s = sender.uppercase()
        val b = body.uppercase()
        return when {
            s.contains("HDFC") || b.contains("HDFC") -> "HDFC"
            s.contains("ICICI") || b.contains("ICICI") -> "ICICI"
            s.contains("SBI") || b.contains("SBI") -> "SBI"
            s.contains("AXIS") || b.contains("AXIS") -> "AXIS"
            s.contains("KOTAK") || b.contains("KOTAK") -> "KOTAK"
            s.contains("PNB") || b.contains("PNB") -> "PNB"
            s.contains("BOB") || b.contains("BOB") || b.contains("BARODA") -> "BOB"
            s.contains("IDFC") || b.contains("IDFC") -> "IDFC"
            s.contains("INDUS") || b.contains("INDUS") -> "INDUSIND"
            s.contains("YES") || b.contains("YES BANK") -> "YES"
            s.contains("AMEX") || b.contains("AMERICAN EXPRESS") || b.contains("AMEX") -> "AMEX"
            s.contains("CITI") || b.contains("CITI") -> "CITI"
            s.contains("RBL") || b.contains("RBL") -> "RBL"
            s.contains("SCB") || b.contains("STANDARD CHARTERED") -> "SCB"
            s.contains("FEDERAL") || b.contains("FEDERAL") -> "FEDERAL"
            s.contains("CANARA") || b.contains("CANARA") -> "CANARA"
            s.contains("UNION") || b.contains("UNION BANK") -> "UNION"
            s.contains("UCO") || b.contains("UCO BANK") -> "UCO"
            s.contains("HSBC") || b.contains("HSBC") -> "HSBC"
            s.contains("SLICE") || b.contains("SLICE") -> "SLICE"
            s.contains("ONECARD") || b.contains("ONECARD") -> "ONECARD"
            s.contains("JUPITER") || b.contains("JUPITER") -> "JUPITER"
            s.contains("FI") || b.contains("FI MONEY") -> "FI"
            s.contains("PAYTM") || b.contains("PAYTM") -> "PAYTM"
            s.contains("AIRBNK") || s.contains("AIRTELPB") || s.contains("APBLTD") ||
                b.contains("AIRTEL PAYMENTS BANK") || b.contains("AIRTEL BANK") || b.contains("AIRTEL MONEY") ||
                (s.contains("AIRTEL") && (b.contains("PAYMENTS BANK") || b.contains("SAVINGS") || b.contains("WALLET A/C"))) -> "AIRTEL"
            else -> null
        }
    }
}
