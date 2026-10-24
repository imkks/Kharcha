package com.kharcha.core.model

enum class Direction {
    DEBIT,
    CREDIT,
}

enum class Channel {
    UPI,
    CARD,
    NEFT,
    IMPS,
    ATM,
    WALLET,
    OTHER,
}

enum class TransactionType {
    Expense,
    Income,
    Transfer,
    Refund,
    Investment,
    Ignored,
}

enum class AccountType {
    Savings,
    Current,
    CreditCard,
    Wallet,
    Cash,
}

enum class TimePeriod {
    THIS_MONTH,
    LAST_MONTH,
    ALL_TIME,
}
