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
        assertEquals("icici_card_spend_v1", txn.templateId)
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
        assertEquals("icici_debit_v1", txn.templateId)
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
        assertEquals("icici_credit_v1", txn.templateId)
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
        assertEquals("hdfc_account_debit_v1", txn.templateId)
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
        assertEquals("hdfc_credit_v1", txn.templateId)
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
        assertEquals("axis_credit_v1", txn.templateId)
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
        assertEquals("kotak_credit_v1", txn.templateId)
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
        assertEquals("pnb_upi_debit_v1", txn.templateId)
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
        assertEquals("union_bank_debit_v1", txn.templateId)
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
        assertEquals("jupiter_spend_v1", txn.templateId)
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
        assertEquals("fi_money_spend_v1", txn.templateId)
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
        assertEquals("paytm_upi_debit_v1", txn.templateId)
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
        assertEquals("airtel_bank_debit_v1", txn.templateId)
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
        assertEquals("scb_card_spend_v1", txn.templateId)
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
        assertEquals("amex_card_spend_v1", txn.templateId)
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
        assertEquals("idfc_card_spend_v1", txn.templateId)
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
        assertEquals("icici_card_spend_v2", txn.templateId)
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
}
