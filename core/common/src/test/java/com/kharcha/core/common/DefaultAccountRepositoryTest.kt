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
        val prefs: android.content.SharedPreferences = mockk(relaxed = true)
        val editor: android.content.SharedPreferences.Editor = mockk(relaxed = true)
        every { editor.putStringSet(any(), any()) } returns editor
        every { editor.commit() } returns true
        every { prefs.edit() } returns editor
        every { context.getSharedPreferences(any(), any()) } returns prefs
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

    @Test
    fun normalizesMaskedPanWhenResolvingAccount() =
        runTest(testDispatcher) {
            val account =
                repository.resolveOrCreateAccount(
                    bankName = "ICICI",
                    last4 = "4620XXXXXXXX6567",
                    channel = Channel.CARD,
                )

            assertEquals(AccountType.CreditCard, account.type)
            assertEquals("6567", account.last4)
            assertEquals("ICICI Card ••6567", account.name)
        }

    @Test
    fun extractsCardFromHdfcBankCardSpendBody() =
        runTest(testDispatcher) {
            val body = "Rs 1,499.00 spent on HDFC Bank Card 8302 At ASSPL on 24-10-24"
            val account =
                repository.resolveOrCreateAccount(
                    bankName = null,
                    last4 = null,
                    channel = null,
                    smsBody = body,
                )

            assertEquals(AccountType.CreditCard, account.type)
            assertEquals("HDFC", account.bankName)
            assertEquals("8302", account.last4)
            assertEquals("HDFC Card ••8302", account.name)
        }

    @Test
    fun resolvesAutoDebitCreatesSavingsAndLinksCreditCard() =
        runTest(testDispatcher) {
            val body =
                "Dear Customer, thank you for your payment of INR 11,311.00 towards ICICI Bank Credit Card Account XX1268 through Auto Debit from Account XX2321 on 04-Oct-26"
            val account =
                repository.resolveOrCreateAccount(
                    bankName = "ICICI",
                    last4 = "1268",
                    channel = Channel.CARD,
                    smsBody = body,
                )

            assertEquals(AccountType.CreditCard, account.type)
            assertEquals("ICICI", account.bankName)
            assertEquals("1268", account.last4)
            assertEquals("ICICI Card ••1268", account.name)
        }

    @Test
    fun resolvesCardmemberPaymentAsCreditCard() =
        runTest(testDispatcher) {
            val body =
                "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 595.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 8302 ON 2-6-2025.YOUR AVAILABLE LIMIT IS RS. 154621.36"
            val account =
                repository.resolveOrCreateAccount(
                    bankName = "HDFC",
                    last4 = "8302",
                    channel = Channel.CARD,
                    smsBody = body,
                )

            assertEquals(AccountType.CreditCard, account.type)
            assertEquals("HDFC", account.bankName)
            assertEquals("8302", account.last4)
            assertEquals("HDFC Card ••8302", account.name)
        }

    @Test
    fun resolvesAccountDebitForCreditCardBillAsSavings() =
        runTest(testDispatcher) {
            val body =
                "ICICI Bank Acct XX321 debited for Rs 96808.63 on 05-Aug-26; Credit Card Bil credited. UPI:621717806116. Call 18002662 for dispute. SMS BLOCK 321 to 9215676766."
            val account =
                repository.resolveOrCreateAccount(
                    bankName = "ICICI",
                    last4 = "321",
                    channel = Channel.UPI,
                    smsBody = body,
                )

            assertEquals(AccountType.Savings, account.type)
            assertEquals("ICICI", account.bankName)
            assertEquals("321", account.last4)
            assertEquals("ICICI ••321", account.name)
        }

    @Test
    fun resolvesAsteriskMaskedAccountInHdfcTransfer() =
        runTest(testDispatcher) {
            val body =
                "HDFC Bank:Rs. 53000.00 debited from a/c *1999 on 04/10/26 to a/c **0034 (UPI Ref No. 615324150359). Not you? Call on 18002586161 to report"
            val account =
                repository.resolveOrCreateAccount(
                    bankName = "HDFC",
                    last4 = "1999",
                    channel = Channel.UPI,
                    smsBody = body,
                )

            assertEquals(AccountType.Savings, account.type)
            assertEquals("HDFC", account.bankName)
            assertEquals("1999", account.last4)
            assertEquals("HDFC ••1999", account.name)
        }

    @Test
    fun resolvesRupayCreditCardUpiAsCreditCard() =
        runTest(testDispatcher) {
            val body = "INR 500 debited from Credit Card ending 8302 on 24-Oct-26 for UPI/4291823..."
            val account =
                repository.resolveOrCreateAccount(
                    bankName = "HDFC",
                    last4 = "8302",
                    channel = Channel.UPI,
                    smsBody = body,
                )

            assertEquals(AccountType.CreditCard, account.type)
            assertEquals("HDFC", account.bankName)
            assertEquals("8302", account.last4)
            assertEquals("HDFC Card ••8302", account.name)
        }

    @Test
    fun consolidates3DigitAnd4DigitMaskedAccounts() =
        runTest(testDispatcher) {
            val first =
                repository.resolveOrCreateAccount(
                    bankName = "ICICI",
                    last4 = "321",
                    channel = Channel.UPI,
                    smsBody = "ICICI Bank Acct XX321 debited for Rs 96808.63",
                )
            assertEquals("321", first.last4)
            assertEquals("ICICI ••321", first.name)

            val second =
                repository.resolveOrCreateAccount(
                    bankName = "ICICI",
                    last4 = "4321",
                    channel = Channel.UPI,
                    smsBody = "ICICI Bank Acct XX4321 debited for Rs 500.00",
                )
            assertEquals(first.id, second.id)
            assertEquals("4321", second.last4)
            assertEquals("ICICI ••4321", second.name)

            val third =
                repository.resolveOrCreateAccount(
                    bankName = "ICICI",
                    last4 = "321",
                    channel = Channel.UPI,
                    smsBody = "ICICI Bank Acct XX321 debited for Rs 100.00",
                )
            assertEquals(first.id, third.id)
            assertEquals("4321", third.last4)
            assertEquals(1, repository.getAccountsSync().size)
        }

    @Test
    fun mergeAccounts_reassignsTransactionsAndLinks() =
        runTest(testDispatcher) {
            val source =
                repository.resolveOrCreateAccount(
                    bankName = "HDFC",
                    last4 = "1111",
                    channel = Channel.UPI,
                    smsBody = "HDFC Bank A/c XX1111 debited for Rs 100",
                )
            val target =
                repository.resolveOrCreateAccount(
                    bankName = "HDFC",
                    last4 = "2222",
                    channel = Channel.IMPS,
                    smsBody = "HDFC Bank A/c XX2222 debited for Rs 200",
                )
            val card =
                repository.resolveOrCreateAccount(
                    bankName = "HDFC",
                    last4 = "9988",
                    channel = Channel.CARD,
                    smsBody = "Spent on HDFC Bank Card ending 9988",
                )

            // Link card to source
            repository.linkSubAccounts(source.id, setOf(card.id))
            assertEquals(source.id, repository.getAccountById(card.id)?.linkedPrimaryAccountId)

            val txnRepo = DefaultTransactionRepository(context, testDispatcher)
            val txn = com.kharcha.core.model.Transaction(
                id = 1001L,
                accountId = source.id,
                amount = 10000L,
                type = com.kharcha.core.model.TransactionType.Expense,
                dateTime = System.currentTimeMillis(),
            )
            txnRepo.insertTransaction(txn)

            val success = repository.mergeAccounts(source.id, target.id, txnRepo)
            assertEquals(true, success)

            // Source account deleted
            assertEquals(null, repository.getAccountById(source.id))
            // Target account remains
            assertEquals(target.id, repository.getAccountById(target.id)?.id)
            // Card re-linked to target
            assertEquals(target.id, repository.getAccountById(card.id)?.linkedPrimaryAccountId)
            // Transaction moved to target
            val updatedTxn = txnRepo.getTransactionsSync().find { it.id == 1001L }
            assertEquals(target.id, updatedTxn?.accountId)
        }
}
