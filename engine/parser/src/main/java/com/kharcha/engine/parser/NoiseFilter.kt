package com.kharcha.engine.parser

import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.RawSms

object NoiseFilter {
    private val otpRegex = Regex("(?i)\\b(otp|one time password)\\b|do not share")
    private val promoRegex =
        Regex("(?i)\\b(offer|promo|win|cashback|voucher|discount|congratulations)\\b")
    private val promoCodeRegex = Regex("(?i)\\bcode\\b")
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

        // 2. Promotional: require promo words or explicit promo code offers
        if ((promoRegex.containsMatchIn(body) && promoCodeRegex.containsMatchIn(body)) ||
            Regex("(?i)\\b(win code|use code|flat \\d+% off|cashback on your next)\\b").containsMatchIn(body) ||
            Regex("(?i)\\b(exclusive offer|special offer|promo code)\\b").containsMatchIn(body)
        ) {
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
        val bodyLower = body.lowercase()
        val hasTransactionKeyword =
            bodyLower.contains("debited") || bodyLower.contains("credited") ||
                bodyLower.contains("spent") || bodyLower.contains("withdrawn") ||
                bodyLower.contains("transferred") || bodyLower.contains("transfer") ||
                bodyLower.contains("paid") || bodyLower.contains("sent") ||
                bodyLower.contains("deducted") || bodyLower.contains("charged") ||
                bodyLower.contains("received") || bodyLower.contains("deposited") ||
                bodyLower.contains("refunded")

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
