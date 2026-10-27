package com.kharcha.core.common

import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountAutoLinkerTest {

    @Test
    fun testSameBankLinks_linksMultipleCreditCardsToPrimaryBank() {
        val primaryBank = Account(
            id = 1L,
            name = "ICICI Bank ••1234",
            bankName = "ICICI",
            last4 = "1234",
            type = AccountType.Savings,
        )
        val card1 = Account(
            id = 2L,
            name = "ICICI Credit Card ••8302",
            bankName = "ICICI",
            last4 = "8302",
            type = AccountType.CreditCard,
        )
        val card2 = Account(
            id = 3L,
            name = "ICICI Amazon Pay ••9911",
            bankName = "ICICI",
            last4 = "9911",
            type = AccountType.CreditCard,
        )
        val hdfcCard = Account(
            id = 4L,
            name = "HDFC Regalia ••5544",
            bankName = "HDFC",
            last4 = "5544",
            type = AccountType.CreditCard,
        )

        val accounts = listOf(primaryBank, card1, card2, hdfcCard)
        val result = AccountAutoLinker.autoLinkAccounts(accounts)

        val linkedCard1 = result.find { it.id == 2L }
        val linkedCard2 = result.find { it.id == 3L }
        val linkedHdfcCard = result.find { it.id == 4L }

        assertEquals(1L, linkedCard1?.linkedPrimaryAccountId)
        assertEquals(1L, linkedCard2?.linkedPrimaryAccountId)
        assertNull(linkedHdfcCard?.linkedPrimaryAccountId)
    }

    @Test
    fun testBillPaymentFromSms_linksCardToSourceBank() {
        val iciciBank = Account(
            id = 10L,
            name = "ICICI Bank ••1234",
            bankName = "ICICI",
            last4 = "1234",
            type = AccountType.Savings,
        )
        val creditCard = Account(
            id = 20L,
            name = "ICICI Credit Card ••8302",
            bankName = "ICICI",
            last4 = "8302",
            type = AccountType.CreditCard,
        )

        val txn = Transaction(
            id = 100L,
            accountId = 10L,
            type = TransactionType.Expense,
            amount = 1500000L,
            dateTime = System.currentTimeMillis(),
            note = "Dear Customer, your ICICI Bank Acc XX1234 has been debited for Rs 15,000.00 on 12-Oct-24 towards ICICI Bank Credit Card ending with 8302. Info: BIL/000123.",
        )

        val accounts = listOf(iciciBank, creditCard)
        val result = AccountAutoLinker.autoLinkAccounts(accounts, listOf(txn))

        val linkedCard = result.find { it.id == 20L }
        assertEquals(10L, linkedCard?.linkedPrimaryAccountId)
    }

    @Test
    fun testExplicitlyUnlinkedIds_areNotReLinked() {
        val primaryBank = Account(
            id = 1L,
            name = "ICICI Bank ••1234",
            bankName = "ICICI",
            last4 = "1234",
            type = AccountType.Savings,
        )
        val card = Account(
            id = 2L,
            name = "ICICI Credit Card ••8302",
            bankName = "ICICI",
            last4 = "8302",
            type = AccountType.CreditCard,
        )

        val accounts = listOf(primaryBank, card)
        // User deliberately unlinked card (id=2)
        val result = AccountAutoLinker.autoLinkAccounts(accounts, explicitlyUnlinkedIds = setOf(2L))

        val linkedCard = result.find { it.id == 2L }
        assertNull(linkedCard?.linkedPrimaryAccountId)
    }
}
