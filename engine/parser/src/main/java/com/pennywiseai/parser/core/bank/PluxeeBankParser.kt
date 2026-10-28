package com.pennywiseai.parser.core.bank

import java.math.BigDecimal

/**
 * Parser for Pluxee (India) — prepaid meal-benefit card, the rebrand of Sodexo.
 *
 * Supported format:
 * - Spend: "Rs. 40.00 spent from Pluxee  Meal wallet, card no.xx1234 on
 *           17-08-2026 16:03:14 at NEW SHAKTHI  . Avl bal Rs.24342.81. Not you call ..."
 *
 * Notes:
 * - The live SMS carries irregular double-spaces ("Pluxee  Meal", "SHAKTHI  ."),
 *   so the regexes here are whitespace-tolerant.
 * - This is a prepaid wallet instrument, NOT a credit card, so no credit-card
 *   semantics (available limit) are set. Amount, type, balance and the card's
 *   last-4 are handled by the base class.
 *
 * Sender IDs are Indian DLT headers embedding the entity name, e.g. AD-PLUXEE,
 * VM-PLUXEE, JD-PLUXEE-S. Routing matches any sender containing "PLUXEE".
 */
class PluxeeBankParser : BaseIndianBankParser() {

    override fun getBankName() = "Pluxee"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        // Match the DLT-wrapped entity name (XX-PLUXEE-S) as well as bare senders.
        return normalizedSender.contains("PLUXEE")
    }

    override fun detectIsCard(message: String): Boolean = true

    override fun extractMerchant(message: String, sender: String): String? {
        val atPattern = Regex(
            """\bat\s+(.+?)(?:\s+on\s+\d|\.\s*Avl|\.\s*Remaining|$)""",
            RegexOption.IGNORE_CASE
        )
        atPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        val cardPattern = Regex(
            """card\s+(?:no\.?\s*)?(?:xx|XX|\*)*(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        cardPattern.find(message)?.let { match ->
            extractLast4Digits(match.groupValues[1])?.let { return it }
        }

        return super.extractAccountLast4(message)
    }
}
