package com.kharcha.core.model

/**
 * Standard default categories for Kharcha.
 * Unified across Settings, Transaction Detail, Review Queue, and categorization engines.
 */
val DEFAULT_SPEND_CATEGORIES: List<String> =
    listOf(
        "Food & Dining",
        "Groceries",
        "Shopping",
        "Transport",
        "Fuel",
        "Bills & Utilities",
        "Entertainment",
        "Health",
        "Investments",
        "Education",
        "Travel",
        "Personal Care",
        "Donation",
        "EMI",
        "Shared",
        "Transfer",
        "Other",
        "Unknown",
    )

val DEFAULT_CREDIT_CATEGORIES: List<String> =
    listOf(
        "Salary",
        "Business",
        "Interest",
        "Investment Return",
        "Bank Deposit",
        "Allowance",
        "Cashback",
        "Rewards",
        "Refund",
        "Reimbursement",
        "Credit",
        "Loan",
        "Transfer",
        "Other",
        "Unknown",
    )

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
