package com.kharcha.engine.parser

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.ParsedTxn
import com.kharcha.core.model.RawSms

class GenericFallbackParser {
    private val amountRegex = Regex("(?i)(?:rs\\.?|inr\\.?|₹|inr:|rs:)\\s*([\\d,]+\\.?\\d*)")
    private val fallbackAmountRegex =
        Regex("(?i)\\b(?:debited|credited|spent|withdrawn|transferred|transfer|paid|sent|deducted|charged)" +
            "\\s*(?:by|for|of)?\\s*(?:rs\\.?|inr\\.?|₹)?\\s*([\\d,]+\\.?\\d*)")

    private val debitKeywords =
        listOf("debited", "spent", "sent", "withdrawn", "paid", "deducted", "charged")
    private val creditKeywords =
        listOf("credited", "received", "deposited", "added to", "refunded")

    fun parse(sms: RawSms): ParsedTxn? {
        val body = sms.body
        val bodyLower = body.lowercase()

        if (bodyLower.contains("coupon") || bodyLower.contains("voucher") ||
            bodyLower.contains("discount") || bodyLower.contains("to shop") ||
            bodyLower.contains("shop now") || bodyLower.contains("credit limit")
        ) {
            return null
        }

        val amountMatch = amountRegex.find(bodyLower) ?: fallbackAmountRegex.find(bodyLower) ?: return null
        val amountMinor = AmountParser.parseMinorUnits(amountMatch.groupValues[1]) ?: return null

        var direction: Direction? = null
        if (bodyLower.contains("transferred to") || bodyLower.contains("transfer to")) {
            direction = Direction.DEBIT
        } else if (bodyLower.contains("transferred from") || bodyLower.contains("transfer from")) {
            direction = Direction.CREDIT
        } else if (creditKeywords.any { bodyLower.contains(it) }) {
            direction = Direction.CREDIT
        } else if (debitKeywords.any { bodyLower.contains(it) }) {
            direction = Direction.DEBIT
        } else if (bodyLower.contains("transfer") || bodyLower.contains("transferred")) {
            direction = if (bodyLower.contains("to your a/c")) Direction.CREDIT else Direction.DEBIT
        }

        if (direction == null) return null

        val channel =
            when {
                bodyLower.contains("upi") || bodyLower.contains("vpa") -> Channel.UPI
                bodyLower.contains("atm") -> Channel.ATM
                bodyLower.contains("card") -> Channel.CARD
                bodyLower.contains("neft") -> Channel.NEFT
                bodyLower.contains("imps") -> Channel.IMPS
                bodyLower.contains("rtgs") -> Channel.NEFT
                bodyLower.contains("wallet") -> Channel.WALLET
                bodyLower.contains("transfer") -> Channel.UPI
                else -> Channel.OTHER
            }

        val last4 =
            Regex("(?i)(?:a/c|acct|account|ac|card).*?(?:\\*+|x+|ending\\s*(?:with|in)?\\s*|[.\\- ]+)(\\d{3,4})\\b")
                .find(bodyLower)?.groupValues?.get(1)
                ?: Regex("(?i)\\b(?:ending|ending in|ending with)\\s*(\\d{3,4})\\b")
                    .find(bodyLower)?.groupValues?.get(1)
                ?: Regex("(?i)(?:a/c|acct|card)[\\s\\-x]*(\\d{4})\\b").find(bodyLower)?.groupValues?.get(1)

        val upiMatch = Regex("(?i)\\b([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)\\b").find(body)
        val upiId = upiMatch?.groupValues?.get(1)

        val refMatch =
            Regex("(?i)(?:ref(?:erence)?(?:\\s*(?:no|num|#))?|txn|rrn)[\\s:.]*([a-zA-Z0-9]{6,20})").find(body)
        val refNumber = refMatch?.groupValues?.get(1)

        val balMatch =
            Regex("(?i)(?:avl\\.?\\s*(?:bal|limit)|bal(?:ance)?)\\s*(?:is|:)?\\s*(?:rs\\.?|inr)?\\s*([\\d,]+\\.?\\d*)")
                .find(body)
        val balanceAfterMinor = balMatch?.groupValues?.get(1)?.let { AmountParser.parseMinorUnits(it) }

        var merchantRaw: String? = null

        val vpaPattern = Regex("(?i)\\bvpa\\s+([a-zA-Z0-9.\\-_@]+)").find(body)
        if (vpaPattern != null) {
            merchantRaw = vpaPattern.groupValues[1].trim()
        }

        if (merchantRaw == null) {
            val merchantPattern =
                Regex(
                    "(?i)\\b(?:to|at|towards|for)\\s+([A-Za-z0-9.\\-_@/& ]{2,35}?)" +
                        "(\\s+(?:on|ref|vpa|via|upi|bal|balance|avl|a/c|acct|using|with|" +
                        "credited|debited|is|was|\\d{2}[\\-/])|[.,;]|$)",
                ).find(body)
            merchantRaw = merchantPattern?.groupValues?.get(1)?.trim()
        }

        if (merchantRaw == null && direction == Direction.CREDIT) {
            val creditSenderPattern =
                Regex(
                    "(?i)\\b(?:by|from)\\s+([A-Za-z0-9.\\-_@/& ]{2,35}?)" +
                        "(\\s+(?:on|ref|via|upi|bal|balance|avl|a/c|acct|[.,;]|$))",
                ).find(body)
            merchantRaw = creditSenderPattern?.groupValues?.get(1)?.trim()
        }

        if (merchantRaw == null) {
            val infoPattern = Regex("(?i)\\binfo[*: ]+([a-zA-Z0-9.\\-_* ]{2,30})").find(body)
            merchantRaw = infoPattern?.groupValues?.get(1)?.trim()
        }

        if (merchantRaw == null && upiId != null) {
            merchantRaw = upiId
        }

        merchantRaw = cleanMerchant(merchantRaw)

        val bankName = extractBankName(sms.sender, body)

        val confidence =
            when {
                merchantRaw != null && last4 != null -> 0.85f
                last4 != null -> 0.70f
                merchantRaw != null -> 0.65f
                else -> 0.50f
            }

        return ParsedTxn(
            amountMinor = amountMinor,
            currency = "INR",
            direction = direction,
            dateTime = sms.receivedAt,
            accountLast4 = last4,
            bankName = bankName,
            merchantRaw = merchantRaw,
            channel = channel,
            balanceAfterMinor = balanceAfterMinor,
            refNumber = refNumber,
            upiId = upiId,
            confidence = confidence,
            templateId = null,
        )
    }

    private fun cleanMerchant(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var cleaned = raw.trim().trim('.', ',', '-', ':', ';')
        val dropPrefixes = listOf("a/c", "acct", "vpa", "upi", "vps*", "pos*")
        for (p in dropPrefixes) {
            if (cleaned.startsWith(p, ignoreCase = true)) {
                cleaned = cleaned.substring(p.length).trim()
            }
        }
        return if (cleaned.length >= 2) cleaned else null
    }

    private fun extractBankName(sender: String, body: String): String? {
        val s = sender.uppercase()
        val b = body.uppercase()
        return when {
            s.contains("HDFC") || b.contains("HDFC") -> "HDFC"
            s.contains("ICICI") || b.contains("ICICI") -> "ICICI"
            s.contains("SBI") || b.contains("SBI") -> "SBI"
            s.contains("AXIS") || b.contains("AXIS") -> "AXIS"
            s.contains("KOTAK") || b.contains("KOTAK") -> "KOTAK"
            s.contains("PNB") || b.contains("PNB") -> "PNB"
            s.contains("BOB") || b.contains("BOB") -> "BOB"
            s.contains("IDFC") || b.contains("IDFC") -> "IDFC"
            s.contains("INDUS") || b.contains("INDUS") -> "INDUSIND"
            s.contains("YES") || b.contains("YES BANK") -> "YES"
            else -> null
        }
    }
}
