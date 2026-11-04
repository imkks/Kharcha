package com.kharcha.feature.home.ui

import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Channel
import com.kharcha.core.model.RecurringFrequency
import com.kharcha.core.model.RecurringTransaction
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.buildRefundMap
import com.kharcha.core.model.forTotals
import com.kharcha.core.model.isCounted
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class HomeScreenCalculationTest {
    private val zoneId = ZoneId.of("Asia/Kolkata")
    private val testYearMonth = YearMonth.of(2026, 10)

    @Test
    fun testMonthlySpendAndIncomeAggregation() {
        val octEpoch1 = Instant.parse("2026-10-15T10:00:00Z").toEpochMilli()
        val octEpoch2 = Instant.parse("2026-10-18T15:30:00Z").toEpochMilli()
        val octEpochIncome = Instant.parse("2026-10-01T09:00:00Z").toEpochMilli()
        val sepEpoch = Instant.parse("2026-09-20T12:00:00Z").toEpochMilli()

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = 450_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = octEpoch1,
                merchantRaw = "Swiggy",
            ),
            Transaction(
                id = 2L,
                amount = 1_200_00L,
                type = TransactionType.Expense,
                channel = Channel.CARD,
                dateTime = octEpoch2,
                merchantRaw = "BESCOM",
            ),
            Transaction(
                id = 3L,
                amount = 85_000_00L,
                type = TransactionType.Income,
                channel = Channel.NEFT,
                dateTime = octEpochIncome,
                merchantRaw = "Acme Corp",
            ),
            Transaction(
                id = 4L,
                amount = 999_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = sepEpoch,
                merchantRaw = "Amazon",
            ),
        )

        val octTransactions = transactions.filter {
            YearMonth.from(Instant.ofEpochMilli(it.dateTime).atZone(zoneId)) == testYearMonth
        }

        val totalSpent = octTransactions
            .filter { it.type == TransactionType.Expense }
            .sumOf { it.amount }

        val totalIncome = octTransactions
            .filter { it.type == TransactionType.Income }
            .sumOf { it.amount }

        assertEquals(1_650_00L, totalSpent)
        assertEquals(85_000_00L, totalIncome)
    }

    @Test
    fun testCreditCardBillPaymentTransferDoesNotDoubleCountInSpend() {
        val octEpoch = Instant.parse("2026-10-15T10:00:00Z").toEpochMilli()
        val cardSpend = Transaction(
            id = 1L,
            amount = 11_311_00L,
            type = TransactionType.Expense,
            channel = Channel.CARD,
            dateTime = octEpoch,
            merchantRaw = "Amazon",
            categorySource = "Shopping",
        )
        val billPaymentDebit = Transaction(
            id = 2L,
            amount = 11_311_00L,
            type = TransactionType.Expense,
            channel = Channel.CARD,
            dateTime = octEpoch,
            merchantRaw = "ICICI Bank Credit Card",
            categorySource = "Bills",
            isCounted = false,
        )

        val transactions = listOf(cardSpend, billPaymentDebit)
        val countedExpenses = transactions
            .filter { it.type == TransactionType.Expense && it.isCounted }
            .sumOf { it.amount }

        assertEquals(11_311_00L, countedExpenses)
    }

    @Test
    fun testRemainingBudgetCalculation() {
        val totalSpent = 15_000_00L
        val budget = 30_000_00L
        val remaining = (budget - totalSpent).coerceAtLeast(0L)
        assertEquals(15_000_00L, remaining)
    }

    @Test
    fun testBudgetExceededRemainingClampedToZero() {
        val totalSpent = 35_000_00L
        val budget = 30_000_00L
        val remaining = (budget - totalSpent).coerceAtLeast(0L)
        assertEquals(0L, remaining)
    }

    @Test
    fun testUnknownSpendAndTopCategoriesAggregation() {
        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = 962_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = Instant.parse("2026-10-15T10:00:00Z").toEpochMilli(),
                categorySource = "Unknown",
                isReviewed = false,
            ),
            Transaction(
                id = 2L,
                amount = 27_878_00L,
                type = TransactionType.Expense,
                channel = Channel.CARD,
                dateTime = Instant.parse("2026-10-16T11:00:00Z").toEpochMilli(),
                categorySource = "Shopping",
                isReviewed = true,
            ),
            Transaction(
                id = 3L,
                amount = 10_290_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = Instant.parse("2026-10-17T12:00:00Z").toEpochMilli(),
                categorySource = "Food & Drinks",
                isReviewed = true,
            ),
            Transaction(
                id = 4L,
                amount = 5_000_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = Instant.parse("2026-10-18T14:00:00Z").toEpochMilli(),
                categorySource = "Shopping",
                isReviewed = true,
            ),
            Transaction(
                id = 5L,
                amount = 1_500_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = Instant.parse("2026-10-19T16:00:00Z").toEpochMilli(),
                categorySource = "Other",
                isReviewed = true,
            ),
        )

        val unknownSpend = transactions
            .filter {
                it.type == TransactionType.Expense &&
                    (it.categorySource.isNullOrBlank() ||
                        it.categorySource.equals("Unknown", ignoreCase = true) ||
                        it.categorySource.equals("Unknown Category", ignoreCase = true) ||
                        !it.isReviewed)
            }
            .sumOf { it.amount }
        assertEquals(962_00L, unknownSpend)

        val topSpendCategories = transactions
            .filter {
                it.type == TransactionType.Expense &&
                    !it.categorySource.isNullOrBlank() &&
                    !it.categorySource.equals("Unknown", ignoreCase = true) &&
                    !it.categorySource.equals("Unknown Category", ignoreCase = true) &&
                    it.isReviewed
            }
            .groupBy { it.categorySource ?: "Other" }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        assertEquals(3, topSpendCategories.size)
        assertEquals("Shopping", topSpendCategories[0].first)
        assertEquals(32_878_00L, topSpendCategories[0].second)
        assertEquals("Food & Drinks", topSpendCategories[1].first)
        assertEquals(10_290_00L, topSpendCategories[1].second)
        assertEquals("Other", topSpendCategories[2].first)
        assertEquals(1_500_00L, topSpendCategories[2].second)
    }

    @Test
    fun testTagCloudExtraction() {
        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = 100_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = 1L,
                note = "Order from Swiggy #Online #Food",
            ),
            Transaction(
                id = 2L,
                amount = 200_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = 2L,
                note = "Blinkit grocery #Online",
            ),
            Transaction(
                id = 3L,
                amount = 300_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = 3L,
                note = "Rent for apartment #Rent",
            ),
        )

        val counts = mutableMapOf<String, Int>()
        for (t in transactions) {
            val note = t.note ?: continue
            val matches = Regex("""#([A-Za-z0-9_]+)""").findAll(note)
            for (m in matches) {
                val tag = m.groupValues[1]
                counts[tag] = (counts[tag] ?: 0) + 1
            }
        }

        assertEquals(2, counts["Online"])
        assertEquals(1, counts["Food"])
        assertEquals(1, counts["Rent"])
    }

    @Test
    fun testAccountsAndCreditCardsSeparation() {
        val accounts =
            listOf(
                com.kharcha.core.model.Account(
                    id = 1L,
                    name = "HDFC ••1999",
                    bankName = "HDFC",
                    last4 = "1999",
                    type = com.kharcha.core.model.AccountType.Savings,
                ),
                com.kharcha.core.model.Account(
                    id = 2L,
                    name = "HDFC Card ••8302",
                    bankName = "HDFC",
                    last4 = "8302",
                    type = com.kharcha.core.model.AccountType.CreditCard,
                    linkedPrimaryAccountId = 1L,
                ),
                com.kharcha.core.model.Account(
                    id = 3L,
                    name = "Amazon Pay",
                    bankName = "Amazon",
                    last4 = null,
                    type = com.kharcha.core.model.AccountType.Wallet,
                ),
            )

        val primaryAccounts = accounts.filter { it.type != com.kharcha.core.model.AccountType.CreditCard }
        val creditCardAccounts = accounts.filter { it.type == com.kharcha.core.model.AccountType.CreditCard }

        assertEquals(2, primaryAccounts.size)
        assertEquals(listOf(1L, 3L), primaryAccounts.map { it.id })

        assertEquals(1, creditCardAccounts.size)
        assertEquals(listOf(2L), creditCardAccounts.map { it.id })
        assertEquals(1L, creditCardAccounts.first().linkedPrimaryAccountId)
    }

    @Test
    fun testLinkedRefundReducesMonthlySpendWithoutInflatingIncome() {
        val octEpoch = Instant.parse("2026-10-15T10:00:00Z").toEpochMilli()
        val expense = Transaction(
            id = 10L,
            amount = 1_000_00L,
            type = TransactionType.Expense,
            channel = Channel.CARD,
            dateTime = octEpoch,
            merchantRaw = "Swiggy",
            categorySource = "Food",
            isCounted = true,
        )
        val refund = Transaction(
            id = 11L,
            amount = 300_00L,
            type = TransactionType.Income,
            channel = Channel.UPI,
            dateTime = octEpoch,
            merchantRaw = "Swiggy",
            categorySource = "Food",
            isCounted = false,
            linkedTransactionId = 10L,
        )
        val salary = Transaction(
            id = 12L,
            amount = 50_000_00L,
            type = TransactionType.Income,
            channel = Channel.NEFT,
            dateTime = octEpoch,
            merchantRaw = "Employer",
            categorySource = "Salary",
            isCounted = true,
        )

        val allTxns = listOf(expense, refund, salary)
        val refundMap = allTxns.buildRefundMap()
        val countedCurrentMonth = allTxns.forTotals(refundMap)

        val spentThisMonth = countedCurrentMonth.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
        val incomeThisMonth = countedCurrentMonth.filter { it.type == TransactionType.Income }.sumOf { it.amount }

        // Expense should be reduced by 300_00 to 700_00
        assertEquals(700_00L, spentThisMonth)
        // Income should only be salary 50_000_00, NOT 50_300_00
        assertEquals(50_000_00L, incomeThisMonth)
    }

    @Test
    fun testUncountedTransactionsExcludedFromSpendAndIncome() {
        val octEpoch = Instant.parse("2026-10-15T10:00:00Z").toEpochMilli()
        val uncountedExpense = Transaction(
            id = 21L,
            amount = 500_00L,
            type = TransactionType.Expense,
            dateTime = octEpoch,
            categorySource = "Transfer",
            isCounted = false,
        )
        val uncountedIncome = Transaction(
            id = 22L,
            amount = 500_00L,
            type = TransactionType.Income,
            dateTime = octEpoch,
            categorySource = "Transfer",
            isCounted = false,
        )
        val countedExpense = Transaction(
            id = 23L,
            amount = 200_00L,
            type = TransactionType.Expense,
            dateTime = octEpoch,
            categorySource = "Food",
            isCounted = true,
        )

        val allTxns = listOf(uncountedExpense, uncountedIncome, countedExpense)
        val countedTxns = allTxns.forTotals()

        val spent = countedTxns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
        val income = countedTxns.filter { it.type == TransactionType.Income }.sumOf { it.amount }

        assertEquals(200_00L, spent)
        assertEquals(0L, income)
    }

    @Test
    fun testDuesListDerivedFromActiveRecurringTransactions() {
        val r1 =
            RecurringTransaction(
                id = 1L,
                merchantName = "Netflix",
                amountMinor = 199_00L,
                frequency = RecurringFrequency.MONTHLY,
                nextDueDateEpoch = 1_000L,
                isActive = true,
            )
        val r2 =
            RecurringTransaction(
                id = 2L,
                merchantName = "Gym",
                amountMinor = 1_500_00L,
                frequency = RecurringFrequency.MONTHLY,
                nextDueDateEpoch = 500L,
                isActive = true,
            )
        val r3 =
            RecurringTransaction(
                id = 3L,
                merchantName = "Old Service",
                amountMinor = 500_00L,
                frequency = RecurringFrequency.MONTHLY,
                nextDueDateEpoch = 200L,
                isActive = false,
            )

        val activeRecurring =
            listOf(r1, r2, r3)
                .filter { it.isActive }
                .sortedBy { it.nextDueDateEpoch }

        assertEquals(2, activeRecurring.size)
        assertEquals(2L, activeRecurring[0].id)
        assertEquals(1L, activeRecurring[1].id)
        assertEquals(1_699_00L, activeRecurring.sumOf { it.amountMinor })
    }

    @Test
    fun testAccountsAndCreditCardsPartitioning() {
        val acc1 =
            Account(
                id = 1L,
                name = "HDFC",
                bankName = "HDFC Bank",
                last4 = "1234",
                type = AccountType.Savings,
                currentBalance = 25_000_00L,
            )
        val acc2 =
            Account(
                id = 2L,
                name = "Amazon Pay",
                bankName = "Amazon Pay",
                last4 = null,
                type = AccountType.Wallet,
                currentBalance = 500_00L,
            )
        val card1 =
            Account(
                id = 3L,
                name = "ICICI Card",
                bankName = "ICICI Bank",
                last4 = "4321",
                type = AccountType.CreditCard,
                currentBalance = 12_000_00L,
            )

        val allAccounts = listOf(acc1, acc2, card1)
        val primaryAccounts = allAccounts.filter { it.type != AccountType.CreditCard }
        val creditCards = allAccounts.filter { it.type == AccountType.CreditCard }

        assertEquals(2, primaryAccounts.size)
        assertEquals(1, creditCards.size)
        assertEquals("HDFC Bank", primaryAccounts[0].bankName)
        assertEquals("ICICI Bank", creditCards[0].bankName)

        val emptyAccounts = emptyList<Account>()
        val emptyPrimary = emptyAccounts.filter { it.type != AccountType.CreditCard }
        val emptyCards = emptyAccounts.filter { it.type == AccountType.CreditCard }
        assertEquals(0, emptyPrimary.size)
        assertEquals(0, emptyCards.size)
    }
}
