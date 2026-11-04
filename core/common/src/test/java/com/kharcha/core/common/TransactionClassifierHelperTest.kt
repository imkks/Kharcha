package com.kharcha.core.common

import com.kharcha.core.model.Channel
import com.kharcha.core.model.DEFAULT_CREDIT_CATEGORIES
import com.kharcha.core.model.DEFAULT_SPEND_CATEGORIES
import com.kharcha.core.model.Direction
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.sanitizeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionClassifierHelperTest {
    @Test
    fun classifiesTransfersCorrectly() {
        val body1 = "Rs 20,000 debited from A/c XX1234 towards Credit Card XX9876 payment"
        val type1 = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = body1,
        )
        assertEquals(TransactionType.Expense, type1)
        assertEquals("Bills & Utilities", TransactionClassifierHelper.guessCategory("Credit Card", false, isTransfer = true, body = body1))

        val body2 = "Rs 5,000 debited for self transfer to SBI"
        val type2 = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = body2,
        )
        assertEquals(TransactionType.Expense, type2)
        assertEquals("Transfer", TransactionClassifierHelper.guessCategory("SBI", false, isTransfer = true, body = body2))

        val autoDebitBody = "Rs 15,000 debited from A/c XX1234 towards Auto Debit for HDFC Credit Card"
        val autoDebitType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = autoDebitBody,
        )
        assertEquals(TransactionType.Expense, autoDebitType)
        assertEquals("Bills & Utilities", TransactionClassifierHelper.guessCategory("HDFC", false, body = autoDebitBody))

        val cardBillReceivedType = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = "Payment of Rs 15,000 received towards your HDFC Bank Credit Card ending 9876",
        )
        assertEquals(TransactionType.Income, cardBillReceivedType)

        val credType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 15,000 debited from A/c XX1234 to VPA cred@axis",
            merchant = "CRED",
        )
        assertEquals(TransactionType.Expense, credType)
    }

    @Test
    fun cardSpendRemainsExpense() {
        val cardSpend = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 1,299 spent on Credit Card XX9876 at AMAZON on 20-Oct",
            merchant = "AMAZON",
        )
        assertEquals(TransactionType.Expense, cardSpend)
    }

    @Test
    fun extractsTargetCardAndSourceAccount() {
        val sms = "Rs 20,000 debited from A/c XX1234 towards Credit Card XX9876 payment"
        val targetCard = TransactionClassifierHelper.extractTargetCardLast4(sms)
        val sourceAcct = TransactionClassifierHelper.extractSourceAccountLast4(sms)
        assertEquals("9876", targetCard)
        assertEquals("1234", sourceAcct)

        val maskedSms = "Rs 20,000 debited from a/c **2321 towards your Card 4620XXXXXXXX6567 payment"
        assertEquals("6567", TransactionClassifierHelper.extractTargetCardLast4(maskedSms))
        assertEquals("2321", TransactionClassifierHelper.extractSourceAccountLast4(maskedSms))

        val autopaySms = "CC 000434155XXXXXX8302 Autopay executed for Rs 5,420 from Acct XXX098"
        assertEquals("8302", TransactionClassifierHelper.extractTargetCardLast4(autopaySms))
        assertEquals("098", TransactionClassifierHelper.extractSourceAccountLast4(autopaySms))
    }

    @Test
    fun classifiesStandardDebitAndCredit() {
        val debitType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 500 debited from A/c XX1234 for Swiggy",
        )
        assertEquals(TransactionType.Expense, debitType)

        val creditType = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = "Rs 50,000 credited to A/c XX1234 salary",
        )
        assertEquals(TransactionType.Income, creditType)
    }

    @Test
    fun cleansMerchantNames() {
        val cleaned = TransactionClassifierHelper.cleanMerchantDisplay(
            raw = "swiggy@icici",
            bank = "ICICI",
            channel = "UPI",
        )
        assertEquals("Swiggy", cleaned)

        val cardNoiseCleaned = TransactionClassifierHelper.cleanMerchantDisplay(
            raw = "IDFC FIRST Bank Credit Card XX5625",
            bank = "IDFC",
            channel = "CARD",
        )
        assertEquals("IDFC CARD", cardNoiseCleaned)

        val datePrefixCleaned = TransactionClassifierHelper.cleanMerchantDisplay(
            raw = "04-Oct-26 at GOOGLE STORE",
            bank = "ICICI",
            channel = "CARD",
        )
        assertEquals("Google Store", datePrefixCleaned)
    }

    @Test
    fun classifiesRefundsCorrectly() {
        val urbanCompanySms =
            "Dear customer, your amount Rs2511 /- has been initiated for refund. " +
                "It will be credited back to you within 7 working days. Thanks Team Urban Company"
        val refundType1 = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = urbanCompanySms,
            merchant = "Urban Company",
        )
        assertEquals(TransactionType.Income, refundType1)
        assertEquals("Refund", TransactionClassifierHelper.guessCategory("Urban Company", isCredit = true, body = urbanCompanySms))

        val refundBody2 = "Refund of Rs. 450.00 from Swiggy has been credited to your account"
        val refundType2 = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = refundBody2,
            merchant = "Swiggy",
        )
        assertEquals(TransactionType.Income, refundType2)
        assertEquals("Refund", TransactionClassifierHelper.guessCategory("Swiggy", isCredit = true, body = refundBody2))

        val reversalBody = "Transaction reversal of Rs 500 credited to a/c **1234"
        val reversalType = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = reversalBody,
        )
        assertEquals(TransactionType.Income, reversalType)
        assertEquals("Refund", TransactionClassifierHelper.guessCategory("Unknown", isCredit = true, body = reversalBody))

        assertEquals(false, TransactionClassifierHelper.isRefundNarrative(null))
        assertEquals(false, TransactionClassifierHelper.isTransferNarrative(null))
        assertEquals(false, TransactionClassifierHelper.isCreditCardBillPayment(null))
        assertEquals(false, TransactionClassifierHelper.isAutoDebit(null))
        assertEquals(null, TransactionClassifierHelper.extractTargetCardLast4(null))
        assertEquals(null, TransactionClassifierHelper.extractSourceAccountLast4(null))
    }

    @Test
    fun classifiesHighValueTransfersAndInvestments() {
        val upiTransferBody = "HDFC Bank:Rs. 53000.00 debited from a/c *1999 on 04/10/26 to a/c **0034"
        val upiTransferType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = upiTransferBody,
            merchant = "04 10 26 To",
            amountMinor = 53_000_00L,
            channel = Channel.UPI,
        )
        assertEquals(TransactionType.Expense, upiTransferType)
        assertEquals("Transfer", TransactionClassifierHelper.guessCategory("04 10 26 To", isCredit = false, isTransfer = true, body = upiTransferBody))

        val mfBody = "Sent Rs.80000.00 From HDFC Bank A/C *1999 To MUTUAL FUNDS NCL"
        val mfType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = mfBody,
            merchant = "Mutual Funds Ncl",
            amountMinor = 80_000_00L,
            channel = Channel.UPI,
        )
        assertEquals(TransactionType.Expense, mfType)
        assertEquals("Transfer", TransactionClassifierHelper.guessCategory("Mutual Funds Ncl", isCredit = false, isTransfer = true, body = mfBody))

        val googleStoreSpend = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 12,999.00 spent on ICICI Bank Card XX3006 on 04-Oct-26 at GOOGLE STORE.",
            merchant = "Google Store",
            amountMinor = 12_999_00L,
            channel = Channel.CARD,
        )
        assertEquals(TransactionType.Expense, googleStoreSpend)

        val largeUpiDebit = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "ICICI Bank Acct XX321 debited for Rs 80000.00 on 22-Oct-26; KRISHNA KUMAR S credited. UPI:218683710640. Call 18002662 for dispute. SMS BLOCK 321 to 9215676766.",
            merchant = "Krishna Kumar S",
            amountMinor = 80_000_00L,
            channel = Channel.UPI,
        )
        assertEquals(TransactionType.Expense, largeUpiDebit)
    }

    @Test
    fun categorizesInwardAndOutwardTransactionsProperly() {
        // Inward: Refund
        val refundCategory = TransactionClassifierHelper.guessCategory(
            merchant = "Idfc First Bank Credit",
            isCredit = true,
            isRefund = true,
            body = "Transaction of INR 739.00 is refunded to IDFC FIRST Bank Credit Card XX5625",
        )
        assertEquals("Refund", refundCategory)

        // Inward: P2P UPI Credit
        val p2pCategory = TransactionClassifierHelper.guessCategory(
            merchant = "Skdheraj 1",
            isCredit = true,
            body = "Credit Alert! Rs.21167.33 credited to HDFC Bank A/c XX1999 on 02-10-26 from VPA skdheraj-1@okicici",
        )
        assertEquals("Transfer", p2pCategory)

        // Inward: Real Salary
        val salaryCategory = TransactionClassifierHelper.guessCategory(
            merchant = "Clearing",
            isCredit = true,
            body = "Update! INR 1,69,221.00 deposited in HDFC Bank A/c XX1999 on 29-SEP-26 for NEFT Cr-Salary",
        )
        assertEquals("Salary", salaryCategory)

        val wellsSalary = TransactionClassifierHelper.guessCategory(
            merchant = "WELLS FARGO",
            isCredit = true,
            body = "INR 1,50,000.00 credited to A/c XX1999 on 31-Dec-25 by NEFT from WELLS FARGO",
        )
        assertEquals("Salary", wellsSalary)

        val qualcommSalary = TransactionClassifierHelper.guessCategory(
            merchant = "QUALCOMM INDIA PVT LTD",
            isCredit = true,
            body = "INR 1,69,221.00 deposited in HDFC Bank A/c XX1999 on 29-SEP-26",
        )
        assertEquals("Salary", qualcommSalary)

        // Inward: Broker Payout / Investment Return (MUST NOT be "Investment" debit category)
        val zerodhaCategory = TransactionClassifierHelper.guessCategory(
            merchant = "Clearing",
            isCredit = true,
            body = "Update! INR 16,891.59 deposited in HDFC Bank A/c XX1999 on 05-SEP-26 " +
                "for NEFT Cr-ZERODHA BROKING LTD",
        )
        assertEquals("Investment Return", zerodhaCategory)

        // Outward: Spends mapped to proper categories
        assertEquals("Groceries", TransactionClassifierHelper.guessCategory("Vijetha Super Markets", isCredit = false))
        assertEquals("Food & Dining", TransactionClassifierHelper.guessCategory("HungerBox", isCredit = false))
        assertEquals("Food & Dining", TransactionClassifierHelper.guessCategory("EatClub", isCredit = false))
        assertEquals("Shopping", TransactionClassifierHelper.guessCategory("Decathlon", isCredit = false))
        assertEquals("Fuel", TransactionClassifierHelper.guessCategory("Pump 25 Ts Police", isCredit = false))
        assertEquals("Travel", TransactionClassifierHelper.guessCategory("Telangana State Road Tran", isCredit = false))
        assertEquals("Bills & Utilities", TransactionClassifierHelper.guessCategory("Auto Debit", isCredit = false))
        assertEquals("Bills & Utilities", TransactionClassifierHelper.guessCategory("Bill Payment", isCredit = false))
        assertEquals("Bills & Utilities", TransactionClassifierHelper.guessCategory("Bharti Airtel", isCredit = false))
        assertEquals("Bills & Utilities", TransactionClassifierHelper.guessCategory("Airtel Recharge", isCredit = false))
        assertEquals("Bills & Utilities", TransactionClassifierHelper.guessCategory("Jio Prepaid", isCredit = false))
        assertEquals("Other", TransactionClassifierHelper.guessCategory("SBI ATM", isCredit = false))
        assertEquals("Other", TransactionClassifierHelper.guessCategory("Cash Withdrawal", isCredit = false))

        // PennyWise merchant categorization integration checks
        assertEquals("Food & Dining", TransactionClassifierHelper.guessCategory("Haldiram Sweets", isCredit = false))
        assertEquals("Food & Dining", TransactionClassifierHelper.guessCategory("Subway Restaurant", isCredit = false))
        assertEquals("Health", TransactionClassifierHelper.guessCategory("Apollo Pharmacy", isCredit = false))
        assertEquals("Entertainment", TransactionClassifierHelper.guessCategory("BookMyShow Media", isCredit = false))
        assertEquals("Investments", TransactionClassifierHelper.guessCategory("Groww Investments", isCredit = false))

        // Credit Card Bill Payment inward message: category Credit & type Income
        val ccBillPaymentBody = "Thank you for payment of INR 10,641.62 towards your FIRST Wealth Credit Card XX5625 on 05 Jun 2026. IDFC FIRST Bank"
        val ccBillCat = TransactionClassifierHelper.guessCategory(
            merchant = "IDFC FIRST Bank",
            isCredit = true,
            body = ccBillPaymentBody,
        )
        assertEquals("Credit", ccBillCat)
        val ccBillType = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = ccBillPaymentBody,
            merchant = "IDFC FIRST Bank",
        )
        assertEquals(TransactionType.Income, ccBillType)

        // Zepto Refund message: category Refund & type Income
        val zeptoBody = "Zepto Marketplace Priv refund of Rs 189.00 credited to ICICI Bank Credit Card XX3006 on 22-MAY-26."
        val zeptoCat = TransactionClassifierHelper.guessCategory(
            merchant = "Zepto Marketplace Priv",
            isCredit = true,
            body = zeptoBody,
        )
        assertEquals("Refund", zeptoCat)
        val zeptoType = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = zeptoBody,
            merchant = "Zepto Marketplace Priv",
        )
        assertEquals(TransactionType.Income, zeptoType)
        val zeptoCleanedName = TransactionClassifierHelper.cleanMerchantDisplay(
            raw = "Zepto Marketplace Priv",
            bank = "ICICI",
            channel = "CARD",
        )
        assertEquals("Zepto Marketplace", zeptoCleanedName)

        // HDFC VPA credit alert: category Transfer & type Income
        val hdfcAlertBody = "Credit Alert! Rs.2511.00 credited to HDFC Bank A/c XX1999 on 21-10-26 from VPA *pany@ptybl (UPI 185726917017)"
        val alertCat = TransactionClassifierHelper.guessCategory(
            merchant = "pany@ptybl",
            isCredit = true,
            body = hdfcAlertBody,
        )
        assertEquals("Transfer", alertCat)
        val alertType = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = hdfcAlertBody,
            merchant = "pany@ptybl",
        )
        assertEquals(TransactionType.Income, alertType)
    }

    @Test
    fun strictlyAdheresToPrescribedCategorySets() {
        // Credit transactions MUST only produce categories in DEFAULT_CREDIT_CATEGORIES
        val creditTestCases = listOf(
            Triple("Zerodha", "credited settlement payout Rs 5000", "Investment Return"),
            Triple("Groww", "credited dividend of Rs 250", "Investment Return"),
            Triple("Employer", "salary credited for month", "Salary"),
            Triple("Friend", "UPI transfer credited Rs 1000", "Transfer"),
            Triple("Amazon", "refund credited Rs 500", "Refund"),
            Triple("Google Pay", "cashback credited Rs 25", "Cashback"),
            Triple("Unknown Source", "UPI credit alert Rs 500", "Transfer"),
            Triple("Something Weird", "inward misc payment", "Other"),
            Triple("Totally Random Merchant", "received money in account", "Unknown"),
        )

        for ((merchant, body, expected) in creditTestCases) {
            val cat = TransactionClassifierHelper.guessCategory(merchant = merchant, isCredit = true, body = body)
            assertEquals("Credit txn for $merchant should match $expected", expected, cat)
            assertTrue("Credit category '$cat' must belong to DEFAULT_CREDIT_CATEGORIES", DEFAULT_CREDIT_CATEGORIES.contains(cat))
        }

        // Debit transactions MUST only produce categories in DEFAULT_SPEND_CATEGORIES
        val debitTestCases = listOf(
            Pair("Swiggy", "Food & Dining"),
            Pair("Zepto", "Groceries"),
            Pair("Amazon", "Shopping"),
            Pair("Uber", "Transport"),
            Pair("Emirates", "Travel"),
            Pair("HPCL Petrol Pump", "Fuel"),
            Pair("Airtel Recharge", "Bills & Utilities"),
            Pair("BookMyShow", "Entertainment"),
            Pair("Apollo Pharmacy", "Health"),
            Pair("Zerodha", "Investments"),
            Pair("Bajaj Finance", "EMI"),
            Pair("Charity Foundation", "Donation"),
            Pair("SBI ATM", "Other"),
            Pair("Misc Payments", "Other"),
            Pair("Completely Random Merchant 12345", "Unknown"),
        )

        for ((merchant, expected) in debitTestCases) {
            val cat = TransactionClassifierHelper.guessCategory(merchant = merchant, isCredit = false)
            assertEquals("Debit txn for $merchant should match $expected", expected, cat)
            assertTrue("Debit category '$cat' must belong to DEFAULT_SPEND_CATEGORIES", DEFAULT_SPEND_CATEGORIES.contains(cat))
        }
    }

    @Test
    fun sanitizeCategoryEnforcesValidCategoriesStrictly() {
        // Credit mapping tests
        assertEquals("Investment Return", sanitizeCategory("Investment", isCredit = true))
        assertEquals("Investment Return", sanitizeCategory("Investments", isCredit = true))
        assertEquals("Investment Return", sanitizeCategory("dividend", isCredit = true))
        assertEquals("Transfer", sanitizeCategory("A/C Transfer", isCredit = true))
        assertEquals("Transfer", sanitizeCategory("ac transfer", isCredit = true))
        assertEquals("Cashback", sanitizeCategory("Cash back", isCredit = true))
        assertEquals("Rewards", sanitizeCategory("reward", isCredit = true))
        assertEquals("Refund", sanitizeCategory("refunded", isCredit = true))
        assertEquals("Other", sanitizeCategory("Other", isCredit = true))
        assertEquals("Unknown", sanitizeCategory("Unknown", isCredit = true))
        assertEquals("Unknown", sanitizeCategory("NonExistentCategory", isCredit = true))

        // Debit mapping tests
        assertEquals("Bills & Utilities", sanitizeCategory("Bills", isCredit = false))
        assertEquals("Bills & Utilities", sanitizeCategory("Bill Payment", isCredit = false))
        assertEquals("Bills & Utilities", sanitizeCategory("Utilities", isCredit = false))
        assertEquals("Food & Dining", sanitizeCategory("Food", isCredit = false))
        assertEquals("Groceries", sanitizeCategory("Grocery", isCredit = false))
        assertEquals("Transport", sanitizeCategory("Transportation", isCredit = false))
        assertEquals("Investments", sanitizeCategory("Investment", isCredit = false))
        assertEquals("Unknown", sanitizeCategory("Unknown Category", isCredit = false))
        assertEquals("Unknown", sanitizeCategory("Unknown", isCredit = false))
        assertEquals("Other", sanitizeCategory("Others", isCredit = false))
        assertEquals("Other", sanitizeCategory("Other", isCredit = false))
        assertEquals("Unknown", sanitizeCategory("NonExistentCategory", isCredit = false))
    }
}
