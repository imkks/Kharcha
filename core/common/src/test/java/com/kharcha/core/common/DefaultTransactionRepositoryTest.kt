package com.kharcha.core.common

import android.content.Context
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.isCounted
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun migratesLegacySmsNoteIntoSeparateSmsBodyField() {
        val body = "Paid Rs 45 at Cafe"
        val file = java.io.File(context.filesDir, "transactions.json")
        file.writeText(
            """[{"id":7,"smsHash":"legacy-hash","accountId":1,"type":"Expense","amount":4500,"currency":"INR","dateTime":1000,"note":"$body"}]""",
        )

        val restored = DefaultTransactionRepository(context, testDispatcher)
        val transaction = restored.getTransactionsSync().single()

        assertNull(transaction.note)
        assertEquals(body, transaction.smsBody)
    }

    @Test
    fun preservesUserNoteAndSmsBodyAsSeparateFieldsOnDisk() =
        runTest(testDispatcher) {
            val transaction =
                Transaction(
                    id = 8L,
                    smsHash = "new-hash",
                    amount = 4500L,
                    type = TransactionType.Expense,
                    dateTime = 1000L,
                    note = "Team lunch",
                    smsBody = "Paid Rs 45 at Cafe",
                )
            repository.insertTransaction(transaction)
            testScheduler.advanceUntilIdle()

            val restored = DefaultTransactionRepository(context, testDispatcher).getTransactionsSync().single()
            assertEquals("Team lunch", restored.note)
            assertEquals("Paid Rs 45 at Cafe", restored.smsBody)
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
            assertEquals(TransactionType.Income, list[0].type)
            assertEquals("Transfer", list[0].categorySource)
            assertEquals(TransactionType.Expense, list[1].type)
            assertEquals("Transfer", list[1].categorySource)
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
                    type = TransactionType.Expense,
                    categorySource = "Transfer",
                    dateTime = 2000L,
                    channel = Channel.UPI,
                )
            val cardCreditPaymentReceived =
                Transaction(
                    id = 103L,
                    accountId = 10L,
                    amount = 500000L,
                    type = TransactionType.Income,
                    categorySource = "Transfer",
                    dateTime = 2500L,
                    channel = Channel.CARD,
                )

            repository.insertTransactions(listOf(cardSpend, bankDebitForBill, cardCreditPaymentReceived))
            testScheduler.advanceUntilIdle()

            val txns = repository.getTransactions().first()
            assertEquals(3, txns.size)

            val totalCountedExpenses = txns.filter { it.isCounted }.sumOf { it.amount }
            assertEquals(500000L, totalCountedExpenses)

            val debitBill = txns.first { it.id == 102L }
            val creditBill = txns.first { it.id == 103L }
            assertEquals(TransactionType.Expense, debitBill.type)
            assertEquals("Transfer", debitBill.categorySource)
            assertEquals(TransactionType.Income, creditBill.type)
            assertEquals("Transfer", creditBill.categorySource)
            assertEquals(103L, debitBill.linkedTransactionId)
            assertEquals(102L, creditBill.linkedTransactionId)
        }

    @Test
    fun selfTransferWithSameUpiRefNumberPreservesBothLegs() =
        runTest(testDispatcher) {
            val iciciDebit =
                Transaction(
                    id = 19446L,
                    accountId = 1L,
                    bankName = "ICICI",
                    amount = 8000000L,
                    type = TransactionType.Expense,
                    refNumber = "218683710640",
                    dateTime = 1792657500000L,
                    channel = Channel.UPI,
                )
            val hdfcCredit =
                Transaction(
                    id = 19447L,
                    accountId = 2L,
                    bankName = "HDFC",
                    amount = 8000000L,
                    type = TransactionType.Income,
                    refNumber = "218683710640",
                    dateTime = 1792657655440L,
                    channel = Channel.UPI,
                )

            repository.insertTransactions(listOf(iciciDebit, hdfcCredit))
            testScheduler.advanceUntilIdle()

            val txns = repository.getTransactions().first()
            assertEquals(2, txns.size)
            val debit = txns.first { it.id == 19446L }
            val credit = txns.first { it.id == 19447L }
            assertEquals(TransactionType.Expense, debit.type)
            assertEquals("Transfer", debit.categorySource)
            assertEquals(TransactionType.Income, credit.type)
            assertEquals("Transfer", credit.categorySource)
            assertEquals(credit.id, debit.linkedTransactionId)
            assertEquals(debit.id, credit.linkedTransactionId)
        }

    @Test
    fun differentBankSameAmountWithinThreeMinutesNotNearDuplicate() =
        runTest(testDispatcher) {
            val hdfcDebit =
                Transaction(
                    id = 19446L,
                    accountId = 1L,
                    bankName = "HDFC",
                    accountLast4 = "1999",
                    amount = 8000000L,
                    type = TransactionType.Expense,
                    merchantRaw = "MUTUAL FUNDS NCL",
                    merchantNormalised = "Mutual Funds Ncl",
                    refNumber = "131687401648",
                    dateTime = 1792657503428L, // 13:55:03
                    channel = Channel.UPI,
                )
            val iciciDebit =
                Transaction(
                    id = 19447L,
                    accountId = 2L,
                    bankName = "ICICI",
                    accountLast4 = "321",
                    amount = 8000000L,
                    type = TransactionType.Expense,
                    merchantRaw = "KRISHNA KUMAR S",
                    merchantNormalised = "Krishna Kumar S",
                    refNumber = "218683710640",
                    dateTime = 1792657655440L, // 13:57:35 (~152s later)
                    channel = Channel.UPI,
                )

            repository.insertTransactions(listOf(hdfcDebit, iciciDebit))
            testScheduler.advanceUntilIdle()

            val txns = repository.getTransactions().first()
            assertEquals(2, txns.size)
            val hdfc = txns.first { it.id == 19446L }
            val icici = txns.first { it.id == 19447L }
            assertEquals("HDFC", hdfc.bankName)
            assertEquals("ICICI", icici.bankName)
            assertEquals(8000000L, hdfc.amount)
            assertEquals(8000000L, icici.amount)
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
            assertEquals(TransactionType.Income, linkedRefund.type)
            org.junit.Assert.assertFalse(linkedRefund.isCounted)

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
                    type = TransactionType.Income,
                    categorySource = "Refund",
                    merchantNormalised = "Swiggy",
                    dateTime = 1000L + (5L * 24 * 60 * 60 * 1000L), // 5 days later
                )

            repository.insertTransactions(listOf(expense, refund))
            testScheduler.advanceUntilIdle()

            val list = repository.getTransactions().first()
            assertEquals(20L, list.first { it.id == 10L }.linkedTransactionId)
            assertEquals(10L, list.first { it.id == 20L }.linkedTransactionId)
        }

    @Test
    fun persistsLocationCoordinatesAndLabel() =
        runTest(testDispatcher) {
            val txn =
                Transaction(
                    id = 101L,
                    accountId = 1L,
                    amount = 32000L,
                    type = TransactionType.Expense,
                    merchantNormalised = "Blue Tokai",
                    dateTime = 5000L,
                    latitude = 12.9716,
                    longitude = 77.5946,
                    locationLabel = "Indiranagar 100ft Rd",
                )

            repository.insertTransactions(listOf(txn))
            testScheduler.advanceUntilIdle()

            // Create a new instance pointing to the same folder to verify disk reload
            val newRepo = DefaultTransactionRepository(context, testDispatcher)
            val list = newRepo.getTransactions().first()
            val loaded = list.first { it.id == 101L }

            assertEquals(12.9716, loaded.latitude!!, 0.0001)
            assertEquals(77.5946, loaded.longitude!!, 0.0001)
            assertEquals("Indiranagar 100ft Rd", loaded.locationLabel)
        }
}
