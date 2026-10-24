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
}
