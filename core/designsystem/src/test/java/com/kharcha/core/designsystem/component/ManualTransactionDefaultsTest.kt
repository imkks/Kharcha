package com.kharcha.core.designsystem.component

import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualTransactionDefaultsTest {
    @Test
    fun defaultsToCashEvenWhenAnotherAccountComesFirst() {
        val bank = Account(id = 1L, name = "Bank", bankName = "Bank", last4 = null, type = AccountType.Savings)
        val cash = Account(id = 2L, name = "Cash", bankName = null, last4 = null, type = AccountType.Cash)

        assertEquals(cash, defaultManualTransactionAccount(listOf(bank, cash)))
    }

    @Test
    fun noCashAccountLeavesManualEntryOnCashFallback() {
        val bank = Account(id = 1L, name = "Bank", bankName = "Bank", last4 = null, type = AccountType.Savings)

        assertNull(defaultManualTransactionAccount(listOf(bank)))
    }
}
