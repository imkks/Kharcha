package com.kharcha.engine.parser

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.RawSms
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SmsParserTest {
    private lateinit var parser: SmsParserImpl
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        val templatesDir = File("src/main/assets/templates")
        val fallbackDir = File("engine/parser/src/main/assets/templates")
        val templates = mutableListOf<ParserTemplate>()

        val dir =
            if (templatesDir.exists() && templatesDir.isDirectory) {
                templatesDir
            } else if (fallbackDir.exists() && fallbackDir.isDirectory) {
                fallbackDir
            } else {
                null
            }

        dir?.listFiles { file -> file.extension == "json" }?.forEach { file ->
            val template = json.decodeFromString<ParserTemplate>(file.readText())
            templates.add(template)
        }

        parser = SmsParserImpl(templates)
    }

    @Test
    fun testExample1UPIDebit() {
        val sms =
            RawSms(
                id = 1,
                sender = "XX-ICICI",
                body = "Rs 450.00 debited from A/c XX1234 on 22-10-26 to VPA swiggy@icici (UPI Ref 6128...)",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn

        assertEquals(45000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("1234", txn.accountLast4)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals("swiggy@icici", txn.upiId)
    }

    @Test
    fun testExample2NEFTCredit() {
        val sms =
            RawSms(
                id = 2,
                sender = "VM-SBIUPI",
                body = "INR 85,000.00 credited to A/c XX1234 on 01-10-26 by NEFT from ACME TECH PVT LTD SAL",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn

        assertEquals(8500000L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("1234", txn.accountLast4)
        assertEquals(Channel.NEFT, txn.channel)
        assertEquals("ACME TECH PVT LTD SAL", txn.merchantRaw)
    }

    @Test
    fun testExample3OTPIgnored() {
        val sms =
            RawSms(
                id = 3,
                sender = "XX-BANK",
                body = "Your OTP for transaction is 482913. Do not share.",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Ignored)
        assertEquals(IgnoreReason.OTP, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun testExample4CreditCardSpend() {
        val sms =
            RawSms(
                id = 4,
                sender = "XX-HDFCBK",
                body = "Rs 1,299 spent on Credit Card XX9876 at AMAZON on 20-Oct. Avl limit Rs 48,701.",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn

        assertEquals(129900L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals("9876", txn.accountLast4)
        assertEquals("AMAZON", txn.merchantRaw)
        assertEquals(4870100L, txn.balanceAfterMinor)
    }

    @Test
    fun testExample6DeclinedIgnored() {
        val sms =
            RawSms(
                id = 6,
                sender = "XX-BANK",
                body = "Transaction of Rs 600 declined due to insufficient funds.",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Ignored)
        assertEquals(IgnoreReason.FAILED, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun testExample7PromoIgnored() {
        val sms =
            RawSms(
                id = 7,
                sender = "XX-BANK",
                body = "Get 5 percent cashback on your next recharge! Use code SAVE5.",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Ignored)
        assertEquals(IgnoreReason.PROMO, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun testExample8ATMWithdrawal() {
        val sms =
            RawSms(
                id = 8,
                sender = "XX-BANK",
                body = "Rs 2,000 withdrawn at ATM on 18-10-26. Avl Bal Rs 12,340.",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn

        assertEquals(200000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals(Channel.ATM, txn.channel)
        assertEquals(1234000L, txn.balanceAfterMinor)
    }

    @Test
    fun testFallbackGenericParser() {
        val sms =
            RawSms(
                id = 9,
                sender = "XX-UNKNOWN",
                body = "Rs 500 debited from A/C 9999",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn

        assertEquals(50000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("9999", txn.accountLast4)
    }

    @Test
    fun testFallbackGenericParserWithRupeeSymbolAndTransfer() {
        val transferSms =
            RawSms(
                id = 10,
                sender = "XX-HDFCBK",
                body = "₹2,500.00 transferred to A/c ending 4321 on 24-Oct-26",
                receivedAt = 1698300000000L,
                subId = null,
            )

        val result = parser.parse(transferSms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn

        assertEquals(250000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("4321", txn.accountLast4)
    }
}
