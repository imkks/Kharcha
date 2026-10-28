package com.pennywiseai.parser.core.test

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.AxisBankParser
import com.pennywiseai.parser.core.bank.BankOfBarodaParser
import com.pennywiseai.parser.core.bank.BankOfIndiaParser
import com.pennywiseai.parser.core.bank.CentralBankOfIndiaParser
import com.pennywiseai.parser.core.bank.FederalBankParser
import com.pennywiseai.parser.core.bank.HDFCBankParser
import com.pennywiseai.parser.core.bank.ICICIBankParser
import com.pennywiseai.parser.core.bank.IDFCFirstBankParser
import com.pennywiseai.parser.core.bank.IndusIndBankParser
import com.pennywiseai.parser.core.bank.KotakBankParser
import com.pennywiseai.parser.core.bank.PNBBankParser
import com.pennywiseai.parser.core.bank.SBIBankParser
import com.pennywiseai.parser.core.bank.YesBankParser
import org.junit.Test
import java.math.BigDecimal

class PennyWiseBankParsersTest {

    @Test
    fun testHdfcBankParser() {
        val parser = HDFCBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "Bill Alert Notification - Should Not Parse",
                message = "New Bill Alert: Your XUBA00000TST1A Bill 1234567890 of Rs.1500.00 is due on 15-Jan-2026. Ignore if paid",
                sender = "CP-HDFCBK-S",
                shouldParse = false,
            ),
            ParserTestCase(
                name = "UPI Debit Transaction",
                message = "Rs.500.00 debited from A/c XX1234 on 20-Oct-25 to merchant@upi (UPI Ref No 123456789012)",
                sender = "CP-HDFCBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "1234",
                    reference = "123456789012",
                ),
            ),
            ParserTestCase(
                name = "Credit Card Refund Initiated - Income",
                message = "Refund initiated: Amt: Rs.34274.66 on HDFC Bank Credit Card 1111. To receive your Refund,please update your Bank details: TnC.",
                sender = "VM-HDFCBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("34274.66"),
                    type = TransactionType.INCOME,
                    accountLast4 = "1111",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testIciciBankParser() {
        val parser = ICICIBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "ICICI Debit Transaction",
                message = "Your A/C ending 4567 is debited for INR 4,500.00 on 24-Oct-24 towards BillDesk. Avl Bal: INR 35,400.00",
                sender = "VM-ICICIB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4500.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "4567",
                ),
            ),
            ParserTestCase(
                name = "ICICI Card Spend",
                message = "INR 2,450.00 spent on ICICI Bank Credit Card ending 8899 on 24-Oct-24 at AMAZON INDIA. Available Limit: INR 85,000.00",
                sender = "VM-ICICIB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2450.00"),
                    type = TransactionType.CREDIT,
                    accountLast4 = "8899",
                ),
            ),
            ParserTestCase(
                name = "ICICI Credit Card Bill Payment Confirmation - Should Not Parse",
                message = "Dear Customer, thank you for your payment of INR 11,311.00 towards ICICI Bank Credit Card Account XX1268 through Auto Debit from Account XX2321 on 04-Oct-26",
                sender = "AD-ICICIT-S",
                shouldParse = false,
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testSbiBankParser() {
        val parser = SBIBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "SBI UPI Debit",
                message = "Your A/C ending with 1234 debited by 1500.00 on 24Oct24 transfer to SWIGGY Ref 429812345678",
                sender = "VM-SBIUPI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1500.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "1234",
                    reference = "429812345678",
                ),
            ),
            ParserTestCase(
                name = "SBI NEFT Credit",
                message = "INR 85,000.00 credited to A/c XX1234 on 01-10-26 by NEFT from ACME TECH PVT LTD SAL",
                sender = "VM-SBIUPI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("85000.00"),
                    type = TransactionType.INCOME,
                    accountLast4 = "1234",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testAxisBankParser() {
        val parser = AxisBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "Axis Card Spend",
                message = "Spent INR 1,599.00 on your Axis Bank Credit Card ending 1234 at FLIPKART on 24-Oct-2024. Avl Lmt: INR 75,000.00",
                sender = "VM-AXISBK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1599.00"),
                    type = TransactionType.CREDIT,
                    accountLast4 = "1234",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testKotakBankParser() {
        val parser = KotakBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "Kotak UPI Debit",
                message = "Sent Rs. 1500.00 from Kotak Bank A/C ending 1234 to swiggy@icici on 24-10-2024. UPI Ref 429812345678. Bal: Rs. 25000.50",
                sender = "VM-KOTAKB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1500.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "1234",
                    reference = "429812345678",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testIdfcBankParser() {
        val parser = IDFCFirstBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "IDFC UPI Debit",
                message = "Rs. 850.00 debited from IDFC FIRST Bank A/C ending 4321 on 24-Oct-24 to blinkit@icici. UPI Ref: 429812345678. Available balance: Rs. 45000.00",
                sender = "JD-IDFCFB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("850.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "4321",
                    reference = "429812345678",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testBankOfBarodaParser() {
        val parser = BankOfBarodaParser()
        val cases = listOf(
            ParserTestCase(
                name = "BOB UPI Debit",
                message = "Rs. 750.00 debited from Bank of Baroda A/c ending 5678 on 24-10-2024 via UPI to zomato@hdfcbank. Ref: 429812345678. Bal: Rs. 18,500.00",
                sender = "VM-BOBTXN",
                expected = ExpectedTransaction(
                    amount = BigDecimal("750.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "5678",
                    reference = "429812345678",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testIndusIndBankParser() {
        val parser = IndusIndBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "IndusInd Debit",
                message = "Your IndusInd Bank A/c ending 9876 has been debited by INR 3,450.00 on 24-10-2024 towards Flipkart. Avl Bal: INR 32,000.00.",
                sender = "VM-INDUSB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("3450.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "9876",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testFederalBankParser() {
        val parser = FederalBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "Federal Bank UPI Debit",
                message = "Rs. 650.00 debited from Federal Bank A/C ending 6789 on 24-10-2024 16:45 via UPI to starbucks@icici. Ref: 429812345678. Bal: Rs. 14,000.00",
                sender = "AD-FEDBNK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("650.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "6789",
                    reference = "429812345678",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testPnbBankParser() {
        val parser = PNBBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "PNB Debit",
                message = "Your A/c ending 7890 debited for INR 1,200.00 on 24-10-2024 towards Amazon. Avail Bal: INR 22,000.00",
                sender = "VM-PNBSMS",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1200.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "7890",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testYesBankParser() {
        val parser = YesBankParser()
        val cases = listOf(
            ParserTestCase(
                name = "Yes Bank Debit",
                message = "Your YES Bank A/c ending 1122 debited for INR 999.00 on 24-10-2024 towards Netflix. Bal: INR 19,000.00",
                sender = "VM-YESBNK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("999.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "1122",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }

    @Test
    fun testCentralBankOfIndiaParser() {
        val parser = CentralBankOfIndiaParser()
        val cases = listOf(
            ParserTestCase(
                name = "Central Bank Debit",
                message = "A/c *1234 debited by Rs.1,200.00 on 24/10/2026 through UPI:629182 to swiggy@okhdfcbank. Bal: Rs.5,400.00",
                sender = "VM-CBISMS",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1200.00"),
                    type = TransactionType.EXPENSE,
                    accountLast4 = "1234",
                ),
            ),
        )
        ParserTestHelper.assertCases(parser, cases)
    }
}
