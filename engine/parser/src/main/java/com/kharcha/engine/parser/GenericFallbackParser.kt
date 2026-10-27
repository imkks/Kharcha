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
            bodyLower.contains("shop now") || bodyLower.contains("credit limit") ||
            bodyLower.contains("power cash") || bodyLower.contains("bonus cash") ||
            bodyLower.contains("brand cash") || bodyLower.contains("store credit") ||
            bodyLower.contains("cash wallet") || bodyLower.contains("reward wallet") ||
            bodyLower.contains("access your wallet") || bodyLower.contains("will be deducted") ||
            bodyLower.contains("will be debited") || bodyLower.contains("scheduled for") ||
            bodyLower.contains("maintain balance") || bodyLower.contains("e-mandate")
        ) {
            return null
        }

        val scopedBody = ClauseSemanticScoper.isolateTransactionClause(body)
        val scopedLower = scopedBody.lowercase()

        val amountMatch =
            amountRegex.find(scopedLower)
                ?: fallbackAmountRegex.find(scopedLower)
                ?: amountRegex.find(bodyLower)
                ?: fallbackAmountRegex.find(bodyLower)
                ?: return null
        val amountMinor = AmountParser.parseMinorUnits(amountMatch.groupValues[1]) ?: return null

        val isBillPaymentFromUser =
            Regex(
                "(?i)received\\s+from\\s+(?:(?:your|the)\\s+)?(?:(?:sbi|hdfc|icici|axis|kotak)\\s+)?(?:debit\\s*card|credit\\s*card|card|a/c|account)",
            ).containsMatchIn(bodyLower)

        var direction: Direction? = null
        if (isBillPaymentFromUser) {
            direction = Direction.DEBIT
        } else {
            val isExplicitAccountDebit =
                Regex(
                    "(?i)\\b(?:a/c|acct|account|card|acc)\\b.*\\bdebited\\b|" +
                        "\\bdebited\\s+(?:for|with|from)\\b|\\bsent\\s+(?:rs|inr|₹).*\\bfrom\\b",
                ).containsMatchIn(bodyLower)
            val isExplicitAccountCredit =
                Regex(
                    "(?i)\\b(?:a/c|acct|account|card|acc)\\b.*\\bcredited\\b|" +
                        "\\bcredited\\s+(?:to|in|into)\\b|\\bdeposited\\s+in\\b|\\bcredit\\s+alert\\b",
                ).containsMatchIn(bodyLower)

            if (isExplicitAccountDebit && !isExplicitAccountCredit) {
                direction = Direction.DEBIT
            } else if (isExplicitAccountCredit && !isExplicitAccountDebit) {
                direction = Direction.CREDIT
            } else if (isExplicitAccountDebit && isExplicitAccountCredit) {
                // E.g. "Acct XX321 debited ... Acct XX034 credited" / "... Zepto credited" -> user's account debited
                direction = Direction.DEBIT
            } else if (bodyLower.contains("transferred to") || bodyLower.contains("transfer to")) {
                direction = Direction.DEBIT
            } else if (bodyLower.contains("transferred from") || bodyLower.contains("transfer from")) {
                direction = Direction.CREDIT
            } else if (debitKeywords.any { bodyLower.contains(it) } && !creditKeywords.any { bodyLower.contains(it) }) {
                direction = Direction.DEBIT
            } else if (creditKeywords.any { bodyLower.contains(it) }) {
                direction = Direction.CREDIT
            } else if (debitKeywords.any { bodyLower.contains(it) }) {
                direction = Direction.DEBIT
            } else if (bodyLower.contains("transfer") || bodyLower.contains("transferred")) {
                direction = if (bodyLower.contains("to your a/c")) Direction.CREDIT else Direction.DEBIT
            }
        }

        if (direction == null) return null

        var channel =
            when {
                bodyLower.contains("upi") || bodyLower.contains("vpa") -> Channel.UPI
                bodyLower.contains("atm") -> Channel.ATM
                bodyLower.contains("card") || bodyLower.contains("cc") -> Channel.CARD
                bodyLower.contains("neft") -> Channel.NEFT
                bodyLower.contains("imps") -> Channel.IMPS
                bodyLower.contains("rtgs") -> Channel.NEFT
                bodyLower.contains("wallet") -> Channel.WALLET
                bodyLower.contains("transfer") -> Channel.UPI
                else -> Channel.OTHER
            }

        var last4 = CardAccountExtractionHelper.extractLast4(body)

        val upiMatch =
            Regex(
                "(?i)\\b(?!(?:debited|credited|spent|withdrawn|balance|avl|paid)@)([a-zA-Z0-9.\\-_]{2,}@[a-zA-Z0-9]+)\\b",
            ).find(body)
        val upiId = upiMatch?.groupValues?.get(1)

        val refMatch =
            Regex("(?i)(?:ref(?:erence)?(?:\\s*(?:no|num|#))?|txn|rrn)[\\s:.]*([a-zA-Z0-9]{6,20})").find(body)
        var refNumber = refMatch?.groupValues?.get(1)

        val balMatch =
            Regex("(?i)(?:avl\\.?\\s*(?:bal|limit)|bal(?:ance)?)\\s*(?:is|:)?\\s*(?:rs\\.?|inr)?\\s*([\\d,]+\\.?\\d*)")
                .find(body)
        val balanceAfterMinor = balMatch?.groupValues?.get(1)?.let { AmountParser.parseMinorUnits(it) }

        var merchantRaw: String? = null

        val contentBody =
            body.replace(
                Regex(
                    "(?i)\\b(?:" +
                        "if\\s+(?:not\\s+you|this\\s+was\\s+not\\s+you|not\\s+done\\s+by\\s+you)|" +
                        "(?:for\\s+)?(?:any\\s+)?dispute(?:\\s+call|\\s+sms|\\s+report|\\s*[,.]|$)|" +
                        "to\\s+block|call\\s+\\d+|sms\\s+block|report\\s+(?:fraud|to)|" +
                        "forward\\s+(?:this)?\\s*sms|" +
                        "(?:cheque\\s+deposits?\\s+in\\s+a/c\\s+are\\s+)?subject\\s+to\\s+clearing|" +
                        "for\\s+more\\s+secure\\s+digital\\s+banking|" +
                        "(?:call|dial|sms)\\s+.*\\b(?:to\\s+know\\s+your\\s+balance|for\\s+dispute)|" +
                        "download\\s+app|link\\s+to\\s+download" +
                        ").*$",
                ),
                "",
            )

        // 1. NEFT / RTGS / IMPS Credit narrative payee extraction
        val (neftSender, neftRef) = CardAccountExtractionHelper.extractNeftSenderAndRef(contentBody)
        if (neftSender != null) {
            merchantRaw = neftSender
            if (refNumber == null && neftRef != null) {
                refNumber = neftRef
            }
        }

        val vpaPattern = Regex("(?i)\\bvpa\\s+([a-zA-Z0-9.\\-_@]+)").find(contentBody)
        if (merchantRaw == null && vpaPattern != null) {
            merchantRaw = vpaPattern.groupValues[1].trim()
        }

        if (merchantRaw == null) {
            val merchantMatches =
                Regex(
                    "(?i)\\b(?:to|at|towards|for|on)\\s+([A-Za-z0-9.\\-_@/&* ]{2,35}?)" +
                        "(?=\\s+(?:on|ref|vpa|via|upi|bal|balance|avl|a/c|acct|using|with|" +
                        "credited|debited|is|was|\\d{2}[\\-/])|[.,;]|$)",
                ).findAll(contentBody)

            for (m in merchantMatches) {
                val candidate = m.groupValues[1].trim()
                val cleaned = CardAccountExtractionHelper.cleanCandidateMerchant(candidate) { cardLast4 ->
                    if (last4 == null) {
                        last4 = cardLast4
                    }
                    channel = Channel.CARD
                }

                if (cleaned != null) {
                    val isDate =
                        cleaned.matches(Regex("(?i)^\\d{1,2}[-/][A-Za-z0-9]{3}(?:[-/]\\d{2,4})?$")) ||
                            cleaned.matches(Regex("^\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}$")) ||
                            cleaned.matches(Regex("(?i)^\\d{1,2}[A-Za-z]{3}\\d{2,4}$"))
                    val isDigitsOnly = cleaned.matches(Regex("^[0-9+\\- ]+$"))
                    val isAmount = cleaned.matches(Regex("(?i)^(?:rs\\.?|inr\\.?|₹)?\\s*[\\d,.]*$"))
                    if (!isDate && !isDigitsOnly && !isAmount) {
                        merchantRaw = cleaned
                        break
                    }
                }
            }
        }

        if (merchantRaw == null && direction == Direction.DEBIT) {
            val creditedPayeePattern =
                Regex("(?i)[;.]\\s*([A-Za-z0-9.\\-_@/&* ]{2,35}?)\\s+credited").find(contentBody)
            merchantRaw = creditedPayeePattern?.groupValues?.get(1)?.trim()
        }

        if (merchantRaw == null && direction == Direction.CREDIT) {
            val creditSenderPattern =
                Regex(
                    "(?i)\\b(?:by|from)\\s+([A-Za-z0-9.\\-_@/&* ]{2,35}?)" +
                        "(\\s+(?:on|ref|via|upi|bal|balance|avl|a/c|acct|[.,;]|$))",
                ).find(contentBody)
            merchantRaw = creditSenderPattern?.groupValues?.get(1)?.trim()
        }

        if (merchantRaw == null) {
            val infoPattern = Regex("(?i)\\binfo[*: ]+([a-zA-Z0-9.\\-_* ]{2,30})").find(contentBody)
            merchantRaw = infoPattern?.groupValues?.get(1)?.trim()
        }

        if (merchantRaw == null && upiId != null) {
            merchantRaw = upiId
        }

        merchantRaw = cleanMerchant(merchantRaw)

        val bankName = CardAccountExtractionHelper.extractBankName(sms.sender, body)

        if (channel == Channel.ATM || bodyLower.contains("atm") || bodyLower.contains("cash wdl")) {
            channel = Channel.ATM
            val atmMerchant = CardAccountExtractionHelper.cleanAtmMerchant(merchantRaw ?: contentBody, bankName)
            if (atmMerchant != null) {
                merchantRaw = atmMerchant
            }
        }

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
        if (cleaned.matches(Regex("^[0-9+\\- ]+$"))) return null
        val dropPrefixes = listOf("a/c", "acct", "vpa", "upi", "vps*", "pos*")
        for (p in dropPrefixes) {
            if (cleaned.startsWith(p, ignoreCase = true)) {
                cleaned = cleaned.substring(p.length).trim()
            }
        }
        if (CardAccountExtractionHelper.isAccountOrCardOrSystemNoise(cleaned)) {
            return null
        }
        return if (cleaned.length >= 2) cleaned else null
    }
}
