package com.kharcha.engine.categorizer

object MerchantNormalizer {
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
        )

    private val BANK_SUFFIXES =
        setOf(
            "ICICI", "HDFC", "SBI", "AXIS", "KOTAK", "PNB", "BOB", "YESBANK", "BANK",
            "OKAXIS", "OKSBI", "OKHDFC", "PAYTM", "HDFCBANK", "ICICIBANK", "SBIBANK",
        )

    fun normalize(rawMerchant: String?): String {
        if (rawMerchant.isNullOrBlank()) return ""

        var normalized = rawMerchant.trim().uppercase()

        // 1. Strip common channel prefixes
        for (prefix in PREFIXES_TO_STRIP) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.removePrefix(prefix).trim()
            }
        }

        // 2. If it contains a UPI handle (e.g. swiggy@icici), strip the handle suffix
        if (normalized.contains("@")) {
            normalized = normalized.substringBefore("@").trim()
        }

        // 3. Remove leading/trailing special characters and multiple spaces
        normalized =
            normalized.replace(Regex("[^A-Z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        // 4. Remove numbers (often transaction IDs, store IDs)
        normalized =
            normalized.replace(Regex("[0-9]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        // 5. Remove .com and other TLDs if present
        if (normalized.endsWith(" COM")) {
            normalized = normalized.removeSuffix(" COM")
        }
        if (normalized.endsWith(" IN")) {
            normalized = normalized.removeSuffix(" IN")
        }

        // 6. Remove common noise words and bank suffixes
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
