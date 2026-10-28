package com.kharcha.engine.parser

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.RawSms
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
                sender = "XX-HDFCBK",
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
        assertTrue("Unknown non-bank sender must not be auto-ingested", result is ParseResult.NoMatch)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertEquals("swiggy", txn.merchantRaw)
        assertEquals("swiggy@icici", txn.upiId)
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
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
        assertNotNull(txn.templateId)
    }

    @Test
    fun testCouponOffersAreIgnoredNotIncome() {
        val sms =
            RawSms(
                id = 26,
                sender = "XX-TATACL",
                body = "Tata CliQ has added 2000 coupon to shop for a watch",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Ignored)
        assertEquals(IgnoreReason.PROMO, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun testIciciCardSpendTemplate() {
        val sms =
            RawSms(
                id = 27,
                sender = "VM-ICICIB",
                body =
                    "ICICI Bank Card XX1005 has been used for a transaction of INR 1,299.00 on 24-Oct-24 " +
                        "at AMAZON. Available limit: INR 45,000.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(129900L, txn.amountMinor)
        assertEquals("ICICI", txn.bankName)
        assertEquals("1005", txn.accountLast4)
        assertEquals("AMAZON", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testIciciDebitTemplate() {
        val sms =
            RawSms(
                id = 28,
                sender = "VM-ICICIB",
                body =
                    "Your A/C ending 4567 is debited for INR 4,500.00 on 24-Oct-24 towards BillDesk. " +
                        "Avl Bal: INR 35,400.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(450000L, txn.amountMinor)
        assertEquals("ICICI", txn.bankName)
        assertEquals("4567", txn.accountLast4)
        assertEquals("BillDesk", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testIciciCreditTemplate() {
        val sms =
            RawSms(
                id = 29,
                sender = "VM-ICICIB",
                body =
                    "Your A/C ending 4567 is credited with INR 75,000.00 on 24-Oct-24 by " +
                        "SALARY/COMPANY PVT LTD. Avl Bal: INR 1,10,400.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(7500000L, txn.amountMinor)
        assertEquals("ICICI", txn.bankName)
        assertEquals("4567", txn.accountLast4)
        assertNotNull(txn.templateId)
        assertEquals(Channel.NEFT, txn.channel)
        assertEquals(Direction.CREDIT, txn.direction)
    }

    @Test
    fun testHdfcAccountDebitTemplate() {
        val sms =
            RawSms(
                id = 30,
                sender = "AD-HDFCBK",
                body =
                    "UPDATE: INR 3,200.00 debited from HDFC Bank A/c ending 8765 on 24-Oct-24 " +
                        "towards Tata Power. Avl bal: INR 22,100.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(320000L, txn.amountMinor)
        assertEquals("HDFC", txn.bankName)
        assertEquals("8765", txn.accountLast4)
        assertEquals("Tata Power", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testHdfcCreditTemplate() {
        val sms =
            RawSms(
                id = 31,
                sender = "AD-HDFCBK",
                body =
                    "INR 50,000.00 credited to HDFC Bank A/c ending 8765 on 24-Oct-24 by transfer " +
                        "from Google India. Avl bal: INR 72,100.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(5000000L, txn.amountMinor)
        assertEquals("HDFC", txn.bankName)
        assertEquals("8765", txn.accountLast4)
        assertNotNull(txn.templateId)
        assertEquals(Channel.NEFT, txn.channel)
        assertEquals(Direction.CREDIT, txn.direction)
    }

    @Test
    fun testAxisCreditTemplate() {
        val sms =
            RawSms(
                id = 32,
                sender = "VK-AXISBK",
                body =
                    "Your Axis Bank A/C ending 1122 is credited with INR 12,000.00 on 24-10-24 " +
                        "by NEFT transfer from ABC Corp. Avl Bal: INR 48,000.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(1200000L, txn.amountMinor)
        assertEquals("AXIS", txn.bankName)
        assertEquals("1122", txn.accountLast4)
        assertNotNull(txn.templateId)
        assertEquals(Channel.NEFT, txn.channel)
        assertEquals(Direction.CREDIT, txn.direction)
    }

    @Test
    fun testKotakCreditTemplate() {
        val sms =
            RawSms(
                id = 33,
                sender = "VM-KOTAKB",
                body =
                    "Your Kotak Bank AC *1234 has been credited with Rs. 25,000.00 on 24-10-24 " +
                        "by transfer from Infosys. Avl Bal: Rs. 65,000.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(2500000L, txn.amountMinor)
        assertEquals("KOTAK", txn.bankName)
        assertEquals("1234", txn.accountLast4)
        assertNotNull(txn.templateId)
        assertEquals(Direction.CREDIT, txn.direction)
    }

    @Test
    fun testPnbUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 34,
                sender = "CP-PNBSMS",
                body =
                    "Your A/C *1234 is debited by Rs.450.00 on 24-10-24 via UPI to zesty@icici. " +
                        "Ref: 429812345678. Bal: Rs.8,200.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(45000L, txn.amountMinor)
        assertEquals("PNB", txn.bankName)
        assertEquals("1234", txn.accountLast4)
        assertNotNull(txn.templateId)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testUnionBankDebitTemplate() {
        val sms =
            RawSms(
                id = 35,
                sender = "CP-UBISMS",
                body =
                    "Union Bank of India: A/C ending 7890 debited by Rs.1,100.00 on 24-10-2024 " +
                        "towards IRCTC. Avl Bal: Rs.14,500.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(110000L, txn.amountMinor)
        assertEquals("UNION", txn.bankName)
        assertEquals("7890", txn.accountLast4)
        assertEquals("IRCTC", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testJupiterSpendTemplate() {
        val sms =
            RawSms(
                id = 36,
                sender = "AD-JUPITR",
                body =
                    "Spent Rs. 380.00 via Jupiter on Federal Bank A/C ending 4567 at Starbucks on 24-10-24. " +
                        "Balance: Rs. 18,200.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(38000L, txn.amountMinor)
        assertEquals("JUPITER", txn.bankName)
        assertEquals("4567", txn.accountLast4)
        assertEquals("Starbucks", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Channel.UPI, txn.channel)
    }

    @Test
    fun testFiMoneySpendTemplate() {
        val sms =
            RawSms(
                id = 37,
                sender = "AD-FIMONY",
                body =
                    "Paid Rs. 290.00 with Fi on Federal Bank A/C ending 3322 at Zomato on 24-10-24. " +
                        "Avl Bal: Rs. 9,450.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(29000L, txn.amountMinor)
        assertEquals("FI", txn.bankName)
        assertEquals("3322", txn.accountLast4)
        assertEquals("Zomato", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Channel.UPI, txn.channel)
    }

    @Test
    fun testPaytmUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 38,
                sender = "AD-PAYTMB",
                body =
                    "Paid Rs. 150.00 to Chai Point from Paytm Payments Bank A/C ending 9876 on 24-10-2024. " +
                        "UPI Ref: 429812345678",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(15000L, txn.amountMinor)
        assertEquals("PAYTM", txn.bankName)
        assertEquals("9876", txn.accountLast4)
        assertEquals("Chai Point", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Channel.UPI, txn.channel)
    }

    @Test
    fun testAirtelBankDebitTemplate() {
        val sms =
            RawSms(
                id = 39,
                sender = "AD-AIRBNK",
                body =
                    "Rs. 249.00 debited from Airtel Payments Bank A/C ending 5543 on 24-10-24 towards " +
                        "DTH Recharge. Bal: Rs. 1,400.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(24900L, txn.amountMinor)
        assertEquals("AIRTEL", txn.bankName)
        assertEquals("5543", txn.accountLast4)
        assertEquals("DTH Recharge", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Channel.WALLET, txn.channel)
    }

    @Test
    fun testScbCardSpendTemplate() {
        val sms =
            RawSms(
                id = 40,
                sender = "AD-SCBLNK",
                body =
                    "Spent INR 3,999.00 on your Standard Chartered Card ending 6655 at Croma on 24-10-2024. " +
                        "Available limit: INR 85,000.00",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(399900L, txn.amountMinor)
        assertEquals("SCB", txn.bankName)
        assertEquals("6655", txn.accountLast4)
        assertEquals("Croma", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Channel.CARD, txn.channel)
    }

    @Test
    fun testAmexCardSpendTemplate() {
        val sms =
            RawSms(
                id = 41,
                sender = "AD-AMEXIN",
                body =
                    "You've spent INR 4,200.00 on your American Express Card ending 1004 at Taj Hotels " +
                        "on 24-10-2024.",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(420000L, txn.amountMinor)
        assertEquals("AMEX", txn.bankName)
        assertEquals("1004", txn.accountLast4)
        assertEquals("Taj Hotels", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Channel.CARD, txn.channel)
    }

    @Test
    fun testBrandPowerCashWalletIsIgnoredNotIncome() {
        val sms =
            RawSms(
                id = 42,
                sender = "XX-TISSOT",
                body = "Rs.2000 has been credited to your Tissot Power Cash Wallet. Access your wallet here: ",
                receivedAt = 1698300000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Ignored)
        assertEquals(IgnoreReason.PROMO, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun testIdfcCreditCardSpendTemplate() {
        val sms =
            RawSms(
                id = 43,
                sender = "VM-IDFCFB-S",
                body =
                    "Transaction Successful! INR 18596.70 spent on your IDFC FIRST Bank Credit Card " +
                        "ending XX5625 at RAZ*Optick Labs Inc on 27 SEP 2026 at 12:08 AM Avbl Limit: " +
                        "INR 389646.5 If not done by you, call 180010888 for dispute or to block your " +
                        "card SMS CCBLOCK 5625 to 5676732",
                receivedAt = 1790447880000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(1859670L, txn.amountMinor)
        assertEquals("IDFC", txn.bankName)
        assertEquals("5625", txn.accountLast4)
        assertEquals("RAZ*Optick Labs Inc", txn.merchantRaw)
        assertNotNull(txn.templateId)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals(38964650L, txn.balanceAfterMinor)
    }

    @Test
    fun testPreDebitEmandateNoticeIsIgnored() {
        val sms =
            RawSms(
                id = 44,
                sender = "VM-HDFCBK",
                body =
                    "E-Mandate!\nRs.199.00 will be deducted on 30/09/26, 00:00:00\n" +
                        "For NETFLIX COM mandate\nUMN 151aa7f6a9d248c4ba545e5b303eefc6@ptyes\n" +
                        "Maintain Balance\n-HDFC Bank",
                receivedAt = 1790447880000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Ignored)
        assertEquals(IgnoreReason.REMINDER, (result as ParseResult.Ignored).reason)
    }

    @Test
    fun testIciciBankCardSpendV2Template() {
        val sms =
            RawSms(
                id = 45,
                sender = "BG-ICICIT-S",
                body =
                    "INR 323.00 spent using ICICI Bank Card XX3006 on 24-Oct-26 on RSP*SWIGGY PVT . " +
                        "Avl Limit: INR 6,93,384.00. If not you, call 1800 2662/SMS BLOCK 3006 to 9215676766.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(32300L, txn.amountMinor)
        assertEquals("ICICI", txn.bankName)
        assertEquals("3006", txn.accountLast4)
        assertEquals("RSP*SWIGGY PVT", txn.merchantRaw?.trim())
        assertNotNull(txn.templateId)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals(69338400L, txn.balanceAfterMinor)
    }

    @Test
    fun testFallbackParserDoesNotParseHelplineAsMerchant() {
        val fallback = GenericFallbackParser()
        val sms =
            RawSms(
                id = 46,
                sender = "UNKNOWN-S",
                body =
                    "INR 323.00 spent on Card XX3006 on 24-Oct-26 on RSP*SWIGGY PVT . " +
                        "Avl Limit: INR 6,93,384.00. If not you, call 1800 2662/SMS BLOCK 3006 to 9215676766.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val txn = fallback.parse(sms)
        org.junit.Assert.assertNotNull(txn)
        assertEquals(32300L, txn?.amountMinor)
        assertEquals("3006", txn?.accountLast4)
        assertEquals("RSP*SWIGGY PVT", txn?.merchantRaw?.trim())
        org.junit.Assert.assertNotEquals("9215676766", txn?.merchantRaw?.trim())
    }

    @Test
    fun testSpamJobOfferIsIgnored() {
        val sms =
            RawSms(
                id = 47,
                sender = "AIRTEL",
                body =
                    "Airtel Warning: SPAM | Dear Sir/Mam, SUCCESSFUL Salary received Rs39800 credited to you, " +
                        "provide You can work through home Contact customer api.whatsapp.com/send?phone=919706698213",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("Spam job offer must be ignored", result is ParseResult.Ignored)
    }

    @Test
    fun testInsuranceClaimSettlementNoticeIsIgnored() {
        val sms =
            RawSms(
                id = 48,
                sender = "MEDIASST",
                body =
                    "We have received confirmation on the settlement of your claim 145769075 for a policy issued by " +
                        "The New India Assurance Co. Ltd for INR 15545 vide reference number AXISCN1489391873 on " +
                        "05 Oct 2026 from your insurer. Current balance is INR 484455 for Policy Period 01 Jun 2026 " +
                        "to 31 May 2027. Click https://massist.in/MASIST/26o3v41 for details. Team Medi Assist",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("Insurance claim settlement notice must be ignored", result is ParseResult.Ignored)
    }

    @Test
    fun testRefundInitiatedNoticeIsIgnored() {
        val sms =
            RawSms(
                id = 49,
                sender = "URBANCO",
                body =
                    "Dear customer, your amount Rs2511 /- has been initiated for refund. " +
                        "It will be credited back to you within 7 working days. Thanks Team Urban Company",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("Refund initiated notification must be ignored", result is ParseResult.Ignored)
    }

    @Test
    fun testMerchantBillDownloadReceiptIsIgnored() {
        val sms =
            RawSms(
                id = 149,
                sender = "APLPHR",
                body =
                    "Dear KRISHNA SINGH ,Thank you for your purchase @ Apollo Pharmacy. " +
                        "Click here http://ms1.in/APLPHR/Vut/3RfdG to download your bill for Rs.500.00 APLPHR",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(
            "Merchant bill download link without bank indicators must be ignored",
            result is ParseResult.Ignored || result is ParseResult.NoMatch,
        )
        if (result is ParseResult.Ignored) {
            assertEquals(IgnoreReason.NOT_A_TRANSACTION, result.reason)
        }
    }

    @Test
    fun testHdfcUpiAccountDebitTemplate() {
        val sms =
            RawSms(
                id = 50,
                sender = "VM-HDFCBK",
                body =
                    "HDFC Bank:Rs. 53000.00 debited from a/c *1999 on 04/10/26 to a/c **0034 " +
                        "(UPI Ref No. 615324150359). Not you? Call on 18002586161 to report",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(5300000L, txn.amountMinor)
        assertEquals("1999", txn.accountLast4)
        assertEquals("HDFC", txn.bankName)
        assertEquals("a/c **0034", txn.merchantRaw?.trim())
        assertEquals("615324150359", txn.refNumber)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testIciciUpiDebitWithPayeeCredited() {
        val sms =
            RawSms(
                id = 51,
                sender = "VM-ICICIB",
                body = "ICICI Bank Acct XX321 debited for Rs 188.00 on 13-Aug-26; Zepto credited. " +
                    "UPI:312420876077. Call 18002662 for dispute. SMS BLOCK 321 to 9215676766.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(18800L, txn.amountMinor)
        assertEquals("321", txn.accountLast4)
        assertEquals("ICICI", txn.bankName)
        assertEquals("Zepto", txn.merchantRaw?.trim())
        assertEquals("312420876077", txn.refNumber)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testIdfcCardRefund() {
        val sms =
            RawSms(
                id = 52,
                sender = "VM-IDFCFB",
                body = "Transaction of INR 739.00 is refunded to IDFC FIRST Bank Credit Card XX5625 " +
                    "on 19-Oct-26 at 01:40 PM. Avl Limit: INR 3,36,043.64.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(73900L, txn.amountMinor)
        assertEquals("5625", txn.accountLast4)
        assertEquals("IDFC", txn.bankName)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals(Direction.CREDIT, txn.direction)
    }

    @Test
    fun testCardAndMerchantSplittingFallback() {
        val sms =
            RawSms(
                id = 53,
                sender = "VM-HDFCBK",
                body = "Rs 1,499.00 spent on HDFC Bank Card 8302 At ASSPL on 24-10-24. Avl limit Rs 45,000.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(149900L, txn.amountMinor)
        assertEquals("8302", txn.accountLast4)
        assertEquals("ASSPL", txn.merchantRaw)
        assertEquals("HDFC", txn.bankName)
        assertEquals(Channel.CARD, txn.channel)
    }

    @Test
    fun testMaskedPanCardExtractionFallback() {
        val sms =
            RawSms(
                id = 54,
                sender = "VM-ICICIB",
                body = "INR 2,499.00 spent on your Card 4620XXXXXXXX6567 at AMAZONIN on 20-Oct-26.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(249900L, txn.amountMinor)
        assertEquals("6567", txn.accountLast4)
        assertEquals("AMAZONIN", txn.merchantRaw)
        assertEquals(Channel.CARD, txn.channel)
    }

    @Test
    fun testDatePrefixMerchantStrippedFallback() {
        val sms =
            RawSms(
                id = 55,
                sender = "VM-ICICIB",
                body = "INR 1,299.00 spent on Card XX3006 on 04-Oct-26 at GOOGLE STORE.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(129900L, txn.amountMinor)
        assertEquals("3006", txn.accountLast4)
        assertEquals("GOOGLE STORE", txn.merchantRaw)
        assertEquals(Channel.CARD, txn.channel)
    }

    @Test
    fun testHdfcNeftCreditSalaryWithIfscAndPayee() {
        val sms =
            RawSms(
                id = 56,
                sender = "VM-HDFCBK",
                body = "Update! INR 1,69,221.00 deposited in HDFC Bank A/c XX1999 on 29-SEP-26 for " +
                    "NEFT Cr-BOFA0CN6215-QUALCOMM INDIA PVT LTD-KUMAR SINGH KRISHNA-BOFAH26272092252." +
                    "Avl bal INR 3,51,355.22. Cheque deposits in A/C are subject to clearing",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(16922100L, txn.amountMinor)
        assertEquals("1999", txn.accountLast4)
        assertEquals("HDFC", txn.bankName)
        assertEquals("QUALCOMM INDIA", txn.merchantRaw)
        assertEquals(Channel.NEFT, txn.channel)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("BOFAH26272092252", txn.refNumber)
        assertEquals(35135522L, txn.balanceAfterMinor)
    }

    @Test
    fun testSbiUpiDebitWithDebitedAtSbiAndDate() {
        val sms =
            RawSms(
                id = 57,
                sender = "VM-SBIUPI",
                body = "Your A/C ending with 1234 debited@SBI by 500.00 on 22Feb22 transfer to MR DIKSHIT SHARMA Ref 123456789012",
                receivedAt = 1645488000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(50000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("MR DIKSHIT SHARMA", txn.merchantRaw)
        assertEquals("123456789012", txn.refNumber)
        org.junit.Assert.assertNotEquals("debited@SBI", txn.upiId)
        assertEquals(Channel.UPI, txn.channel)
    }

    @Test
    fun testWellsFargoNeftCreditSalary() {
        val sms =
            RawSms(
                id = 58,
                sender = "VM-HDFCBK",
                body = "Update! INR 1,50,000.00 deposited in HDFC Bank A/c XX1999 on 31-Dec-25 for " +
                    "NEFT-HDFCH00825101407-WELLS. Avl bal INR 2,00,000.00.",
                receivedAt = 1767139200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(15000000L, txn.amountMinor)
        assertEquals("1999", txn.accountLast4)
        assertEquals("WELLS", txn.merchantRaw)
        assertEquals("HDFCH00825101407", txn.refNumber)
        assertEquals(Channel.NEFT, txn.channel)
        assertEquals(Direction.CREDIT, txn.direction)
    }

    @Test
    fun testAtmWithdrawalExtraction() {
        val sms =
            RawSms(
                id = 59,
                sender = "VM-SBINB",
                body = "INR 5,000.00 withdrawn from A/c XX1234 on 04-Jan-24 at SBI ATM S1BB000454545 from A/c. Avl bal INR 10,000.00.",
                receivedAt = 1704326400000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(500000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("SBI ATM", txn.merchantRaw)
        assertEquals(Channel.ATM, txn.channel)
    }

    @Test
    fun testHdfcAutoDebitInfoCode() {
        val sms =
            RawSms(
                id = 60,
                sender = "VM-HDFCBK",
                body = "INR 3,189.00 debited from HDFC Bank A/c XX1999 on 04-Oct-26 InfoATD*Auto Debi. Avl bal INR 50,000.00.",
                receivedAt = 1791072000000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(318900L, txn.amountMinor)
        assertEquals("1999", txn.accountLast4)
        assertEquals("Auto Debit", txn.merchantRaw)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun testDateAndTransferPrefixStrippedInFallback() {
        val sms =
            RawSms(
                id = 61,
                sender = "VM-HDFCBK",
                body = "INR 2,500.00 debited from A/c XX1234 on date 21Feb24 trf to COUNTRYDELIGHT Ref 9876543210.",
                receivedAt = 1708473600000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(250000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("COUNTRYDELIGHT", txn.merchantRaw)
        assertEquals("9876543210", txn.refNumber)
    }

    @Test
    fun testFallbackParser_ignoresPrecedingBalanceAndExtractsRealAmount() {
        val sms =
            RawSms(
                id = 62,
                sender = "VM-CENTBK",
                body = "Dear Customer, available balance in your account is Rs 18,500.00. Rs 250.00 debited for Chai Point.",
                receivedAt = 1708473600000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(25000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("Chai Point", txn.merchantRaw)
    }

    @Test
    fun testFallbackParser_billPaymentFromDebitCardIsDebitNotCredit() {
        val sms =
            RawSms(
                id = 63,
                sender = "VM-SBICRD",
                body = "Payment of Rs. 1,499.00 received from your SBI Debit Card XX1234 for Airtel Broadband.",
                receivedAt = 1708473600000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(149900L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("1234", txn.accountLast4)
    }

    @Test
    fun testBoiDebitTemplate() {
        val sms =
            RawSms(
                id = 64,
                sender = "VM-BOITXN",
                body = "Your A/C XXXXX1234 is debited for Rs 850.00 on 24-10-2026 by UPI/6291823/Zomato. Avl Bal Rs 14,250.00",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(85000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("Zomato", txn.merchantRaw)
        assertEquals(Direction.DEBIT, txn.direction)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testCbiDebitTemplate() {
        val sms =
            RawSms(
                id = 65,
                sender = "VM-CBISMS",
                body = "A/c *1234 debited by Rs.1,200.00 on 24/10/2026 through UPI:629182 to swiggy@okhdfcbank. Bal: Rs.5,400.00",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(120000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("swiggy@okhdfcbank", txn.merchantRaw)
        assertEquals(Direction.DEBIT, txn.direction)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testIndianBankDebitTemplate() {
        val sms =
            RawSms(
                id = 66,
                sender = "VM-INDBNK",
                body = "Dear Customer, A/c no. ending 1234 is debited by Rs. 450.00 on 24-10-2026 to Zomato. Ref: 429182. Bal: Rs 8,100",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(45000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("Zomato", txn.merchantRaw)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testAubankDebitTemplate() {
        val sms =
            RawSms(
                id = 67,
                sender = "VM-AUBANK",
                body = "Rs. 1,499.00 spent on AU Bank Debit Card ending 1234 at RELIANCE RETAIL on 24-Oct-26. Avail Bal: Rs 42,100",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(149900L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("RELIANCE RETAIL", txn.merchantRaw)
        assertEquals(Channel.CARD, txn.channel)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testCredPaymentTemplate() {
        val sms =
            RawSms(
                id = 68,
                sender = "VM-CREDCL",
                body = "Payment of Rs 14,500.00 to HDFC Credit Card ending 1234 successful via CRED on 24-10-2026. UTR: 4291823",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(1450000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals(Direction.DEBIT, txn.direction)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testPluxeeSpendTemplate() {
        val sms =
            RawSms(
                id = 69,
                sender = "VM-PLUXEE",
                body = "Spent Rs. 420.00 on Pluxee Card 1234 at SWEET DREAMS on 24-Oct-26. Remaining card balance: Rs 1,840.00",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(42000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("SWEET DREAMS", txn.merchantRaw)
        assertEquals(Channel.CARD, txn.channel)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testSbiAccountDebitTemplate() {
        val sms =
            RawSms(
                id = 70,
                sender = "VM-SBINB",
                body = "Dear SBI Customer, A/c X1234 debited by Rs 5,000.00 on 24Oct26 through ATM cash wdl. Bal: Rs 24,150.00",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(500000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testKotakDebitTemplate() {
        val sms =
            RawSms(
                id = 71,
                sender = "VM-KOTAKB",
                body = "Sent Rs. 3,500.00 from Kotak Bank A/c XX1234 to Reliance Digital on 24-Oct-2026. Net Banking Ref 123456. Bal: Rs 41,000",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(350000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("Reliance Digital", txn.merchantRaw)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testBobCardSpendTemplate() {
        val sms =
            RawSms(
                id = 72,
                sender = "VM-BOBTXN",
                body = "Rs. 950.00 spent on Bank of Baroda Debit Card XX1234 at DMART on 24-10-2026. Avl Bal: Rs 8,300",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(95000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("DMART", txn.merchantRaw)
        assertEquals(Channel.CARD, txn.channel)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testCanaraUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 73,
                sender = "VM-CANBNK",
                body = "Your A/c ending 1234 debited by INR 620.00 on 24-Oct-2026 via UPI to Zomato. UPI Ref 429182. Avail Bal: INR 19,400",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(62000L, txn.amountMinor)
        assertEquals("1234", txn.accountLast4)
        assertEquals("Zomato", txn.merchantRaw)
        assertNotNull(txn.templateId)
    }

    @Test
    fun testHdfcUpiDebitToAccountTransferMessage() {
        val sms =
            RawSms(
                id = 74,
                sender = "VM-HDFCBK",
                body = "HDFC Bank:Rs. 53000.00 debited from a/c *1999 on 04/10/26 to a/c **0034 (UPI Ref No. 615324150359). Not you? Call on 18002586161 to report",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(5300000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("1999", txn.accountLast4)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals("615324150359", txn.refNumber)
    }

    @Test
    fun testIdfcCreditCardPaymentMessage() {
        val sms =
            RawSms(
                id = 75,
                sender = "AX-IDFCFB-S",
                body = "Thank you for payment of INR 10,641.62 towards your FIRST Wealth Credit Card XX5625 on 05 Jun 2026. IDFC FIRST Bank",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(1064162L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("5625", txn.accountLast4)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals("IDFC", txn.bankName)
    }

    @Test
    fun testIciciCreditCardAutoDebitMessage() {
        val sms =
            RawSms(
                id = 76,
                sender = "AD-ICICIT-S",
                body = "Dear Customer, thank you for your payment of INR 11,311.00 towards ICICI Bank Credit Card Account XX1268 through Auto Debit from Account XX2321 on 04-Oct-26",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("Credit card bill payment confirmation should be skipped", result is ParseResult.NoMatch)
    }

    @Test
    fun testHdfcCardmemberPaymentReceivedMessage() {
        val sms =
            RawSms(
                id = 77,
                sender = "VD-HDFCBK-S",
                body = "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 595.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 8302 ON 2-6-2025.YOUR AVAILABLE LIMIT IS RS. 154621.36",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(59500L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("8302", txn.accountLast4)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals("HDFC", txn.bankName)
        assertEquals(15462136L, txn.balanceAfterMinor)
    }

    @Test
    fun testIciciAccountDebitCreditCardBillMessage() {
        val sms =
            RawSms(
                id = 78,
                sender = "VM-ICICIB",
                body = "ICICI Bank Acct XX321 debited for Rs 96808.63 on 05-Aug-26; Credit Card Bil credited. UPI:621717806116. Call 18002662 for dispute. SMS BLOCK 321 to 9215676766.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(9680863L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("321", txn.accountLast4)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals("621717806116", txn.refNumber)
        assertEquals("ICICI", txn.bankName)
    }

    @Test
    fun testIdfcEStatementDueMessageMustNotBeTracked() {
        val sms =
            RawSms(
                id = 201,
                sender = "AD-IDFCFB-S",
                body = """The eStatement for your FIRST Wealth Credit Card XX5625 is here https://idfcfr.in/IDFCFB/pFjq11
To access eStatement use your date of birth in DDMM format
Payment Due by 04-11-2026
Min Due: INR 707.00
Total Due: INR 35,330.41
Click to pay: https://idfcfr.in/IDFCFB/fztZ8n
IDFC FIRST Bank""",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("eStatement / Due reminder must NOT be tracked as a transaction", result !is ParseResult.Success)
    }

    @Test
    fun testIciciKrishnaKumarSMessageMustBeTracked() {
        val sms =
            RawSms(
                id = 202,
                sender = "AD-ICICIT-S",
                body = "ICICI Bank Acct XX321 debited for Rs 80000.00 on 22-Oct-26; KRISHNA KUMAR S credited. UPI:218683710640. Call 18002662 for dispute. SMS BLOCK 321 to 9215676766.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("Legitimate ICICI debit transaction must be tracked", result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(8000000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("321", txn.accountLast4)
        assertEquals("KRISHNA KUMAR S", txn.merchantRaw)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals("ICICI", txn.bankName)
    }

    @Test
    fun testIdfcCreditCardRefundNumericDateMessage() {
        val sms =
            RawSms(
                id = 79,
                sender = "AX-IDFCFB-S",
                body = "Transaction of INR 1599.00 is refunded to IDFC FIRST Bank Credit Card XX5625 on 28-08-2026. Please refer to your email for details.",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(159900L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("5625", txn.accountLast4)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals("IDFC", txn.bankName)
    }

    @Test
    fun testIciciCreditCardRefundMessageWithMerchant() {
        val sms =
            RawSms(
                id = 80,
                sender = "AD-ICICIT-S",
                body = "Zepto Marketplace Priv refund of Rs 189.00 credited to ICICI Bank Credit Card XX3006 on 22-MAY-26. Revised total due Rs 14,568.00, minimum due Rs 493.00",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(18900L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("3006", txn.accountLast4)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals("ICICI", txn.bankName)
        assertEquals("Zepto Marketplace Priv", txn.merchantRaw)
    }

    @Test
    fun testHdfcUpiCreditAlertWithVpaMessage() {
        val sms =
            RawSms(
                id = 81,
                sender = "VM-HDFCBK-S",
                body = "Credit Alert! Rs.2511.00 credited to HDFC Bank A/c XX1999 on 21-10-26 from VPA *pany@ptybl (UPI 185726917017)",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(251100L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("1999", txn.accountLast4)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals("HDFC", txn.bankName)
        assertEquals("*pany", txn.merchantRaw)
        assertEquals("185726917017", txn.refNumber)
    }

    @Test
    fun testHdfcSentRsUpiDebitTemplate() {
        val sms =
            RawSms(
                id = 82,
                sender = "VM-HDFCBK-S",
                body = "Sent Rs.80000.00 From HDFC Bank A/C *1999 To MUTUAL FUNDS NCL on 24-10-24. UPI Ref 123456789012",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(8000000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("1999", txn.accountLast4)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals("HDFC", txn.bankName)
        assertEquals("MUTUAL FUNDS NCL", txn.merchantRaw)
        assertEquals("123456789012", txn.refNumber)
    }

    @Test
    fun testIciciAccountCreditTemplate() {
        val sms =
            RawSms(
                id = 84,
                sender = "VM-ICICIB",
                body = "ICICI Bank Account XX321 is credited with Rs 50,000.00 on 22-MAY-26 by transfer from Employer. Avl Bal Rs 1,50,000.00",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(5000000L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("321", txn.accountLast4)
        assertEquals("ICICI", txn.bankName)
    }

    @Test
    fun testIciciCardSpendOnTemplate() {
        val sms =
            RawSms(
                id = 85,
                sender = "BG-ICICIT-S",
                body = "Rs 12,999.00 spent on ICICI Bank Card XX3006 on 04-Oct-26 at GOOGLE STORE. Avl Limit: Rs 80,000.00",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue(result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(1299900L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("3006", txn.accountLast4)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals("ICICI", txn.bankName)
        assertEquals("GOOGLE STORE", txn.merchantRaw)
        org.junit.Assert.assertNull(txn.refNumber)
    }

    @Test
    fun testIciciCardPaymentAckTemplate() {
        val sms =
            RawSms(
                id = 86,
                sender = "AD-ICICIT-S",
                body = "Dear Customer, thank you for payment of INR 11,311.00 towards ICICI Bank Credit Card Account XX1268 on 04-Oct-26",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("Credit card bill payment confirmation should be skipped", result is ParseResult.NoMatch)
    }

    @Test
    fun testIciciCardSpendWithEmiOffer() {
        val sms =
            RawSms(
                id = 87,
                sender = "AD-ICICIT-S",
                body = "Rs 12,999.00 spent on ICICI Bank Card XX3006 on 04-Oct-26 at GOOGLE STORE. Avl Lmt: Rs 6,96,812.00. To dispute, call 18002662/SMS BLOCK 3006 to 9215676766. To convert this txn to EMI give a missed call on 9924667667. Know more about EMI conversion at https://icici.co/ICICIT/cc4asx",
                receivedAt = 1792897200000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("Expected Success but got $result", result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(1299900L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("3006", txn.accountLast4)
        assertEquals(Channel.CARD, txn.channel)
        assertEquals("ICICI", txn.bankName)
        assertEquals("GOOGLE STORE", txn.merchantRaw)
        org.junit.Assert.assertNull(txn.refNumber)
    }

    @Test
    fun testIciciBankDebit80000() {
        val sms =
            RawSms(
                id = 88,
                sender = "AD-ICICIT-S",
                body = "ICICI Bank Acct XX321 debited for Rs 80000.00 on 22-Oct-26; KRISHNA KUMAR S credited. UPI:218683710640. Call 18002662 for dispute. SMS BLOCK 321 to 9215676766.",
                receivedAt = 1792657655000L,
                subId = null,
            )
        val result = parser.parse(sms)
        assertTrue("Expected Success but got $result", result is ParseResult.Success)
        val txn = (result as ParseResult.Success).txn
        assertEquals(8000000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("321", txn.accountLast4)
        assertEquals(Channel.UPI, txn.channel)
        assertEquals("ICICI", txn.bankName)
        assertEquals("KRISHNA KUMAR S", txn.merchantRaw)
        assertEquals("218683710640", txn.refNumber)
    }
}
