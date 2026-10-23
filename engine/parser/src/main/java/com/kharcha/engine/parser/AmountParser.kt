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
        val cleanStr = amountStr.replace(",", "").replace(" ", "")

        return try {
            val amountDouble = cleanStr.toDouble()
            if (amountDouble <= 0.0) {
                return null // Reject zero and negative amounts
            }
            // Convert to minor units (multiply by 100)
            (amountDouble * 100).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
