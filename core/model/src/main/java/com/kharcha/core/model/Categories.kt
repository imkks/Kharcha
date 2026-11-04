package com.kharcha.core.model

/**
 * Strongly-typed category representation separating Expense (debit) and Income (credit) categories
 * at compile-time and runtime.
 */
sealed interface TransactionCategory {
    val name: String

    @JvmInline
    value class Expense(override val name: String) : TransactionCategory {
        companion object {
            val FoodAndDining = Expense("Food & Dining")
            val Groceries = Expense("Groceries")
            val Shopping = Expense("Shopping")
            val Transport = Expense("Transport")
            val Fuel = Expense("Fuel")
            val BillsAndUtilities = Expense("Bills & Utilities")
            val Entertainment = Expense("Entertainment")
            val Health = Expense("Health")
            val Investments = Expense("Investments")
            val Education = Expense("Education")
            val Travel = Expense("Travel")
            val PersonalCare = Expense("Personal Care")
            val Donation = Expense("Donation")
            val EMI = Expense("EMI")
            val Shared = Expense("Shared")
            val Transfer = Expense("Transfer")
            val Other = Expense("Other")
            val Unknown = Expense("Unknown")

            val ALL: List<Expense> = listOf(
                FoodAndDining,
                Groceries,
                Shopping,
                Transport,
                Fuel,
                BillsAndUtilities,
                Entertainment,
                Health,
                Investments,
                Education,
                Travel,
                PersonalCare,
                Donation,
                EMI,
                Shared,
                Transfer,
                Other,
                Unknown,
            )

            val COMMON_SUGGESTIONS: List<Expense> = listOf(
                FoodAndDining,
                Groceries,
                Shopping,
                Transport,
                BillsAndUtilities,
            )

            fun from(raw: String?, customCategories: Set<String> = emptySet()): Expense =
                Expense(sanitizeCategory(raw, isCredit = false, customCategories))
        }
    }

    @JvmInline
    value class Income(override val name: String) : TransactionCategory {
        companion object {
            val Salary = Income("Salary")
            val Business = Income("Business")
            val Interest = Income("Interest")
            val InvestmentReturn = Income("Investment Return")
            val BankDeposit = Income("Bank Deposit")
            val Allowance = Income("Allowance")
            val Cashback = Income("Cashback")
            val Rewards = Income("Rewards")
            val Refund = Income("Refund")
            val Reimbursement = Income("Reimbursement")
            val Credit = Income("Credit")
            val Loan = Income("Loan")
            val Transfer = Income("Transfer")
            val Other = Income("Other")
            val Unknown = Income("Unknown")

            val ALL: List<Income> = listOf(
                Salary,
                Business,
                Interest,
                InvestmentReturn,
                BankDeposit,
                Allowance,
                Cashback,
                Rewards,
                Refund,
                Reimbursement,
                Credit,
                Loan,
                Transfer,
                Other,
                Unknown,
            )

            val COMMON_SUGGESTIONS: List<Income> = listOf(
                Salary,
                InvestmentReturn,
                Cashback,
                Refund,
                Transfer,
            )

            fun from(raw: String?, customCategories: Set<String> = emptySet()): Income =
                Income(sanitizeCategory(raw, isCredit = true, customCategories))
        }
    }

    companion object {
        fun categoriesFor(type: TransactionType, customCategories: Set<String> = emptySet()): List<TransactionCategory> =
            when (type) {
                TransactionType.Expense -> (Expense.ALL.map { it.name } + customCategories)
                    .distinct()
                    .map { Expense(it) }
                TransactionType.Income -> (Income.ALL.map { it.name } + customCategories)
                    .distinct()
                    .map { Income(it) }
            }

        fun commonSuggestionsFor(type: TransactionType): List<TransactionCategory> =
            when (type) {
                TransactionType.Expense -> Expense.COMMON_SUGGESTIONS
                TransactionType.Income -> Income.COMMON_SUGGESTIONS
            }

        fun from(raw: String?, type: TransactionType, customCategories: Set<String> = emptySet()): TransactionCategory =
            when (type) {
                TransactionType.Expense -> Expense.from(raw, customCategories)
                TransactionType.Income -> Income.from(raw, customCategories)
            }
    }
}

fun TransactionType.allowedCategories(customCategories: Set<String> = emptySet()): List<TransactionCategory> =
    TransactionCategory.categoriesFor(this, customCategories)

fun TransactionType.allowedCategoryNames(customCategories: Set<String> = emptySet()): List<String> =
    allowedCategories(customCategories).map { it.name }

fun TransactionType.commonSuggestions(): List<TransactionCategory> =
    TransactionCategory.commonSuggestionsFor(this)

/**
 * Standard default categories for Kharcha.
 * Unified across Settings, Transaction Detail, Review Queue, and categorization engines.
 */
val DEFAULT_SPEND_CATEGORIES: List<String> =
    TransactionCategory.Expense.ALL.map { it.name }

val DEFAULT_CREDIT_CATEGORIES: List<String> =
    TransactionCategory.Income.ALL.map { it.name }

val DEFAULT_CREDIT_NON_INCOME_CATEGORIES: Set<String> =
    setOf(
        "Refund",
        "Transfer",
        "Reimbursement",
        "Credit",
        "Loan",
    )

val DEFAULT_CATEGORIES: List<String> =
    (DEFAULT_SPEND_CATEGORIES + DEFAULT_CREDIT_CATEGORIES).distinct()

/**
 * Sanitizes any raw category string to ensure it strictly belongs to the defined
 * set of categories for Kharcha.
 *
 * Debit transactions MUST belong to DEFAULT_SPEND_CATEGORIES + user custom categories.
 * Credit transactions MUST belong to DEFAULT_CREDIT_CATEGORIES + user custom categories.
 * Any unknown, legacy, or mismatched category is mapped to its correct equivalent or
 * falls back to "Other".
 */
fun sanitizeCategory(
    rawCategory: String?,
    isCredit: Boolean,
    customCategories: Set<String> = emptySet(),
): String {
    if (rawCategory.isNullOrBlank()) {
        return "Unknown"
    }

    val trimmed = rawCategory.trim()
    val allowedList = if (isCredit) DEFAULT_CREDIT_CATEGORIES else DEFAULT_SPEND_CATEGORIES
    val allowedSet = (allowedList + customCategories).toSet()

    // 1. Exact match against allowed
    if (allowedSet.contains(trimmed)) {
        return trimmed
    }

    // 2. Case-insensitive match against allowed
    val caseInsensitive = allowedSet.firstOrNull { it.equals(trimmed, ignoreCase = true) }
    if (caseInsensitive != null) {
        return caseInsensitive
    }

    // 3. Known aliases & cross-category mappings
    val lower = trimmed.lowercase()
    if (lower in setOf("unknown", "unknown category", "uncategorised", "uncategorized", "not categorised")) {
        return "Unknown"
    }
    if (lower in setOf("other", "others", "misc", "miscellaneous")) {
        return "Other"
    }

    return if (isCredit) {
        when {
            lower in setOf("investment", "investments", "dividend", "dividends", "payout", "settlement payout", "stock", "stocks") ->
                "Investment Return"
            lower in setOf("a/c transfer", "ac transfer", "internal transfer", "self transfer", "p2p") ->
                "Transfer"
            lower in setOf("cash back", "cash-back") ->
                "Cashback"
            lower in setOf("reward", "points", "cliq cash") ->
                "Rewards"
            lower in setOf("reversal", "refunded") ->
                "Refund"
            lower in setOf("credit card payment", "bill payment", "card payment") ->
                "Credit"
            lower in setOf("salary", "payroll", "stipend") ->
                "Salary"
            lower in setOf("interest") ->
                "Interest"
            lower in setOf("deposit", "cash deposit") ->
                "Bank Deposit"
            lower in setOf("business", "settlement") ->
                "Business"
            lower in setOf("reimbursement") ->
                "Reimbursement"
            lower in setOf("loan", "disbursement") ->
                "Loan"
            lower in setOf("allowance") ->
                "Allowance"
            else -> "Unknown"
        }
    } else {
        when {
            lower in setOf(
                "bills", "bill", "bill payment", "utilities", "mobile",
                "credit card payment", "banking", "tax", "bank charge", "insurance"
            ) -> "Bills & Utilities"
            lower in setOf("food", "dining", "food & drinks", "restaurant", "cafe") ->
                "Food & Dining"
            lower in setOf("grocery", "groceries & essentials") ->
                "Groceries"
            lower in setOf("transportation", "cab", "auto", "commute") ->
                "Transport"
            lower in setOf("flight", "train", "hotel", "trips") ->
                "Travel"
            lower in setOf("investment", "mutual funds", "stocks") ->
                "Investments"
            lower in setOf("loan", "loans", "loans and emi") ->
                "EMI"
            lower in setOf("healthcare", "fitness", "medical", "pharmacy") ->
                "Health"
            lower in setOf("grooming", "salon", "beauty") ->
                "Personal Care"
            lower in setOf("charity", "seva") ->
                "Donation"
            else -> "Unknown"
        }
    }
}
