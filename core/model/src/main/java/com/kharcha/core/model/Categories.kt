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
