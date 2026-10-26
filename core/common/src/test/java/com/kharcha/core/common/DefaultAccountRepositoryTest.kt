package com.kharcha.core.common

import android.content.Context
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Channel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAccountRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tempDir: File
    private lateinit var context: Context
    private lateinit var repository: DefaultAccountRepository

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "account_repo_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        context = mockk()
        every { context.filesDir } returns tempDir
        every { context.applicationContext } returns context
        repository = DefaultAccountRepository(context, testDispatcher)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun resolvesSavingsAndCreditCardAccountsSeparately() =
        runTest(testDispatcher) {
            val savings =
                repository.resolveOrCreateAccount(
                    bankName = "HDFC",
                    last4 = "1234",
                    channel = Channel.UPI,
                )
            val creditCard =
                repository.resolveOrCreateAccount(
                    bankName = "HDFC",
                    last4 = "9876",
                    channel = Channel.CARD,
                )

            assertEquals(AccountType.Savings, savings.type)
            assertEquals("HDFC ••1234", savings.name)

            assertEquals(AccountType.CreditCard, creditCard.type)
            assertEquals("HDFC Card ••9876", creditCard.name)

            assertNotEquals(savings.id, creditCard.id)
        }

    @Test
    fun returnsSameAccountOnSubsequentCalls() =
        runTest(testDispatcher) {
            val acc1 =
                repository.resolveOrCreateAccount(
                    bankName = "SBI",
                    last4 = "4321",
                    channel = Channel.UPI,
                )
            val acc2 =
                repository.resolveOrCreateAccount(
                    bankName = "SBI",
                    last4 = "4321",
                    channel = Channel.UPI,
                )

            assertEquals(acc1.id, acc2.id)
            assertEquals(acc1.name, acc2.name)
            assertEquals(1, repository.getAccountsSync().size)
        }

    @Test
    fun updatesBalanceIfNewer() =
        runTest(testDispatcher) {
            val initial =
                repository.resolveOrCreateAccount(
                    bankName = "ICICI",
                    last4 = "5555",
                    channel = Channel.UPI,
                    balanceMinor = 100000L,
                    timestamp = 1000L,
                )
            assertEquals(100000L, initial.currentBalance)

            val updated =
                repository.resolveOrCreateAccount(
                    bankName = "ICICI",
                    last4 = "5555",
                    channel = Channel.UPI,
                    balanceMinor = 80000L,
                    timestamp = 2000L,
                )
            assertEquals(80000L, updated.currentBalance)
            assertEquals(2000L, updated.balanceUpdatedAt)
        }

    @Test
    fun extractsBankAndLast4FromSmsBody() =
        runTest(testDispatcher) {
            val body = "Rs 1,299 spent on Credit Card XX9876 at AMAZON. Bank: HDFC"
            val account =
                repository.resolveOrCreateAccount(
                    bankName = null,
                    last4 = null,
                    channel = null,
                    smsBody = body,
                )

            assertEquals(AccountType.CreditCard, account.type)
            assertEquals("HDFC", account.bankName)
            assertEquals("9876", account.last4)
        }
}
