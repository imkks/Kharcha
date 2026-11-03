package com.kharcha.core.common

import android.content.Context
import com.kharcha.core.model.RecurringFrequency
import com.kharcha.core.model.RecurringTransaction
import com.kharcha.core.model.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRecurringTransactionRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk()
    private lateinit var transactionRepository: DefaultTransactionRepository
    private lateinit var recurringRepository: DefaultRecurringTransactionRepository

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("files")
        every { context.filesDir } returns filesDir
        transactionRepository = DefaultTransactionRepository(context, testDispatcher)
        recurringRepository = DefaultRecurringTransactionRepository(context, transactionRepository, testDispatcher)
    }

    @Test
    fun saveAndRetrieveRecurringTransaction() = runTest(testDispatcher) {
        val template = RecurringTransaction(
            merchantName = "Gym Membership",
            amountMinor = 150000L,
            frequency = RecurringFrequency.MONTHLY,
            dayOfMonth = 1,
        )

        val id = recurringRepository.saveRecurringTransaction(template)
        advanceUntilIdle()

        assertTrue(id > 0L)
        val all = recurringRepository.getRecurringTransactionsSync()
        assertEquals(1, all.size)
        assertEquals("Gym Membership", all.first().merchantName)
        assertEquals(150000L, all.first().amountMinor)

        // Reload from disk into fresh repository instance
        val reloaded = DefaultRecurringTransactionRepository(context, transactionRepository, testDispatcher)
        val reloadedAll = reloaded.getRecurringTransactionsSync()
        assertEquals(1, reloadedAll.size)
        assertEquals("Gym Membership", reloadedAll.first().merchantName)
    }

    @Test
    fun deleteRecurringTransactionRemovesItem() = runTest(testDispatcher) {
        val id1 = recurringRepository.saveRecurringTransaction(
            RecurringTransaction(merchantName = "Item 1", amountMinor = 1000L)
        )
        val id2 = recurringRepository.saveRecurringTransaction(
            RecurringTransaction(merchantName = "Item 2", amountMinor = 2000L)
        )
        advanceUntilIdle()

        assertEquals(2, recurringRepository.getRecurringTransactionsSync().size)

        recurringRepository.deleteRecurringTransaction(id1)
        advanceUntilIdle()

        val remaining = recurringRepository.getRecurringTransactionsSync()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining.first().id)
    }

    @Test
    fun setActiveTogglesActiveState() = runTest(testDispatcher) {
        val id = recurringRepository.saveRecurringTransaction(
            RecurringTransaction(merchantName = "Netflix", amountMinor = 64900L, isActive = true)
        )
        advanceUntilIdle()

        recurringRepository.setActive(id, false)
        advanceUntilIdle()

        assertFalse(recurringRepository.getRecurringTransactionsSync().first().isActive)
    }

    @Test
    fun materializeDueInsertsTransactionsAndAdvancesNextDueDate() = runTest(testDispatcher) {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 3, 10)
        val dueEpoch = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        recurringRepository.saveRecurringTransaction(
            RecurringTransaction(
                merchantName = "Milk Subscription",
                amountMinor = 3000L,
                frequency = RecurringFrequency.DAILY,
                nextDueDateEpoch = dueEpoch,
                isActive = true,
            )
        )
        recurringRepository.saveRecurringTransaction(
            RecurringTransaction(
                merchantName = "Paused Service",
                amountMinor = 5000L,
                frequency = RecurringFrequency.DAILY,
                nextDueDateEpoch = dueEpoch,
                isActive = false,
            )
        )
        advanceUntilIdle()

        val materialized = recurringRepository.materializeDue(today)
        advanceUntilIdle()

        assertEquals(1, materialized)

        // Verify transaction inserted
        val txns = transactionRepository.getTransactionsSync()
        assertEquals(1, txns.size)
        assertEquals("Milk Subscription", txns.first().merchantNormalised)
        assertEquals(3000L, txns.first().amount)
        assertEquals(TransactionType.Expense, txns.first().type)

        // Verify template advanced to tomorrow (March 11)
        val updatedTemplate = recurringRepository.getRecurringTransactionsSync().first { it.id == 1L }
        val expectedNextEpoch = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        assertEquals(expectedNextEpoch, updatedTemplate.nextDueDateEpoch)
    }
}
