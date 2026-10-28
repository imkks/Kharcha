package com.kharcha.engine.classifier

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.ParsedTxn
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifierTest {
    private val classifier = Classifier()

    @Test
    fun defaultToExpenseForDebit() {
        val txn =
            ParsedTxn(
                amountMinor = 1000,
                direction = Direction.DEBIT,
                dateTime = 1000L,
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

        val result = classifier.classify(txn, accountId = 1L)
        assertEquals(TransactionType.Expense, result.first)
    }

    @Test
    fun defaultToIncomeForCredit() {
        val txn =
            ParsedTxn(
                amountMinor = 1000,
                direction = Direction.CREDIT,
                dateTime = 1000L,
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

        val result = classifier.classify(txn, accountId = 1L)
        assertEquals(TransactionType.Income, result.first)
    }

    @Test
    fun detectTransferBetweenAccounts() {
        val debitTxn =
            ParsedTxn(
                amountMinor = 5000,
                direction = Direction.DEBIT,
                dateTime = 1000L,
                accountLast4 = "1234",
                bankName = "HDFC",
                merchantRaw = "Self",
                channel = Channel.UPI,
                balanceAfterMinor = null,
                refNumber = null,
                upiId = null,
                confidence = 1f,
                templateId = null,
            )

        val recentIncomeTxn =
            Transaction(
                id = 2L,
                accountId = 2L,
                type = TransactionType.Income,
                amount = 5000,
                dateTime = 1100L,
            )

        val result = classifier.classify(debitTxn, accountId = 1L, recentTransactions = listOf(recentIncomeTxn))
        assertEquals(TransactionType.Expense, result.first)
        assertEquals(2L, result.second)
    }
}
