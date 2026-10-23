package com.kharcha.engine.parser

object AmountParser {
    /**
     * Parses an amount string extracted from an SMS into minor units (paise/cents).
     * Rejects invalid amounts (e.g., negative, zero, or formatted with Rs.1.2L).
     *
     * @param amountStr the raw amount string (e.g., "1,234.50", "1234", "85,000.00")
     * @return the amount in minor units, or null if parsing fails or amount is invalid
     */
    fun parseMinorUnits(amountStr: String?): Long? {
        if (amountStr.isNullOrBlank()) return null

        // Reject amounts using "L" or "k" styles like 1.2L or 5k
        if (amountStr.contains(Regex("[a-zA-Z]"))) {
            return null
        }

        // Remove commas and spaces
        val cleanStr = amountStr.replace(",", "").replace(" ", "").trim()
        if (cleanStr.isEmpty()) return null

        return try {
            val parts = cleanStr.split(".")
            if (parts.size > 2) return null

            val rupeesPart = parts[0]
            if (rupeesPart.isEmpty() && parts.size == 1) return null
            val rupees = if (rupeesPart.isEmpty()) 0L else rupeesPart.toLong()
            if (rupees < 0L) return null

            val paise =
                if (parts.size == 2) {
                    val rawPaise = parts[1]
                    when {
                        rawPaise.isEmpty() -> 0L
                        rawPaise.length == 1 -> rawPaise.toLong() * 10L
                        rawPaise.length == 2 -> rawPaise.toLong()
                        else -> rawPaise.substring(0, 2).toLong()
                    }
                } else {
                    0L
                }

            if (paise < 0L) return null

            val totalMinor = (rupees * 100L) + paise
            if (totalMinor <= 0L) null else totalMinor
        } catch (e: NumberFormatException) {
            null
        }
    }
}
