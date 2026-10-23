package com.kharcha.engine.parser

import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.RawSms

object NoiseFilter {
    fun filter(sms: RawSms): ParseResult.Ignored? {
        val bodyLower = sms.body.lowercase()

        // 1. OTP
        if (bodyLower.contains("otp") ||
            bodyLower.contains("one time password") ||
            bodyLower.contains("do not share")
        ) {
            return ParseResult.Ignored(IgnoreReason.OTP)
        }

        // 2. Promotional
        if (bodyLower.contains("cashback") && bodyLower.contains("code") ||
            bodyLower.contains("offer") || bodyLower.contains("promo") || bodyLower.contains("win")
        ) {
            return ParseResult.Ignored(IgnoreReason.PROMO)
        }

        // 3. Reminder
        if (bodyLower.contains("reminder") || bodyLower.contains("due date") || bodyLower.contains("minimum due")) {
            return ParseResult.Ignored(IgnoreReason.REMINDER)
        }

        // 4. Failed
        if (bodyLower.contains("declined") || bodyLower.contains("failed") ||
            bodyLower.contains("insufficient funds") || bodyLower.contains("unsuccessful")
        ) {
            return ParseResult.Ignored(IgnoreReason.FAILED)
        }

        // 5. Enquiry (Balance check only)
        if ((bodyLower.contains("balance") || bodyLower.contains("bal")) &&
            !bodyLower.contains("debited") && !bodyLower.contains("credited") &&
            !bodyLower.contains("spent") && !bodyLower.contains("withdrawn")
        ) {
            return ParseResult.Ignored(IgnoreReason.ENQUIRY)
        }

        // 6. Registration/Mandate
        if (bodyLower.contains("mandate") && bodyLower.contains("registered")) {
            return ParseResult.Ignored(IgnoreReason.NOT_A_TRANSACTION)
        }

        return null
    }
}
