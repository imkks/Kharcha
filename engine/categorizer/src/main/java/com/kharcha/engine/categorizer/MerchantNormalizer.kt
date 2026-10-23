package com.kharcha.engine.categorizer

object MerchantNormalizer {
    private val PREFIXES_TO_STRIP =
        listOf(
            "UPI/", "POS/", "VPA/", "NEFT/", "IMPS/", "RTGS/", "ATM/", "BIL/", "MATM/",
            "ACH/", "ECS/", "PUR/", "INB/", "SI/", "NFS/", "CASH/", "CHQ/",
        )

    private val NOISE_WORDS =
        setOf(
            "PVT", "LTD", "PRIVATE", "LIMITED", "INC", "LLP", "CORP", "CORPORATION", "CO",
            "ORDER", "TXN", "TRANSACTION", "PAYMENT", "REF",
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

        // 2. Remove leading/trailing special characters and multiple spaces
        normalized =
            normalized.replace(Regex("[^A-Z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        // 3. Remove numbers (often transaction IDs, store IDs)
        normalized =
            normalized.replace(Regex("[0-9]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        // 4. Remove .com and other TLDs if present by replacing common ones
        if (normalized.endsWith(" COM")) {
            normalized = normalized.removeSuffix(" COM")
        }
        if (normalized.endsWith(" IN")) {
            normalized = normalized.removeSuffix(" IN")
        }

        // 5. Remove common noise words
        val words = normalized.split(" ").filter { it.isNotBlank() }
        val filteredWords = words.filter { it !in NOISE_WORDS }

        return filteredWords.joinToString(" ").trim()
    }
}
