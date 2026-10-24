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

    @Test
    fun testSbiUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 11,
                sender = "VM-SBIUPI",
                body = "Dear UPI user A/C 1234 debited by 500.0 on 24Oct24 tran id 429812345678 to Swiggy",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(50000L, txn.amountMinor)
        assertEquals("SBI", txn.bankName)
        assertEquals("1234", txn.accountLast4)
        assertEquals("sbi_upi_debit_v1", txn.templateId)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testSbiCreditTemplate() {
        val sms =
            RawSms(
                id = 12,
                sender = "VM-SBINB",
                body = "Your A/C ending 4321 credited by Rs 15,000.00 on 24Oct24 by NEFT transfer from ABC Corp.",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(1500000L, txn.amountMinor)
        assertEquals("SBI", txn.bankName)
        assertEquals("4321", txn.accountLast4)
        assertEquals("sbi_credit_v1", txn.templateId)
        assertEquals(Channel.NEFT, txn.channel)
        assertEquals(Direction.CREDIT, txn.direction)
    }

    @Test
    fun testHdfcCardSpendTemplate() {
        val sms =
            RawSms(
                id = 13,
                sender = "AD-HDFCBK",
                body = "Rs. 1,499.00 spent on HDFC Bank Credit Card ending 9876 at Amazon on 24-10-24.",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(149900L, txn.amountMinor)
        assertEquals("HDFC", txn.bankName)
        assertEquals("9876", txn.accountLast4)
        assertEquals("Amazon", txn.merchantRaw)
        assertEquals("hdfc_card_spend_v1", txn.templateId)
        assertEquals(Channel.CARD, txn.channel)
    }

    @Test
    fun testAxisCardSpendTemplate() {
        val sms =
            RawSms(
                id = 14,
                sender = "VK-AXISBK",
                body = "INR 2,850.00 spent on Axis Bank Credit Card ending with 3456 at Flipkart on 24-10-24.",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(285000L, txn.amountMinor)
        assertEquals("AXIS", txn.bankName)
        assertEquals("3456", txn.accountLast4)
        assertEquals("Flipkart", txn.merchantRaw)
        assertEquals("axis_card_spend_v1", txn.templateId)
        assertEquals(Channel.CARD, txn.channel)
    }

    @Test
    fun testAxisUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 15,
                sender = "VK-AXISBK",
                body = "INR 350.00 debited from Axis Bank A/c ending 1122 on 24-10-24 via UPI to ChaiPoint.",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(35000L, txn.amountMinor)
        assertEquals("AXIS", txn.bankName)
        assertEquals("1122", txn.accountLast4)
        assertEquals("axis_upi_debit_v1", txn.templateId)
        assertEquals(Channel.UPI, txn.channel)
    }

    @Test
    fun testKotakUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 16,
                sender = "VM-KOTAKB",
                body =
                    "Sent Rs.1,500.00 from Kotak Bank AC *1234 to swiggy@icici on 24-10-24. " +
                        "UPI Ref 429812345678. Bal: Rs.25000.50",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(150000L, txn.amountMinor)
        assertEquals("KOTAK", txn.bankName)
        assertEquals("1234", txn.accountLast4)
        assertEquals("swiggy@icici", txn.merchantRaw)
        assertEquals("kotak_upi_debit_v1", txn.templateId)
        assertEquals(2500050L, txn.balanceAfterMinor)
    }

    @Test
    fun testBobUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 17,
                sender = "BP-BOBTXN",
                body =
                    "Your A/C XXXXXX5678 is debited for Rs.750.00 on 24-10-2024 " +
                        "by UPI/P2M/zomato@axis/429812345678. Avl Bal: Rs.15000.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(75000L, txn.amountMinor)
        assertEquals("BOB", txn.bankName)
        assertEquals("5678", txn.accountLast4)
        assertEquals("zomato@axis", txn.merchantRaw)
        assertEquals("bob_upi_debit_v1", txn.templateId)
    }

    @Test
    fun testPnbDebitTemplate() {
        val sms =
            RawSms(
                id = 18,
                sender = "CP-PNBSMS",
                body =
                    "Dear Customer, A/c *1234 debited with INR 1,200.00 on 24-10-24 " +
                        "towards Amazon. Avl Bal INR 18,500.00.",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(120000L, txn.amountMinor)
        assertEquals("PNB", txn.bankName)
        assertEquals("1234", txn.accountLast4)
        assertEquals("Amazon", txn.merchantRaw)
        assertEquals("pnb_debit_v1", txn.templateId)
    }

    @Test
    fun testCanaraDebitTemplate() {
        val sms =
            RawSms(
                id = 19,
                sender = "CP-CANBNK",
                body =
                    "Your A/C ...1234 debited by Rs.2,300.00 on 24-10-2024 14:30:15 " +
                        "via UPI Ref 429812345678 to Uber. Bal: Rs.10,200.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(230000L, txn.amountMinor)
        assertEquals("CANARA", txn.bankName)
        assertEquals("1234", txn.accountLast4)
        assertEquals("Uber", txn.merchantRaw)
        assertEquals("canara_debit_v1", txn.templateId)
    }

    @Test
    fun testIdfcUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 20,
                sender = "JD-IDFCFB",
                body =
                    "Rs. 850.00 debited from IDFC FIRST Bank A/C ending 4321 on 24-Oct-24 to blinkit@icici. " +
                        "UPI Ref: 429812345678. Available balance: Rs. 45000.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(85000L, txn.amountMinor)
        assertEquals("IDFC", txn.bankName)
        assertEquals("4321", txn.accountLast4)
        assertEquals("blinkit@icici", txn.merchantRaw)
        assertEquals("idfc_upi_debit_v1", txn.templateId)
    }

    @Test
    fun testIndusIndDebitTemplate() {
        val sms =
            RawSms(
                id = 21,
                sender = "JD-INDBNK",
                body =
                    "Your IndusInd Bank A/c ending 9876 has been debited by INR 3,450.00 on 24-10-2024 " +
                        "towards Flipkart. Avl Bal: INR 32,000.00.",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(345000L, txn.amountMinor)
        assertEquals("INDUSIND", txn.bankName)
        assertEquals("9876", txn.accountLast4)
        assertEquals("Flipkart", txn.merchantRaw)
        assertEquals("indusind_debit_v1", txn.templateId)
    }

    @Test
    fun testFederalDebitTemplate() {
        val sms =
            RawSms(
                id = 22,
                sender = "AD-FEDBNK",
                body =
                    "Rs. 650.00 debited from Federal Bank A/C ending 6789 on 24-10-2024 16:45 via UPI " +
                        "to starbucks@icici. Ref: 429812345678. Bal: Rs. 14,000.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(65000L, txn.amountMinor)
        assertEquals("FEDERAL", txn.bankName)
        assertEquals("6789", txn.accountLast4)
        assertEquals("starbucks@icici", txn.merchantRaw)
        assertEquals("federal_debit_v1", txn.templateId)
    }

    @Test
    fun testYesBankDebitTemplate() {
        val sms =
            RawSms(
                id = 23,
                sender = "AD-YESBNK",
                body =
                    "Sent Rs. 990.00 from YES BANK A/C **5544 to tatacliq@hdfc on 24-10-24. " +
                        "UPI Ref: 429812345678. Avl Bal: Rs. 21,300.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(99000L, txn.amountMinor)
        assertEquals("YESBANK", txn.bankName)
        assertEquals("5544", txn.accountLast4)
        assertEquals("tatacliq@hdfc", txn.merchantRaw)
        assertEquals("yes_bank_debit_v1", txn.templateId)
    }

    @Test
    fun testOneCardSpendTemplate() {
        val sms =
            RawSms(
                id = 24,
                sender = "VK-ONECRD",
                body =
                    "You spent Rs. 1,890.00 on your OneCard ending with 1234 at Apple Store on 24-10-2024. " +
                        "Available limit: Rs. 98,000.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(189000L, txn.amountMinor)
        assertEquals("ONECARD", txn.bankName)
        assertEquals("1234", txn.accountLast4)
        assertEquals("Apple Store", txn.merchantRaw)
        assertEquals("onecard_spend_v1", txn.templateId)
    }

    @Test
    fun testSliceSpendTemplate() {
        val sms =
            RawSms(
                id = 25,
                sender = "VK-SLICEC",
                body = "Spent Rs. 420.00 at Chaayos via slice card ending 8899 on 24-10-24.",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(42000L, txn.amountMinor)
        assertEquals("SLICE", txn.bankName)
        assertEquals("8899", txn.accountLast4)
        assertEquals("Chaayos", txn.merchantRaw)
        assertEquals("slice_spend_v1", txn.templateId)
    }
}
