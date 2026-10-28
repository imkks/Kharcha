package com.kharcha.engine.classifier

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.ParsedTxn
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifierCardRefundTest {
    private val classifier = Classifier()

    @Test
    fun detectCardPaymentAsTransfer() {
        val debitTxn =
            ParsedTxn(
                amountMinor = 2000000,
                direction = Direction.DEBIT,
                dateTime = 1000L,
                accountLast4 = "1234",
                bankName = "HDFC",
                merchantRaw = "Credit Card XX9876 payment",
                channel = Channel.OTHER,
                balanceAfterMinor = null,
                refNumber = null,
                upiId = null,
                confidence = 1f,
                templateId = null,
            )

        val recentCreditTxn =
            Transaction(
                id = 2L,
                accountId = 2L,
                type = TransactionType.INCOME,
                amount = 2000000,
                dateTime = 1100L,
            )

        val result = classifier.classify(debitTxn, accountId = 1L, recentTransactions = listOf(recentCreditTxn))
        assertEquals(TransactionType.EXPENSE, result.first)
        assertEquals(2L, result.second)
    }

    @Test
    fun detectRefund() {
        val creditTxn =
            ParsedTxn(
                amountMinor = 500,
                direction = Direction.CREDIT,
                dateTime = 1000L + (5 * 24 * 60 * 60 * 1000L),
                accountLast4 = "1234",
                bankName = "HDFC",
                merchantRaw = "Swiggy",
                channel = Channel.UPI,
                balanceAfterMinor = null,
                refNumber = null,
                upiId = null,
                confidence = 1f,
                templateId = null,
            )

        val recentDebitTxn =
            Transaction(
                id = 2L,
                accountId = 1L,
                type = TransactionType.EXPENSE,
                amount = 1000,
                dateTime = 1000L,
                merchantRaw = "Swiggy",
                merchantNormalised = "Swiggy",
            )

        val result = classifier.classify(creditTxn, accountId = 1L, recentTransactions = listOf(recentDebitTxn))
        assertEquals(TransactionType.INCOME, result.first)
        assertEquals(2L, result.second)
    }
}
