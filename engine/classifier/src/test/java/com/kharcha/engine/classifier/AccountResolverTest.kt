package com.kharcha.engine.classifier

import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountResolverTest {
    private val resolver = AccountResolver()

    @Test
    fun resolveExistingAccount() {
        val existing =
            Account(
                id = 1L,
                name = "HDFC 1234",
                bankName = "HDFC",
                last4 = "1234",
                type = AccountType.Savings,
            )
        val result = resolver.resolveAccount("HDFC", "1234", null, listOf(existing))
        assertEquals(1L, result.id)
    }

    @Test
    fun createNewAccount() {
        val result = resolver.resolveAccount("SBI", "9999", null, emptyList())
        assertEquals("SBI 9999", result.name)
        assertEquals("SBI", result.bankName)
        assertEquals("9999", result.last4)
        assertEquals(AccountType.Savings, result.type)
        assertEquals(0L, result.id)
    }

    @Test
    fun createCreditCardAccount() {
        val result = resolver.resolveAccount("AMEX", "1111", Channel.CARD, emptyList())
        assertEquals(AccountType.CreditCard, result.type)
    }

    @Test
    fun updateBalanceIfNewer() {
        val existing =
            Account(
                id = 1L,
                name = "HDFC 1234",
                bankName = "HDFC",
                last4 = "1234",
                type = AccountType.Savings,
                currentBalance = 1000,
                balanceUpdatedAt = 1000L,
            )
        val result =
            resolver.resolveAccount(
                "HDFC",
                "1234",
                null,
                listOf(existing),
                parsedTxnBalance = 500,
                txnDateTime = 2000L,
            )
        assertEquals(500L, result.currentBalance)
        assertEquals(2000L, result.balanceUpdatedAt)
    }
}
