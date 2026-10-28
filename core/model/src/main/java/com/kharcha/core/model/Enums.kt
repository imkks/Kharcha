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

val TransactionType.isCounted: Boolean
    get() = this == TransactionType.Expense || this == TransactionType.Income

val TransactionType.isCredit: Boolean
    get() = this == TransactionType.Income || this == TransactionType.Refund

val Transaction.isTransfer: Boolean
    get() = categorySource.equals("Transfer", ignoreCase = true) ||
        categorySource.equals("A/C Transfer", ignoreCase = true) ||
        merchantNormalised.equals("Transfer", ignoreCase = true) ||
        type == TransactionType.Transfer

val Transaction.isCounted: Boolean
    get() = type.isCounted && !isTransfer

