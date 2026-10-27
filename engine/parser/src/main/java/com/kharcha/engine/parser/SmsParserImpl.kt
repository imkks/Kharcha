package com.kharcha.engine.parser

import com.kharcha.core.model.Channel
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.ParsedTxn
import com.kharcha.core.model.RawSms
import com.kharcha.core.model.SmsParser

class SmsParserImpl(
    private val templates: List<ParserTemplate>,
) : SmsParser {
    private val fallbackParser = GenericFallbackParser()

    override fun parse(sms: RawSms): ParseResult {
        // 1. Noise Filter
        val ignoreResult = NoiseFilter.filter(sms)
        if (ignoreResult != null) {
            return ignoreResult
        }

        // 2. Template Matching
        for (template in templates) {
            val senderMatches =
                template.senderPatterns.any { pattern ->
                    sms.sender.matches(Regex(pattern))
                }

            if (senderMatches) {
                val regex = Regex(template.regex, RegexOption.IGNORE_CASE)
                val matchResult = regex.find(sms.body)

                if (matchResult != null) {
                    val amountStr = matchResult.groups["amount"]?.value
                    val amountMinor = AmountParser.parseMinorUnits(amountStr) ?: continue

                    val dateStr =
                        try {
                            matchResult.groups["date"]?.value
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    val timeStr =
                        try {
                            matchResult.groups["time"]?.value
                        } catch (e: IllegalArgumentException) {
                            null
                        }

                    val dateTime = DateParser.parse(dateStr, timeStr, template.dateFormats, sms.receivedAt)

                    val rawLast4 =
                        try {
                            matchResult.groups["last4"]?.value
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    var normalizedLast4 = CardAccountExtractionHelper.extractLast4(rawLast4)

                    val rawMerchant =
                        try {
                            matchResult.groups["merchant"]?.value
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    var channel = template.channel
                    var merchant = rawMerchant
                    if (merchant != null) {
                        merchant =
                            CardAccountExtractionHelper.cleanCandidateMerchant(merchant) { cardLast4 ->
                                if (normalizedLast4 == null) {
                                    normalizedLast4 = cardLast4
                                }
                                channel = Channel.CARD
                            }
                    }

                    val balanceStr =
                        try {
                            matchResult.groups["balance"]?.value
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    val balanceMinor = AmountParser.parseMinorUnits(balanceStr)
                    val refNumber =
                        try {
                            matchResult.groups["ref"]?.value
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    val upiId =
                        try {
                            matchResult.groups["upi"]?.value
                        } catch (e: IllegalArgumentException) {
                            null
                        }

                    return ParseResult.Success(
                        ParsedTxn(
                            amountMinor = amountMinor,
                            // Configurable later
                            currency = "INR",
                            direction = template.direction,
                            dateTime = dateTime,
                            accountLast4 = normalizedLast4,
                            bankName = template.bank,
                            merchantRaw = merchant ?: upiId,
                            channel = channel,
                            balanceAfterMinor = balanceMinor,
                            refNumber = refNumber,
                            upiId = upiId,
                            confidence = template.confidence,
                            templateId = template.id,
                        ),
                    )
                }
            }
        }

        // 3. Fallback Parser
        val fallbackTxn = fallbackParser.parse(sms)
        if (fallbackTxn != null) {
            return ParseResult.Success(fallbackTxn)
        }

        // 4. No Match
        return ParseResult.NoMatch
    }
}
