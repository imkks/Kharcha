package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class AccountDetailTest {

    private fun createTxn(
        id: Long,
        accountId: Long,
        type: TransactionType,
        amount: Long,
        dateTime: Long = 1729000000000L,
    ): Transaction = Transaction(
        id = id,
        accountId = accountId,
        type = type,
        amount = amount,
        dateTime = dateTime,
        merchantNormalised = "Store",
        categorySource = "Shopping",
        channel = Channel.CARD,
        isReviewed = true,
        createdAt = dateTime,
        updatedAt = dateTime,
    )

    @Test
    fun testAssociatedAccountTransactionsAggregation() {
        val primary = Account(
            id = 10L,
            name = "HDFC Bank",
            bankName = "HDFC",
            last4 = "1999",
            type = AccountType.Savings,
            currentBalance = 110460_00L,
        )
        val creditCard = Account(
            id = 20L,
            name = "HDFC Credit Card",
            bankName = "HDFC",
            last4 = "8302",
            type = AccountType.CreditCard,
            linkedPrimaryAccountId = 10L,
        )
        val otherAccount = Account(
            id = 30L,
            name = "ICICI Bank",
            bankName = "ICICI",
            last4 = "5820",
            type = AccountType.Savings,
        )

        val allAccounts = listOf(primary, creditCard, otherAccount)
        val linkedIds = allAccounts.filter { it.linkedPrimaryAccountId == primary.id }.map { it.id }
        val allAccountIds = (setOf(primary.id) + linkedIds).toSet()

        val txns = listOf(
            createTxn(id = 1L, accountId = 10L, type = TransactionType.Expense, amount = 1500_00L),
            createTxn(id = 2L, accountId = 20L, type = TransactionType.Expense, amount = 2500_00L),
            createTxn(id = 3L, accountId = 10L, type = TransactionType.Income, amount = 50000_00L),
            createTxn(id = 4L, accountId = 30L, type = TransactionType.Expense, amount = 999_00L),
        )

        val accountTxns = txns.filter { it.accountId in allAccountIds }
        assertEquals(3, accountTxns.size)
        assertTrue(accountTxns.any { it.id == 1L })
        assertTrue(accountTxns.any { it.id == 2L })
        assertTrue(accountTxns.any { it.id == 3L })

        val totalExpense = accountTxns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
        assertEquals(4000_00L, totalExpense)

        val totalIncome = accountTxns.filter { it.type == TransactionType.Income }.sumOf { it.amount }
        assertEquals(50000_00L, totalIncome)
    }

    @Test
    fun testMonthFilteringForAccountTransactions() {
        val zoneId = ZoneId.systemDefault()
        // October 2026 timestamp
        val oct2026Epoch = 1793045735000L
        // September 2026 timestamp (roughly 30 days earlier)
        val sep2026Epoch = oct2026Epoch - (30L * 24L * 3600L * 1000L)

        val octYm = YearMonth.from(Instant.ofEpochMilli(oct2026Epoch).atZone(zoneId).toLocalDate())
        val sepYm = YearMonth.from(Instant.ofEpochMilli(sep2026Epoch).atZone(zoneId).toLocalDate())

        val txns =
            listOf(
                createTxn(
                    id = 1L,
                    accountId = 10L,
                    type = TransactionType.Expense,
                    amount = 1000_00L,
                    dateTime = oct2026Epoch,
                ),
                createTxn(
                    id = 2L,
                    accountId = 10L,
                    type = TransactionType.Expense,
                    amount = 2000_00L,
                    dateTime = sep2026Epoch,
                ),
            )

        val octTxns = txns.filter {
            YearMonth.from(Instant.ofEpochMilli(it.dateTime).atZone(zoneId).toLocalDate()) == octYm
        }
        assertEquals(1, octTxns.size)
        assertEquals(1L, octTxns.first().id)
        assertEquals(1000_00L, octTxns.first().amount)

        val sepTxns = txns.filter {
            YearMonth.from(Instant.ofEpochMilli(it.dateTime).atZone(zoneId).toLocalDate()) == sepYm
        }
        assertEquals(1, sepTxns.size)
        assertEquals(2L, sepTxns.first().id)
    }
}
