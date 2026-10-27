package com.kharcha.engine.categorizer

object MerchantNormalizer {
    private val BANK_NARRATIVE_REGEX =
        Regex(
            "^(DATE\\s+)?\\d{1,2}[A-Z]{3}\\d{2,4}\\s+(TRANSFER\\s+TO|TRF\\s+TO)\\s+|" +
                "^\\d{1,2}-\\d{2}-\\d{2,4}\\s+(TO|BY)\\s+|" +
                "^\\d{1,2}-[A-Z]{3}-\\d{2,4}\\s+(AT|TO|BY)\\s+|" +
                "^\\d{1,2}[A-Z]{3}\\d{2,4}\\s+(TRANSFER\\s+TO|TRF\\s+TO)\\s+|" +
                "^\\d{1,2}-[A-Z]{3}-\\d{2,4}\\s+INFO[A-Z*]+\\s*",
            RegexOption.IGNORE_CASE,
        )

    private val GATEWAY_PREFIX_REGEX =
        Regex(
            "^(RAZ\\*|RSP\\*|CAS\\*|PAY\\*|PYU\\*|PHP\\*|PPSL\\*|PTM\\*|Cashfree\\*|Razorpay\\*|Payu\\*|Ing\\*|null\\*|" +
                "RAZ\\s+|RSP\\s+|CAS\\s+|PAY\\s+|PYU\\s+|PHP\\s+|PPSL\\s+|PTM\\s+|Cashfree\\s+|Razorpay\\s+|Payu\\s+|Ing\\s+)",
            RegexOption.IGNORE_CASE,
        )

    private val PREFIXES_TO_STRIP =
        listOf(
            "UPI/", "POS/", "VPA/", "NEFT/", "IMPS/", "RTGS/", "ATM/", "BIL/", "MATM/",
            "ACH/", "ECS/", "PUR/", "INB/", "SI/", "NFS/", "CASH/", "CHQ/", "ECOM/",
            "UPI ", "POS ", "VPA ", "ECOM ",
        )

    private val NOISE_WORDS =
        setOf(
            "PVT", "LTD", "PRIVATE", "LIMITED", "INC", "LLP", "CORP", "CORPORATION", "CO",
            "ORDER", "TXN", "TRANSACTION", "PAYMENT", "REF", "UPI", "VPA", "POS", "ECOM",
            "PAYU", "RAZORPAY", "BILLDESK", "CCAVENUE", "CASHFREE", "PINELABS", "JUSPAY",
            "ID",
        )

    private val BANK_SUFFIXES =
        setOf(
            "ICICI", "HDFC", "SBI", "AXIS", "KOTAK", "PNB", "BOB", "YESBANK", "BANK",
            "OKAXIS", "OKSBI", "OKHDFC", "PAYTM", "HDFCBANK", "ICICIBANK", "SBIBANK",
        )

    fun normalize(rawMerchant: String?): String {
        if (rawMerchant.isNullOrBlank()) return ""

        var normalized = rawMerchant.trim().uppercase()

        // 1. Strip bank narrative date and transfer prefixes
        normalized = normalized.replace(BANK_NARRATIVE_REGEX, "").trim()

        // 2. Strip payment gateway prefixes
        normalized = normalized.replace(GATEWAY_PREFIX_REGEX, "").trim()

        // 3. Strip common channel prefixes
        for (prefix in PREFIXES_TO_STRIP) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.removePrefix(prefix).trim()
            }
        }

        // 4. Handle web/URL prefixes and merged domains like WWWBIGBASKETCOM
        if (normalized.startsWith("WWW.")) {
            normalized = normalized.removePrefix("WWW.").trim()
        } else if (normalized.startsWith("WWW ")) {
            normalized = normalized.removePrefix("WWW ").trim()
        } else if (normalized.startsWith("WWW") && normalized.endsWith("COM") && normalized.length > 6) {
            normalized = normalized.removePrefix("WWW").removeSuffix("COM").trim()
        }

        // 5. If it contains a UPI handle (e.g. swiggy@icici), strip the handle suffix
        if (normalized.contains("@")) {
            normalized = normalized.substringBefore("@").trim()
        }

        // 6. Remove leading/trailing special characters and multiple spaces
        normalized =
            normalized.replace(Regex("[^A-Z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        // 7. Remove standalone numbers of 4+ digits (order numbers, ref numbers, store IDs)
        // Keeps short identifiers like VJ82, 7 in MS 7 NINE, 25 in PUMP 25
        normalized =
            normalized.replace(Regex("\\b[0-9]{4,}\\b"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        // 8. Remove .com and other TLDs if present
        if (normalized.endsWith(" COM")) {
            normalized = normalized.removeSuffix(" COM").trim()
        }
        if (normalized.endsWith(" IN")) {
            normalized = normalized.removeSuffix(" IN").trim()
        }
        if (normalized.endsWith(" CO IN")) {
            normalized = normalized.removeSuffix(" CO IN").trim()
        }

        // 9. Remove common noise words and bank suffixes
        val words = normalized.split(" ").filter { it.isNotBlank() }
        val nonNoiseWords = words.filter { it !in NOISE_WORDS }
        val filteredWords = if (nonNoiseWords.isEmpty()) words else nonNoiseWords

        val withoutBankSuffix =
            if (filteredWords.size > 1 &&
                (filteredWords.last() in BANK_SUFFIXES || filteredWords.last().endsWith("BANK"))
            ) {
                filteredWords.dropLast(1)
            } else {
                filteredWords
            }

        return withoutBankSuffix.joinToString(" ").trim()
    }
}
