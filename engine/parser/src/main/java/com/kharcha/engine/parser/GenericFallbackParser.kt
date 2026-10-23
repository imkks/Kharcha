package com.kharcha.engine.parser

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.ParsedTxn
import com.kharcha.core.model.RawSms

class GenericFallbackParser {
    private val amountRegex = Regex("(?i)(?:rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)")
    private val debitKeywords = listOf("debited", "spent", "sent", "withdrawn", "paid")
    private val creditKeywords = listOf("credited", "received", "deposited")

    fun parse(sms: RawSms): ParsedTxn? {
        val bodyLower = sms.body.lowercase()

        val amountMatch = amountRegex.find(bodyLower) ?: return null
        val amountMinor = AmountParser.parseMinorUnits(amountMatch.groupValues[1]) ?: return null

        var direction: Direction? = null
        if (debitKeywords.any { bodyLower.contains(it) }) {
            direction = Direction.DEBIT
        } else if (creditKeywords.any { bodyLower.contains(it) }) {
            direction = Direction.CREDIT
        }

        if (direction == null) return null

        val channel =
            if (bodyLower.contains("upi") || bodyLower.contains("vpa")) {
                Channel.UPI
            } else if (bodyLower.contains("atm")) {
                Channel.ATM
            } else if (bodyLower.contains("card")) {
                Channel.CARD
            } else if (bodyLower.contains("neft")) {
                Channel.NEFT
            } else {
                Channel.OTHER
            }

        // Very basic extraction for last4 and merchant if possible
        val last4 =
            Regex("(?i)(?:a/c|acct|card).*?\\*+?(\\d{4})").find(bodyLower)?.groupValues?.get(1)
                ?: Regex("(?i)(?:a/c|acct|card)[\\s\\-x]*(\\d{4})\\b").find(bodyLower)?.groupValues?.get(1)

        val confidence = if (last4 != null) 0.7f else 0.5f

        return ParsedTxn(
            amountMinor = amountMinor,
            // Assuming INR for generic fallback
            currency = "INR",
            direction = direction,
            dateTime = sms.receivedAt,
            accountLast4 = last4,
            bankName = null,
            merchantRaw = null,
            channel = channel,
            balanceAfterMinor = null,
            refNumber = null,
            upiId = null,
            confidence = confidence,
            templateId = null,
        )
    }
}
