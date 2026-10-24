package com.kharcha.engine.parser

import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.RawSms

object NoiseFilter {
    private val otpRegex = Regex("(?i)\\b(otp|one time password)\\b|do not share")
    private val promoKeywordsRegex =
        Regex("(?i)\\b(coupon[s]?|voucher[s]?|discount[s]?|promo\\b|promo\\s*code|coupon\\s*code|" +
            "voucher\\s*code|reward\\s*points?|supercoin[s]?|to\\s+shop|shop\\s+now|apply\\s+now|" +
            "claim\\s+now|avail\\s+now|order\\s+now|pre-?approved|personal\\s+loan|credit\\s+limit|" +
            "instant\\s+loan|upgrade\\s+your\\s+card|exclusive\\s+offer|special\\s+offer|festive\\s+offer|" +
            "mega\\s+sale|spin\\s+to\\s+win|win\\s+code|use\\s+code|flat\\s+\\d+%\\s+off|" +
            "save\\s+up\\s+to|valid\\s+till|expires\\s+(?:on|soon)|power\\s+cash|bonus\\s+cash|" +
            "brand\\s+cash|loyalty\\s+cash|promotional\\s+cash|store\\s+credits?|reward\\s+wallet|" +
            "cash\\s+wallet|access\\s+your\\s+wallet|check\\s+your\\s+wallet|use\\s+your\\s+wallet)\\b")
    private val brandWalletRegex =
        Regex("(?i)\\b(?:credited\\s+to|added\\s+to)\\s+your\\s+([a-zA-Z\\s]+?)\\s+wallet\\b")
    private val bankIndicatorsRegex =
        Regex("(?i)\\b(a/c|account|ending|card|\\*+|x+|upi\\s+ref|utr|neft|imps|rtgs)\\b")
    private val winCongratRegex = Regex("(?i)\\b(congratulations|congrats|won|win)\\b")
    private val reminderRegex =
        Regex("(?i)\\b(reminder|due date|minimum due|overdue)\\b")
    private val failedRegex =
        Regex("(?i)\\b(declined|failed|insufficient funds|unsuccessful)\\b")
    private val mandateRegex =
        Regex("(?i)\\bmandate\\b.*\\bregistered\\b")

    fun filter(sms: RawSms): ParseResult.Ignored? {
        val body = sms.body

        // 1. OTP
        if (otpRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.OTP)
        }

        // 2. Promotional offers, coupons, shopping vouchers, brand cash wallets, loans
        if (promoKeywordsRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.PROMO)
        }

        if (brandWalletRegex.containsMatchIn(body) && !bankIndicatorsRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.PROMO)
        }

        val bodyLower = body.lowercase()
        val hasTransactionKeyword =
            bodyLower.contains("debited") || bodyLower.contains("credited") ||
                bodyLower.contains("spent") || bodyLower.contains("withdrawn") ||
                bodyLower.contains("transferred") || bodyLower.contains("transfer") ||
                bodyLower.contains("paid") || bodyLower.contains("sent") ||
                bodyLower.contains("deducted") || bodyLower.contains("charged") ||
                bodyLower.contains("received") || bodyLower.contains("deposited") ||
                bodyLower.contains("refunded")

        if (winCongratRegex.containsMatchIn(body) && !hasTransactionKeyword) {
            return ParseResult.Ignored(IgnoreReason.PROMO)
        }

        // 3. Reminder
        if (reminderRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.REMINDER)
        }

        // 4. Failed
        if (failedRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.FAILED)
        }

        // 5. Enquiry (Balance check only)
        if ((bodyLower.contains("balance") || bodyLower.contains("bal")) && !hasTransactionKeyword) {
            return ParseResult.Ignored(IgnoreReason.ENQUIRY)
        }

        // 6. Registration/Mandate
        if (mandateRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.NOT_A_TRANSACTION)
        }

        return null
    }
}
