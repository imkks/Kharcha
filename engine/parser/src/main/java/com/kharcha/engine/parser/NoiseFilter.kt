package com.kharcha.engine.parser

import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.RawSms

object NoiseFilter {
    private val completedMoneyVerbRegex =
        Regex(
            """\b(debited|deducted|credited|deposited|withdrawn|spent|received|transferred|""" +
                """sent|charged|used|refunded|reversed|paid|purchased)\b""",
            RegexOption.IGNORE_CASE,
        )

    private val otpRegex =
        Regex("(?i)\\b(otp|one[-\\s]?time password|verification code)\\b|do not share")

    private val promoKeywordsRegex =
        Regex(
            "(?i)\\b(coupon[s]?|voucher[s]?|discount[s]?|promo\\b|promo\\s*code|coupon\\s*code|" +
                "voucher\\s*code|reward\\s*points?|supercoin[s]?|to\\s+shop|shop\\s+now|apply\\s+now|" +
                "claim\\s+now|avail\\s+now|order\\s+now|pre-?approved|personal\\s+loan|credit\\s+limit|" +
                "instant\\s+loan|upgrade\\s+your\\s+card|exclusive\\s+offer|special\\s+offer|festive\\s+offer|" +
                "mega\\s+sale|spin\\s+to\\s+win|win\\s+code|use\\s+code|flat\\s+\\d+%\\s+off|" +
                "save\\s+up\\s+to|valid\\s+till|expires\\s+(?:on|soon)|power\\s+cash|bonus\\s+cash|" +
                "brand\\s+cash|loyalty\\s+cash|promotional\\s+cash|store\\s+credits?|reward\\s+wallet|" +
                "cash\\s+wallet|access\\s+your\\s+wallet|check\\s+your\\s+wallet|use\\s+your\\s+wallet)\\b",
        )

    private val brandWalletRegex =
        Regex("(?i)\\b(?:credited\\s+to|added\\s+to)\\s+your\\s+([a-zA-Z\\s]+?)\\s+wallet\\b")
    private val bankIndicatorsRegex =
        Regex("(?i)\\b(a/c|account|ending|card|\\*+|x+|upi\\s+ref|utr|neft|imps|rtgs)\\b")
    private val winCongratRegex = Regex("(?i)\\b(congratulations|congrats|won|win\\b)\\b")
    private val reminderRegex =
        Regex("(?i)\\b(reminder|due date|minimum due|min amount due|minimum amount due|overdue|in arrears|ignore if paid|pls pay)\\b")
    private val statementAlertRegex =
        Regex(
            "(?i)\\b(?:credit\\s+card|card)\\s+[x\\*\\d]+\\s+statement\\s+is\\s+sent\\b|" +
                "\\bstatement\\s+is\\s+sent\\s+to\\b|" +
                "\\b(?:e-?statement|monthly\\s+statement)\\b.*\\b(?:sent|generated|due)\\b|" +
                "\\btotal\\s+(?:of\\s+)?(?:rs\\.?|inr|₹)?\\s*[\\d,]+\\.?\\d*\\s+or\\s+minimum\\s+" +
                "(?:of\\s+)?(?:rs\\.?|inr|₹)?\\s*[\\d,]+\\.?\\d*\\s+is\\s+due\\b",
        )
    private val brokerPayoutRegex =
        Regex(
            "(?i)\\b(?:quarterly\\s+settlement\\s+payout|funds\\s+payout)\\b.*" +
                "\\b(?:processed|credited\\s+to\\s+your\\s+primary\\s+bank)\\b",
        )
    private val preDebitMandateRegex =
        Regex(
            "(?i)\\b(e-?mandate!?|mandate\\s+(?:alert|notification|details?)|" +
                "pre-?debit\\s+(?:alert|notification)|auto-?debit\\s+scheduled|upi-mandate)\\b",
        )
    private val futureDebitRegex =
        Regex(
            "(?i)\\b(?:will\\s+be\\s+(?:deducted|debited|auto-?debited)|scheduled\\s+(?:for|to\\s+be)\\s+(?:deducted|debited)|" +
                "to\\s+be\\s+debited\\s+on|mandate\\s+set\\s+for|upcoming\\s+mandate)\\b",
        )
    private val maintainBalanceRegex =
        Regex("(?i)\\bmaintain\\s+(?:sufficient\\s+)?(?:balance|funds)\\b")
    private val futureCreditRefundRegex =
        Regex(
            "(?i)\\b(?:initiated\\s+for\\s+refund|refund\\s+(?:has\\s+been\\s+)?initiated|" +
                "will\\s+be\\s+credited|credited\\s+back\\s+to\\s+you|" +
                "credited\\s+(?:back\\s+)?within\\s+\\d+\\s+working\\s+days)\\b",
        )
    private val spamScamRegex =
        Regex(
            "(?i)\\b(?:warning:?\\s*spam|airtel\\s+warning|jio\\s+warning|vi\\s+warning|" +
                "work\\s+(?:from|through)\\s+home|part\\s*time\\s+job|" +
                "earn\\s+(?:daily|online|money\\s+from\\s+home)|" +
                "api\\.whatsapp\\.com|wa\\.me/|t\\.me/)\\b",
        )
    private val insuranceClaimRegex =
        Regex(
            "(?i)\\b(?:settlement\\s+of\\s+your\\s+claim|claim\\s+\\d+\\s+for\\s+a\\s+policy|" +
                "from\\s+your\\s+insurer|policy\\s+period|medi\\s*assist|insurance\\s+claim|" +
                "health\\s+insurance\\s+claim)\\b",
        )
    private val telecomExpiryRegex =
        Regex(
            "(?i)\\b(?:plan|pack|validity)\\s+(?:has\\s+)?expir(?:ed|es)|" +
                "\\b(?:recharge\\s+(?:now|with|to)|enjoy\\s+uninterrupted)\\b|" +
                "\\b(?:recharge\\s+(?:of|for)\\s+(?:rs\\.?|inr|₹)?\\s*\\d+\\s+is\\s+due)\\b",
        )
    private val failedRegex =
        Regex(
            "(?i)\\b(declined|failed|insufficient funds|unsuccessful|failed transaction|payment failed|inward return|unsuccessful transaction)\\b",
        )
    private val mandateRegex =
        Regex("(?i)\\bmandate\\b.*\\b(?:registered|successfully created)\\b")
    private val serviceWelcomeRegex =
        Regex(
            "(?i)\\b(?:welcome\\s+to\\s+[a-zA-Z0-9\\s]+!.*(?:activated|is\\s+now\\s+active)|" +
                "wi-?fi\\s+id\\s+[a-zA-Z0-9_]+\\s+is\\s+now\\s+activated|" +
                "plan\\s+details\\s+are:?|" +
                "connection\\s+type\\s*:|" +
                "plan\\s+rental\\s*:|" +
                "advance\\s+amount\\s+paid.*adjusted\\s+in\\s+upcoming|" +
                "adjusted\\s+in\\s+upcoming\\s+monthly\\s+bills)\\b",
        )
    private val merchantInvoiceRegex =
        Regex("""(?i)\b(?:download|view)\s+(?:your\s+)?(?:bill|invoice|receipt|e-bill)\b""")

    fun filter(sms: RawSms): ParseResult.Ignored? {
        val body = sms.body
        val bodyLower = body.lowercase()

        // Merchant invoice / receipt download links (Apollo Pharmacy, etc.)
        if (merchantInvoiceRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.NOT_A_TRANSACTION)
        }

        // Service activation / Broadband / Wi-Fi onboarding notice
        if (serviceWelcomeRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.NOT_A_TRANSACTION)
        }

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

        val hasTransactionKeyword = completedMoneyVerbRegex.containsMatchIn(bodyLower)

        if (winCongratRegex.containsMatchIn(body) && !hasTransactionKeyword) {
            return ParseResult.Ignored(IgnoreReason.PROMO)
        }

        // PennyWise: payment request / collect request
        if (bodyLower.contains("has requested") ||
            bodyLower.contains("payment request") ||
            bodyLower.contains("collect request") ||
            bodyLower.contains("requesting payment") ||
            bodyLower.contains("requests rs") ||
            bodyLower.contains("ignore if already paid")
        ) {
            return ParseResult.Ignored(IgnoreReason.REMINDER)
        }

        // PennyWise: merchant payment acknowledgment ("have received payment")
        if (bodyLower.contains("have received payment")) {
            return ParseResult.Ignored(IgnoreReason.NOT_A_TRANSACTION)
        }

        // PennyWise: ASBA/IPO fund blocking
        if (bodyLower.contains("asba") || bodyLower.contains("is blocked in your")) {
            return ParseResult.Ignored(IgnoreReason.NON_FINANCIAL)
        }

        // PennyWise: reward/gift voucher delivery notices without debit verbs
        if ((bodyLower.contains("e-voucher") || bodyLower.contains("evoucher")) &&
            (bodyLower.contains("received") || bodyLower.contains("reward") || bodyLower.contains("redemption")) &&
            !bodyLower.contains("spent") && !bodyLower.contains("debited") && !bodyLower.contains("charged")
        ) {
            return ParseResult.Ignored(IgnoreReason.NOT_A_TRANSACTION)
        }

        // PennyWise: convert-bill-to-EMI offers
        if (bodyLower.contains("convert") &&
            (bodyLower.contains("smartemi") || bodyLower.contains("into emi")) &&
            !Regex("""\b(debited|deducted|spent|charged|withdrawn)\b""").containsMatchIn(bodyLower)
        ) {
            return ParseResult.Ignored(IgnoreReason.PROMO)
        }

        // PennyWise: auto-debit intimations ("will be auto-debited")
        if (bodyLower.contains("will be auto-debited") || bodyLower.contains("will be auto debited")) {
            return ParseResult.Ignored(IgnoreReason.REMINDER)
        }

        // PennyWise: bill-due reminders ("due on" without completed money verb)
        if (bodyLower.contains("due on") && !hasTransactionKeyword) {
            return ParseResult.Ignored(IgnoreReason.REMINDER)
        }

        val isPastTransaction = hasTransactionKeyword

        // 3. Reminder, Statement Alerts & Pre-debit e-mandate notices
        if ((reminderRegex.containsMatchIn(body) && !isPastTransaction) ||
            statementAlertRegex.containsMatchIn(body) ||
            brokerPayoutRegex.containsMatchIn(body) ||
            preDebitMandateRegex.containsMatchIn(body) ||
            futureDebitRegex.containsMatchIn(body) ||
            maintainBalanceRegex.containsMatchIn(body) ||
            (telecomExpiryRegex.containsMatchIn(body) && !hasTransactionKeyword) ||
            (bodyLower.contains("mandate") && (bodyLower.contains("umn") || bodyLower.contains("will be")))
        ) {
            return ParseResult.Ignored(IgnoreReason.REMINDER)
        }

        // 4. Failed / Declined
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

        // 7. Spam, scam job offers, WhatsApp links
        if (spamScamRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.PROMO)
        }

        // 8. Insurance claim / TPA policy settlement notices
        if (insuranceClaimRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.NOT_A_TRANSACTION)
        }

        // 9. Future refund initiated notices
        if (futureCreditRefundRegex.containsMatchIn(body)) {
            return ParseResult.Ignored(IgnoreReason.REMINDER)
        }

        return null
    }
}
