package com.kharcha.core.model

data class Transaction(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val timestamp: Long,
    val type: TransactionType,
    val categoryId: String?,
    val merchantName: String?,
    val accountId: String?,
    val channel: String?,
    val note: String? = null,
    val confidence: Float = 1.0f,
    val needsReview: Boolean = false,
)
