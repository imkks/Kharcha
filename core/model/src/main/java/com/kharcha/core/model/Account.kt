package com.kharcha.core.model

data class Account(
    val id: Long = 0L,
    val name: String,
    val bankName: String?,
    val last4: String?,
    val type: AccountType,
    val currentBalance: Long? = null,
    val balanceUpdatedAt: Long? = null,
    val isArchived: Boolean = false,
)
