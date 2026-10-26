package com.kharcha.core.common

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionClassifierHelperTest {
    @Test
    fun classifiesTransfersCorrectly() {
        val type1 = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 20,000 debited from A/c XX1234 towards Credit Card XX9876 payment",
        )
        assertEquals(TransactionType.Transfer, type1)

        val type2 = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 5,000 debited for self transfer to SBI",
        )
        assertEquals(TransactionType.Transfer, type2)

        val autoDebitType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 15,000 debited from A/c XX1234 towards Auto Debit for HDFC Credit Card",
        )
        assertEquals(TransactionType.Transfer, autoDebitType)

        val cardBillReceivedType = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = "Payment of Rs 15,000 received towards your HDFC Bank Credit Card ending 9876",
        )
        assertEquals(TransactionType.Transfer, cardBillReceivedType)

        val credType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 15,000 debited from A/c XX1234 to VPA cred@axis",
            merchant = "CRED",
        )
        assertEquals(TransactionType.Transfer, credType)
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
        assertEquals(TransactionType.Refund, refundType1)

        val refundType2 = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = "Refund of Rs. 450.00 from Swiggy has been credited to your account",
            merchant = "Swiggy",
        )
        assertEquals(TransactionType.Refund, refundType2)

        val reversalType = TransactionClassifierHelper.classifyType(
            direction = Direction.CREDIT,
            body = "Transaction reversal of Rs 500 credited to a/c **1234",
        )
        assertEquals(TransactionType.Refund, reversalType)

        assertEquals(false, TransactionClassifierHelper.isRefundNarrative(null))
        assertEquals(false, TransactionClassifierHelper.isTransferNarrative(null))
        assertEquals(false, TransactionClassifierHelper.isCreditCardBillPayment(null))
        assertEquals(false, TransactionClassifierHelper.isAutoDebit(null))
        assertEquals(null, TransactionClassifierHelper.extractTargetCardLast4(null))
        assertEquals(null, TransactionClassifierHelper.extractSourceAccountLast4(null))
    }

    @Test
    fun classifiesHighValueTransfersAndInvestments() {
        val upiTransferType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "HDFC Bank:Rs. 53000.00 debited from a/c *1999 on 04/10/26 to a/c **0034",
            merchant = "04 10 26 To",
            amountMinor = 53_000_00L,
            channel = Channel.UPI,
        )
        assertEquals(TransactionType.Transfer, upiTransferType)

        val mfType = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Sent Rs.80000.00 From HDFC Bank A/C *1999 To MUTUAL FUNDS NCL",
            merchant = "Mutual Funds Ncl",
            amountMinor = 80_000_00L,
            channel = Channel.UPI,
        )
        assertEquals(TransactionType.Transfer, mfType)

        val googleStoreSpend = TransactionClassifierHelper.classifyType(
            direction = Direction.DEBIT,
            body = "Rs 12,999.00 spent on ICICI Bank Card XX3006 on 04-Oct-26 at GOOGLE STORE.",
            merchant = "Google Store",
            amountMinor = 12_999_00L,
            channel = Channel.CARD,
        )
        assertEquals(TransactionType.Expense, googleStoreSpend)
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
        assertEquals("A/C Transfer", p2pCategory)

        // Inward: Real Salary
        val salaryCategory = TransactionClassifierHelper.guessCategory(
            merchant = "Clearing",
            isCredit = true,
            body = "Update! INR 1,69,221.00 deposited in HDFC Bank A/c XX1999 on 29-SEP-26 for NEFT Cr-Salary",
        )
        assertEquals("Salary", salaryCategory)

        // Inward: Broker Payout / Investment Return
        val zerodhaCategory = TransactionClassifierHelper.guessCategory(
            merchant = "Clearing",
            isCredit = true,
            body = "Update! INR 16,891.59 deposited in HDFC Bank A/c XX1999 on 05-SEP-26 " +
                "for NEFT Cr-ZERODHA BROKING LTD",
        )
        assertEquals("Investment", zerodhaCategory)

        // Outward: Spends mapped to proper categories
        assertEquals("Groceries", TransactionClassifierHelper.guessCategory("Vijetha Super Markets", isCredit = false))
        assertEquals("Food & Dining", TransactionClassifierHelper.guessCategory("HungerBox", isCredit = false))
        assertEquals("Shopping", TransactionClassifierHelper.guessCategory("Decathlon", isCredit = false))
        assertEquals("Fuel", TransactionClassifierHelper.guessCategory("Pump 25 Ts Police", isCredit = false))
        assertEquals("Travel", TransactionClassifierHelper.guessCategory("Telangana State Road Tran", isCredit = false))
    }
}
