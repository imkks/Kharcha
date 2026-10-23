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
}
