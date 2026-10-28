package com.kharcha.feature.home.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
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
        )

        val unknownSpend = transactions
            .filter { it.type == TransactionType.Expense && (it.categorySource == "Unknown" || it.categorySource == "Other" || !it.isReviewed) }
            .sumOf { it.amount }
        assertEquals(962_00L, unknownSpend)

        val topSpendCategories = transactions
            .filter { it.type == TransactionType.Expense && it.categorySource != "Unknown" && it.categorySource != "Other" }
            .groupBy { it.categorySource ?: "Others" }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        assertEquals(2, topSpendCategories.size)
        assertEquals("Shopping", topSpendCategories[0].first)
        assertEquals(32_878_00L, topSpendCategories[0].second)
        assertEquals("Food & Drinks", topSpendCategories[1].first)
        assertEquals(10_290_00L, topSpendCategories[1].second)
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
}
