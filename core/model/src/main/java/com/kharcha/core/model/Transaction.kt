package com.kharcha.core.model

data class Transaction(
    val id: Long = 0L,
    val smsHash: String? = null,
    val accountId: Long,
    val type: TransactionType,
    val amount: Long,
    val currency: String = "INR",
    val dateTime: Long,
    val merchantRaw: String? = null,
    val merchantNormalised: String? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val categorySource: String? = null,
    val categoryConfidence: Float? = null,
    val parseConfidence: Float? = null,
    val channel: Channel? = null,
    val refNumber: String? = null,
    val upiId: String? = null,
    val balanceAfter: Long? = null,
    val note: String? = null,
    val isReviewed: Boolean = false,
    val isIgnored: Boolean = false,
    val linkedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class Category(
    val id: String,
    val name: String,
    val iconId: String? = null,
)
