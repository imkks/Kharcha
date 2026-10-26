package com.kharcha.engine.parser

import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.RawSms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseFilterTest {
    @Test
    fun doesNotIgnoreLegitimateTransactionWithWithin() {
        // "within 24 hours" must not be ignored as promo due to "win"
        val sms =
            RawSms(
                id = 1,
                sender = "XX-HDFCBK",
                body = "Rs 500 debited from a/c 1234. If not done by you call 1800 within 24 hours.",
                receivedAt = 1000L,
                subId = null,
            )
        assertNull(NoiseFilter.filter(sms))
    }

    @Test
    fun doesNotIgnoreWordsContainingOtpSubstring() {
        val sms =
            RawSms(
                id = 2,
                sender = "XX-HDFCBK",
                body = "Rs 1200 spent at Hotpot restaurant on card XX1234.",
                receivedAt = 1000L,
                subId = null,
            )
        assertNull(NoiseFilter.filter(sms))
    }

    @Test
    fun ignoresRealOtp() {
        val sms =
            RawSms(
                id = 3,
                sender = "XX-HDFCBK",
                body = "Your OTP is 492104 for online purchase. Do not share.",
                receivedAt = 1000L,
                subId = null,
            )
        val result = NoiseFilter.filter(sms)
        assertNotNull(result)
        assertTrue(result is ParseResult.Ignored)
        assertEquals(IgnoreReason.OTP, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun ignoresRealPromo() {
        val sms =
            RawSms(
                id = 4,
                sender = "XX-HDFCBK",
                body = "Get flat 50% discount! Use code SAVE50 on your next recharge.",
                receivedAt = 1000L,
                subId = null,
            )
        val result = NoiseFilter.filter(sms)
        assertNotNull(result)
        assertEquals(IgnoreReason.PROMO, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun ignoresFailedTransaction() {
        val sms =
            RawSms(
                id = 5,
                sender = "XX-HDFCBK",
                body = "Txn of Rs 500 declined due to insufficient funds.",
                receivedAt = 1000L,
                subId = null,
            )
        val result = NoiseFilter.filter(sms)
        assertNotNull(result)
        assertEquals(IgnoreReason.FAILED, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun ignoresReminder() {
        val sms =
            RawSms(
                id = 6,
                sender = "XX-HDFCBK",
                body = "Reminder: Your credit card payment minimum due is Rs 2000 by 25-Oct.",
                receivedAt = 1000L,
                subId = null,
            )
        val result = NoiseFilter.filter(sms)
        assertNotNull(result)
        assertEquals(IgnoreReason.REMINDER, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun doesNotIgnoreTransferOrPaymentWithBalance() {
        val transferSms =
            RawSms(
                id = 7,
                sender = "XX-HDFCBK",
                body = "HDFC Bank: Rs 2000 transferred to a/c ending 5678 on 24-OCT-26. Avl Bal Rs 15,000",
                receivedAt = 1000L,
                subId = null,
            )
        assertNull(NoiseFilter.filter(transferSms))

        val paidSms =
            RawSms(
                id = 8,
                sender = "XX-SBIBNK",
                body = "Paid Rs 350 on Paytm using SBI UPI. Avl Bal Rs 5000",
                receivedAt = 1000L,
                subId = null,
            )
        assertNull(NoiseFilter.filter(paidSms))
    }

    @Test
    fun ignoresCouponAndVoucherOffers() {
        val couponSms =
            RawSms(
                id = 9,
                sender = "XX-TATACL",
                body = "Tata CliQ has added 2000 coupon to shop for a watch",
                receivedAt = 1000L,
                subId = null,
            )
        val result = NoiseFilter.filter(couponSms)
        assertNotNull(result)
        assertEquals(IgnoreReason.PROMO, (result as ParseResult.Ignored).reason)

        val voucherSms =
            RawSms(
                id = 10,
                sender = "XX-MYNTRA",
                body = "Congratulations! Rs. 500 voucher added to your account to shop on Myntra.",
                receivedAt = 1000L,
                subId = null,
            )
        val voucherResult = NoiseFilter.filter(voucherSms)
        assertNotNull(voucherResult)
        assertEquals(IgnoreReason.PROMO, (voucherResult as ParseResult.Ignored).reason)
    }

    @Test
    fun ignoresCreditLimitAndLoanOffers() {
        val limitSms =
            RawSms(
                id = 11,
                sender = "XX-HDFCBK",
                body = "Your credit limit has been increased to Rs. 2,00,000 on your HDFC card.",
                receivedAt = 1000L,
                subId = null,
            )
        val result = NoiseFilter.filter(limitSms)
        assertNotNull(result)
        assertEquals(IgnoreReason.PROMO, (result as ParseResult.Ignored).reason)

        val loanSms =
            RawSms(
                id = 12,
                sender = "XX-AXISBK",
                body = "Pre-approved personal loan of Rs 5,00,000 is ready for you. Apply now!",
                receivedAt = 1000L,
                subId = null,
            )
        val loanResult = NoiseFilter.filter(loanSms)
        assertNotNull(loanResult)
        assertEquals(IgnoreReason.PROMO, (loanResult as ParseResult.Ignored).reason)
    }

    @Test
    fun ignoresBrandWalletAndPowerCashOffers() {
        val tissotSms =
            RawSms(
                id = 13,
                sender = "XX-TISSOT",
                body = "Rs.2000 has been credited to your Tissot Power Cash Wallet. Access your wallet here: ",
                receivedAt = 1000L,
                subId = null,
            )
        val result = NoiseFilter.filter(tissotSms)
        assertNotNull(result)
        assertEquals(IgnoreReason.PROMO, (result as ParseResult.Ignored).reason)

        val brandWalletSms =
            RawSms(
                id = 14,
                sender = "XX-LNSKRT",
                body = "Rs. 500 has been credited to your Lenskart wallet.",
                receivedAt = 1000L,
                subId = null,
            )
        val brandResult = NoiseFilter.filter(brandWalletSms)
        assertNotNull(brandResult)
        assertEquals(IgnoreReason.PROMO, (brandResult as ParseResult.Ignored).reason)
    }

    @Test
    fun ignoresPreDebitMandateNotices() {
        val mandateSms =
            RawSms(
                id = 15,
                sender = "XX-HDFCBK",
                body =
                    "E-Mandate!\nRs.199.00 will be deducted on 30/09/26, 00:00:00\n" +
                        "For NETFLIX COM mandate\nUMN 151aa7f6a9d248c4ba545e5b303eefc6@ptyes\n" +
                        "Maintain Balance\n-HDFC Bank",
                receivedAt = 1000L,
                subId = null,
            )
        val result = NoiseFilter.filter(mandateSms)
        assertNotNull(result)
        assertEquals(IgnoreReason.REMINDER, (result as ParseResult.Ignored).reason)

        val autoDebitNotice =
            RawSms(
                id = 16,
                sender = "XX-ICICIB",
                body = "Pre-debit notification: Rs. 1500 will be deducted from a/c XX1234 on 05-Nov for SIP.",
                receivedAt = 1000L,
                subId = null,
            )
        val autoDebitResult = NoiseFilter.filter(autoDebitNotice)
        assertNotNull(autoDebitResult)
        assertEquals(IgnoreReason.REMINDER, (autoDebitResult as ParseResult.Ignored).reason)
    }

    @Test
    fun ignoresStatementAlertsAndBrokerPayouts() {
        val statementSms =
            RawSms(
                id = 17,
                sender = "XX-ICICIB",
                body = "ICICI Bank Credit Card XX4007 Statement is sent to im*****07@gmail.com. " +
                    "Total of Rs 13,746.00 or minimum of Rs 690.00 is due by 03-NOV-26.",
                receivedAt = 1000L,
                subId = null,
            )
        val statementResult = NoiseFilter.filter(statementSms)
        assertNotNull(statementResult)
        assertEquals(IgnoreReason.REMINDER, (statementResult as ParseResult.Ignored).reason)

        val payoutSms =
            RawSms(
                id = 18,
                sender = "XX-ZERODH",
                body = "Your quarterly settlement payout for Rs.16891.59 for Equity a/c (WW6697) is processed " +
                    "and credited to your primary bank a/c - Zerodha",
                receivedAt = 1000L,
                subId = null,
            )
        val payoutResult = NoiseFilter.filter(payoutSms)
        assertNotNull(payoutResult)
        assertEquals(IgnoreReason.REMINDER, (payoutResult as ParseResult.Ignored).reason)
    }
}
