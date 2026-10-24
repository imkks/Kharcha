package com.kharcha.core.common

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
}
