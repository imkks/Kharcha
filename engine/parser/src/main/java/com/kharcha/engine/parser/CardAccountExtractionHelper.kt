package com.kharcha.engine.parser

import com.kharcha.core.model.Channel

object CardAccountExtractionHelper {

    private val MASKED_ACCOUNT_REGEX = Regex("""\b(?:\d{2,9})?[xX*]{1,12}(\d{3,4})\b""")
    private val CARD_OR_ACC_ENDING_REGEX =
        Regex("""(?i)(?:card|cc|acct?|account)?\s*(?:ending(?:\s+with|\s+in)?)\s*[*xX.\s\-]*(\d{3,4})\b""")
    private val CARD_OR_ACC_NUM_REGEX =
        Regex("""(?i)\b(?:credit\s*card|debit\s*card|card|cc|a/c|acc|acct|account)\s*(?:no\.?|num(?:ber)?|[xX*.\s\-])*?\b(\d{3,4})\b""")
    private val EXPLICIT_ENDING_REGEX = Regex("""(?i)\b(?:ending|ending in|ending with)\s*[*xX.\s\-]*(\d{3,4})\b""")
    private val CONSECUTIVE_MASK_REGEX = Regex("""(?:\*{2,}|x{2,}|xx)(\d{3,4})""", RegexOption.IGNORE_CASE)

    private val DATE_AT_REGEX = Regex("""(?i)^\d{1,2}-[A-Za-z0-9]{3}(?:-\d{2,4})?\s+at\s+(.+)$""")
    private val CARD_PREFIX_REGEX =
        Regex(
            """(?i)^(?:(?:[A-Za-z0-9]+\s+){0,3}(?:bank\s+)?(?:credit\s*card|debit\s*card|card|cc))\s*(?:ending(?:\s+with|\s+in)?|[xX*.\s\-])*?(\d{3,4})\s+(?:at|towards|for|to|on)\s+(.+)$""",
        )

    private val ACCOUNT_OR_CARD_NOISE_REGEX =
        Regex(
            "(?i)^(?:(?:(?:your|[A-Za-z]+)\\s+)?(?:[A-Za-z]+\\s+)*(?:credit\\s*card|debit\\s*card|card|cc)(?:\\s+ending|\\s+no\\.?|\\s+num|\\s*[*xX0-9]+|\\s*bil+)?.*|" +
                "(?:card|cc)\\s*[*xX0-9]+.*|" +
                ".*(?:autopay|mandate|billpay|credit card payment|credit card bil).*|" +
                ".*(?:mobile number|secure digital banking).*|" +
                "(?:HDFC|ICICI|SBI|AXIS|KOTAK|IDFC|PNB|BOB|UCO|YES|AMEX|CITI|RBL)\\s+CARD)$",
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

    private val NEFT_CR_REGEX =
        Regex(
            """(?i)\b(?:for\s+)?(?:NEFT|RTGS|IMPS)\s+(?:Cr|Credit)[-/: ]+([A-Za-z0-9.\-_/&* ]+?)(?:(?:\.|\s+)Avl|\s+bal|\s+balance|\s+ref|[.,;]|$)""",
        )

    /**
     * Extracts sender/company name and UTR reference number from Indian banking NEFT/RTGS/IMPS credit narratives.
     * e.g. "NEFT Cr-BOFA0CN6215-QUALCOMM INDIA PVT LTD-KUMAR SINGH KRISHNA-BOFAH26272092252"
     * -> sender = "QUALCOMM INDIA PVT LTD", ref = "BOFAH26272092252"
     */
    fun extractNeftSenderAndRef(text: String): Pair<String?, String?> {
        val match = NEFT_CR_REGEX.find(text) ?: return Pair(null, null)
        val fullCr = match.groupValues[1].trim()
        val segments = fullCr.split(Regex("[-/]")).map { it.trim() }.filter { it.isNotBlank() }
        if (segments.isEmpty()) return Pair(null, null)

        val isCodeOrIfsc = { s: String ->
            s.matches(Regex("""^[A-Za-z]{4}0[A-Za-z0-9]{6}$""")) ||
                (s.matches(Regex("""^[A-Za-z0-9]{8,18}$""")) && s.any { it.isDigit() } && s.any { it.isLetter() } && !s.contains(" "))
        }

        val sender =
            when {
                segments.size >= 2 && isCodeOrIfsc(segments[0]) -> segments[1]
                segments.isNotEmpty() && !isCodeOrIfsc(segments[0]) -> segments[0]
                segments.size >= 2 -> segments[1]
                else -> segments[0]
            }

        val ref =
            if (segments.size >= 3 && isCodeOrIfsc(segments.last())) {
                segments.last()
            } else {
                null
            }

        return Pair(sender, ref)
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

        // 1. Strip date prefix (e.g. "04-Oct-26 at GOOGLE STORE" -> "GOOGLE STORE")
        val dateMatch = DATE_AT_REGEX.find(cleaned)
        if (dateMatch != null && dateMatch.groupValues[1].isNotBlank()) {
            cleaned = dateMatch.groupValues[1].trim()
        }

        // 2. Strip Card prefix (e.g. "HDFC Bank Card 8302 At ASSPL" -> "ASSPL")
        val cardMatch = CARD_PREFIX_REGEX.find(cleaned)
        if (cardMatch != null) {
            val cardLast4 = cardMatch.groupValues[1]
            val realMerch = cardMatch.groupValues[2].trim()
            onCardExtracted(cardLast4)
            cleaned = realMerch
        }

        // 3. Reject noise
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
            s.contains("AIRTEL") || b.contains("AIRTEL") -> "AIRTEL"
            else -> null
        }
    }
}
