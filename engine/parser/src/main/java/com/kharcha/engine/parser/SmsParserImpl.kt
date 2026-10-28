package com.kharcha.engine.parser

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.ParsedTxn
import com.kharcha.core.model.RawSms
import com.kharcha.core.model.SmsParser
import com.pennywiseai.parser.core.GenericTransactionExtractor
import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.BankParserFactory
import java.math.BigDecimal
import java.math.RoundingMode

class SmsParserImpl(
    @Suppress("UnusedPrivateProperty")
    private val templates: List<ParserTemplate> = emptyList(),
) : SmsParser {

    private val upiRegex = Regex("""\b([a-zA-Z0-9.\-_]+@[a-zA-Z0-9]+)\b""")
    private val balanceOrLimitRegex = Regex(
        """(?i)\b(?:bal|balance|avl\s*bal|available\s*balance|avl\s*limit|available\s*limit|limit)[-:\s]+(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.\d{1,2})?)""",
    )
    private val fallbackMerchantRegex = Regex(
        """(?i)\b(?:to\s+vpa|to|at|for|towards)\s+([A-Za-z0-9\s._*-]+?)(?:\s+(?:on\b|at\b|ref\b|upi\b|avl\b|bal\b|\d{1,2}[-/])|$)""",
    )

    override fun parse(sms: RawSms): ParseResult {
        // 1. Noise Filter (comprehensive noise engine from Pennywise & Kharcha)
        val ignoreResult = NoiseFilter.filter(sms)
        if (ignoreResult != null) {
            return ignoreResult
        }

        // 2. PennyWise BankParserFactory
        val parsed = BankParserFactory.parse(sms.body, sms.sender, sms.receivedAt)
        if (parsed != null) {
            val amountMinor = (parsed.amount * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()

            var balanceMinor = parsed.balance?.let {
                (it * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
            } ?: parsed.creditLimit?.let {
                (it * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
            }

            if (balanceMinor == null) {
                balanceOrLimitRegex.find(sms.body)?.let { m ->
                    val str = m.groupValues[1].replace(",", "")
                    try {
                        balanceMinor = (BigDecimal(str) * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
                    } catch (_: NumberFormatException) {
                        // ignore
                    }
                }
            }

            val direction = if (parsed.type == TransactionType.INCOME) {
                Direction.CREDIT
            } else {
                Direction.DEBIT
            }

            val bLower = sms.body.lowercase()
            val isBillPaymentDebit = bLower.contains("credit card bil credited") ||
                bLower.contains("credit card bill credited") ||
                (bLower.contains("acct xx") && bLower.contains("debited") && bLower.contains("credit card"))

            val isCard = !isBillPaymentDebit && (
                parsed.isFromCard ||
                    bLower.contains("credit card") ||
                    bLower.contains("debit card") ||
                    bLower.contains("card ending") ||
                    bLower.contains("card xx") ||
                    bLower.contains("card *") ||
                    bLower.contains("card no") ||
                    bLower.contains("spent on card") ||
                    bLower.contains("spent on credit card") ||
                    bLower.contains("spent on debit card") ||
                    bLower.contains("on your card") ||
                    Regex("""\bcard\s+\d{4}\b""").containsMatchIn(bLower)
            )

            val isAtm = bLower.contains("atm") ||
                (bLower.contains("withdrawn") && !bLower.contains("transfer"))

            val isNeft = bLower.contains("neft") ||
                bLower.contains("by transfer") ||
                bLower.contains("by salary") ||
                bLower.contains("salary/")

            val isImps = bLower.contains("imps")
            val isWallet = parsed.isMobileWallet ||
                bLower.contains("wallet") ||
                bLower.contains("payments bank") ||
                sms.sender.contains("AIRBNK", ignoreCase = true) ||
                sms.sender.contains("AIRTELPB", ignoreCase = true)

            val isUpi = !isCard && (
                bLower.contains("upi") ||
                    bLower.contains("vpa") ||
                    bLower.contains("fi money") ||
                    bLower.contains("with fi") ||
                    sms.sender.contains("fimony", ignoreCase = true) ||
                    sms.sender.contains("upi", ignoreCase = true) ||
                    parsed.merchant?.contains("@") == true ||
                    parsed.reference?.matches(Regex("\\d{12}")) == true
                )

            val channel = when {
                isCard -> Channel.CARD
                isAtm -> Channel.ATM
                isNeft -> Channel.NEFT
                isImps -> Channel.IMPS
                isUpi -> Channel.UPI
                isWallet -> Channel.WALLET
                else -> Channel.OTHER
            }

            val upiId = upiRegex.findAll(sms.body).map { it.value }.firstOrNull { candidate ->
                val lower = candidate.lowercase()
                !lower.startsWith("debited@") && !lower.startsWith("credited@")
            } ?: if (parsed.merchant?.contains("@") == true) parsed.merchant else null

            var cleanedMerchant = parsed.merchant
                ?.removePrefix("UPI - ")
                ?.removePrefix("UPI-")
                ?.removePrefix("UPI -")
                ?.trim()

            if (cleanedMerchant != null) {
                cleanedMerchant = CardAccountExtractionHelper.cleanCandidateMerchant(cleanedMerchant) ?: cleanedMerchant
            }

            val merchantRaw = cleanedMerchant?.takeIf { it.isNotBlank() }
                ?: upiId
                ?: fallbackMerchantRegex.find(sms.body)?.groupValues?.get(1)?.trim()?.let {
                    CardAccountExtractionHelper.cleanCandidateMerchant(it) ?: it
                }?.takeIf { it.isNotBlank() }

            val rawBank = parsed.bankName
            val normalizedBank = if (sms.sender.contains("FIMONY", ignoreCase = true) || sms.body.contains("Fi Money", ignoreCase = true)) {
                "FI"
            } else {
                normalizeBankName(rawBank)
            }

            return ParseResult.Success(
                ParsedTxn(
                    amountMinor = amountMinor,
                    currency = parsed.currency,
                    direction = direction,
                    dateTime = sms.receivedAt,
                    accountLast4 = parsed.accountLast4,
                    bankName = normalizedBank,
                    merchantRaw = merchantRaw,
                    channel = channel,
                    balanceAfterMinor = balanceMinor,
                    refNumber = parsed.reference,
                    upiId = upiId,
                    confidence = 1.0f,
                    templateId = normalizedBank,
                ),
            )
        }

        // 3. PennyWise GenericTransactionExtractor Suggestion Fallback
        val suggestion = GenericTransactionExtractor.extract(sms.sender, sms.body, sms.receivedAt)
        if (suggestion != null && suggestion.confidence >= GenericTransactionExtractor.SUGGEST_AT) {
            val sTxn = suggestion.transaction
            val amountMinor = (sTxn.amount * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
            var balanceMinor = sTxn.balance?.let {
                (it * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
            } ?: sTxn.creditLimit?.let {
                (it * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
            }

            if (balanceMinor == null) {
                balanceOrLimitRegex.find(sms.body)?.let { m ->
                    val str = m.groupValues[1].replace(",", "")
                    try {
                        balanceMinor = (BigDecimal(str) * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
                    } catch (_: NumberFormatException) {
                        // ignore
                    }
                }
            }

            val direction = if (sTxn.type == TransactionType.INCOME) Direction.CREDIT else Direction.DEBIT

            val bLower = sms.body.lowercase()
            val isCard = sTxn.isFromCard || bLower.contains("card")
            val isAtm = bLower.contains("atm") || (bLower.contains("withdrawn") && !bLower.contains("transfer"))
            val isNeft = bLower.contains("neft")
            val isImps = bLower.contains("imps")
            val isUpi = !isCard && (bLower.contains("upi") || bLower.contains("vpa") || sms.sender.contains("upi", ignoreCase = true))

            val channel = when {
                isCard -> Channel.CARD
                isAtm -> Channel.ATM
                isNeft -> Channel.NEFT
                isImps -> Channel.IMPS
                isUpi -> Channel.UPI
                else -> Channel.OTHER
            }

            val upiId = upiRegex.find(sms.body)?.value
            val merchantRaw = sTxn.merchant?.takeIf { it.isNotBlank() } ?: upiId

            val normalizedBank = normalizeBankName(sTxn.bankName)

            return ParseResult.Success(
                ParsedTxn(
                    amountMinor = amountMinor,
                    currency = sTxn.currency,
                    direction = direction,
                    dateTime = sms.receivedAt,
                    accountLast4 = sTxn.accountLast4,
                    bankName = normalizedBank,
                    merchantRaw = merchantRaw,
                    channel = channel,
                    balanceAfterMinor = balanceMinor,
                    refNumber = sTxn.reference,
                    upiId = upiId,
                    confidence = suggestion.confidence / 100.0f,
                    templateId = normalizedBank,
                ),
            )
        }

        return ParseResult.NoMatch
    }

    private fun normalizeBankName(rawBank: String): String {
        return when {
            rawBank.contains("HDFC", ignoreCase = true) -> "HDFC"
            rawBank.contains("ICICI", ignoreCase = true) -> "ICICI"
            rawBank.contains("SBI", ignoreCase = true) || rawBank.contains("State Bank of India", ignoreCase = true) -> "SBI"
            rawBank.contains("AXIS", ignoreCase = true) -> "AXIS"
            rawBank.contains("KOTAK", ignoreCase = true) -> "KOTAK"
            rawBank.contains("PNB", ignoreCase = true) || rawBank.contains("Punjab National", ignoreCase = true) -> "PNB"
            rawBank.contains("BOB", ignoreCase = true) || rawBank.contains("Bank of Baroda", ignoreCase = true) -> "BOB"
            rawBank.contains("CANARA", ignoreCase = true) -> "CANARA"
            rawBank.contains("UNION", ignoreCase = true) || rawBank.contains("UBI", ignoreCase = true) -> "UNION"
            rawBank.contains("IDFC", ignoreCase = true) -> "IDFC"
            rawBank.contains("INDUSIND", ignoreCase = true) -> "INDUSIND"
            rawBank.contains("YES", ignoreCase = true) -> "YESBANK"
            rawBank.contains("AMEX", ignoreCase = true) || rawBank.contains("American Express", ignoreCase = true) -> "AMEX"
            rawBank.contains("CITI", ignoreCase = true) -> "CITI"
            rawBank.contains("FEDERAL", ignoreCase = true) -> "FEDERAL"
            rawBank.contains("SLICE", ignoreCase = true) -> "SLICE"
            rawBank.contains("ONECARD", ignoreCase = true) -> "ONECARD"
            rawBank.contains("JUPITER", ignoreCase = true) -> "JUPITER"
            rawBank.contains("FIMONY", ignoreCase = true) || rawBank.contains("FI MONEY", ignoreCase = true) || rawBank.split(Regex("\\s+")).any { it.equals("FI", ignoreCase = true) } -> "FI"
            rawBank.contains("PAYTM", ignoreCase = true) -> "PAYTM"
            rawBank.contains("AIRTEL", ignoreCase = true) -> "AIRTEL"
            rawBank.contains("SCB", ignoreCase = true) || rawBank.contains("Standard Chartered", ignoreCase = true) -> "SCB"
            rawBank.contains("AUBANK", ignoreCase = true) || rawBank.contains("AU Small", ignoreCase = true) || rawBank.contains("AU Bank", ignoreCase = true) -> "AUBANK"
            rawBank.contains("CBI", ignoreCase = true) || rawBank.contains("Central Bank", ignoreCase = true) -> "CBI"
            rawBank.contains("BOI", ignoreCase = true) || rawBank.contains("Bank of India", ignoreCase = true) -> "BOI"
            rawBank.contains("UCO", ignoreCase = true) -> "UCO"
            rawBank.contains("INDIAN BANK", ignoreCase = true) -> "INDIAN_BANK"
            rawBank.contains("PLUXEE", ignoreCase = true) -> "PLUXEE"
            rawBank.contains("CRED", ignoreCase = true) -> "CRED"
            else -> rawBank
        }
    }
}
