package com.kharcha.core.common

import android.content.Context
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultTransactionRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk()
    private lateinit var repository: DefaultTransactionRepository

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("files")
        every { context.filesDir } returns filesDir
        repository = DefaultTransactionRepository(context, testDispatcher)
    }

    @Test
    fun deduplicatesExactReferenceNumber() =
        runTest(testDispatcher) {
            val txn1 =
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    smsHash = "hash1",
                    amount = 50000L,
                    refNumber = "UPI123456",
                    type = TransactionType.Expense,
                    dateTime = 1000L,
                )
            val txn2 =
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    smsHash = "hash2",
                    amount = 50000L,
                    refNumber = "UPI123456",
                    type = TransactionType.Expense,
                    dateTime = 2000L,
                )

            repository.insertTransactions(listOf(txn1, txn2))
            testScheduler.advanceUntilIdle()

            val list = repository.getTransactions().first()
            assertEquals(1, list.size)
            assertEquals("UPI123456", list[0].refNumber)
        }

    @Test
    fun deduplicatesNearDuplicateTransactions() =
        runTest(testDispatcher) {
            val txn1 =
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    amount = 25000L,
                    merchantNormalised = "Swiggy",
                    type = TransactionType.Expense,
                    dateTime = 1000L,
                    channel = Channel.UPI,
                )
            val txn2 =
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    amount = 25000L,
                    merchantNormalised = "Swiggy",
                    type = TransactionType.Expense,
                    dateTime = 3000L, // 2 seconds later
                    channel = Channel.UPI,
                )

            repository.insertTransactions(listOf(txn1, txn2))
            testScheduler.advanceUntilIdle()

            val list = repository.getTransactions().first()
            assertEquals(1, list.size)
        }

    @Test
    fun reconcilesOwnAccountTransfers() =
        runTest(testDispatcher) {
            val debit =
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    amount = 100000L,
                    type = TransactionType.Expense,
                    dateTime = 1000L,
                    channel = Channel.UPI,
                )
            val credit =
                Transaction(
                    id = 2L,
                    accountId = 2L,
                    amount = 100000L,
                    type = TransactionType.Income,
                    dateTime = 2000L,
                    channel = Channel.UPI,
                )

            repository.insertTransactions(listOf(debit, credit))
            testScheduler.advanceUntilIdle()

            val list = repository.getTransactions().first()
            assertEquals(2, list.size)
            assertEquals(TransactionType.Transfer, list[0].type)
            assertEquals(TransactionType.Transfer, list[1].type)
            assertEquals(2L, list.find { it.id == 1L }?.linkedTransactionId)
            assertEquals(1L, list.find { it.id == 2L }?.linkedTransactionId)
        }

    @Test
    fun cardSpendsCountAsExpenseWhileCardBillPaymentIsTransfer() =
        runTest(testDispatcher) {
            val cardSpend =
                Transaction(
                    id = 101L,
                    accountId = 10L,
                    amount = 500000L,
                    type = TransactionType.Expense,
                    merchantNormalised = "Amazon",
                    categorySource = "Shopping",
                    dateTime = 1000L,
                    channel = Channel.CARD,
                )
            val bankDebitForBill =
                Transaction(
                    id = 102L,
                    accountId = 1L,
                    amount = 500000L,
                    type = TransactionType.Transfer,
                    categorySource = "Transfer",
                    dateTime = 2000L,
                    channel = Channel.UPI,
                )
            val cardCreditPaymentReceived =
                Transaction(
                    id = 103L,
                    accountId = 10L,
                    amount = 500000L,
                    type = TransactionType.Transfer,
                    categorySource = "Transfer",
                    dateTime = 2500L,
                    channel = Channel.CARD,
                )

            repository.insertTransactions(listOf(cardSpend, bankDebitForBill, cardCreditPaymentReceived))
            testScheduler.advanceUntilIdle()

            val txns = repository.getTransactions().first()
            assertEquals(3, txns.size)

            val totalExpenses = txns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
            assertEquals(500000L, totalExpenses)

            val debitBill = txns.first { it.id == 102L }
            val creditBill = txns.first { it.id == 103L }
            assertEquals(TransactionType.Transfer, debitBill.type)
            assertEquals(TransactionType.Transfer, creditBill.type)
            assertEquals(103L, debitBill.linkedTransactionId)
            assertEquals(102L, creditBill.linkedTransactionId)
        }

    @Test
    fun propagatesCategoryToAllMerchantTransactionsAndSavesRule() =
        runTest(testDispatcher) {
            val prefs: UserPreferences = mockk(relaxed = true)
            val txn1 =
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    amount = 50000L,
                    merchantNormalised = "Blue Tokai",
                    categorySource = "Other",
                    type = TransactionType.Expense,
                    dateTime = 1000L,
                    isReviewed = false,
                )
            val txn2 =
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    amount = 65000L,
                    merchantNormalised = "Blue Tokai",
                    categorySource = "Other",
                    type = TransactionType.Expense,
                    dateTime = 2000L,
                    isReviewed = false,
                )

            repository.insertTransactions(listOf(txn1, txn2))
            testScheduler.advanceUntilIdle()

            val updatedCount =
                repository.updateTransactionAndPropagateMerchant(
                    transaction = txn1,
                    newCategory = "Food & Dining",
                    userPreferences = prefs,
                )
            testScheduler.advanceUntilIdle()

            assertEquals(2, updatedCount)
            val list = repository.getTransactions().first()
            assertEquals("Food & Dining", list[0].categorySource)
            assertEquals("Food & Dining", list[1].categorySource)
            org.junit.Assert.assertTrue(list[0].isReviewed)
            org.junit.Assert.assertTrue(list[1].isReviewed)
            io.mockk.verify { prefs.setMerchantCategory("blue tokai", "Food & Dining") }
        }

    @Test
    fun linkAndUnlinkTransactionsManually() =
        runTest(testDispatcher) {
            val expense =
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    amount = 251100L,
                    type = TransactionType.Expense,
                    merchantNormalised = "Urban Company",
                    dateTime = 1000L,
                )
            val refund =
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    amount = 251100L,
                    type = TransactionType.Income,
                    merchantNormalised = "Urban Company",
                    dateTime = 2000L,
                )

            repository.insertTransactions(listOf(expense, refund))
            testScheduler.advanceUntilIdle()

            repository.linkTransactions(1L, 2L, isRefund = true)
            testScheduler.advanceUntilIdle()

            val linkedList = repository.getTransactions().first()
            val linkedExpense = linkedList.first { it.id == 1L }
            val linkedRefund = linkedList.first { it.id == 2L }

            assertEquals(2L, linkedExpense.linkedTransactionId)
            assertEquals(1L, linkedRefund.linkedTransactionId)
            assertEquals(TransactionType.Refund, linkedRefund.type)

            repository.unlinkTransactions(1L)
            testScheduler.advanceUntilIdle()

            val unlinkedList = repository.getTransactions().first()
            org.junit.Assert.assertNull(unlinkedList.first { it.id == 1L }.linkedTransactionId)
            org.junit.Assert.assertNull(unlinkedList.first { it.id == 2L }.linkedTransactionId)
        }

    @Test
    fun autoReconcilesRefundWithin90Days() =
        runTest(testDispatcher) {
            val expense =
                Transaction(
                    id = 10L,
                    accountId = 1L,
                    amount = 45000L,
                    type = TransactionType.Expense,
                    merchantNormalised = "Swiggy",
                    dateTime = 1000L,
                )
            val refund =
                Transaction(
                    id = 20L,
                    accountId = 1L,
                    amount = 45000L,
                    type = TransactionType.Refund,
                    merchantNormalised = "Swiggy",
                    dateTime = 1000L + (5L * 24 * 60 * 60 * 1000L), // 5 days later
                )

            repository.insertTransactions(listOf(expense, refund))
            testScheduler.advanceUntilIdle()

            val list = repository.getTransactions().first()
            assertEquals(20L, list.first { it.id == 10L }.linkedTransactionId)
            assertEquals(10L, list.first { it.id == 20L }.linkedTransactionId)
        }
}
