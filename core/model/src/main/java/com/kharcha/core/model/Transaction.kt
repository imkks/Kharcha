package com.kharcha.core.model

data class Transaction(
    val id: String,
    val merchant: String,
    val amount: Long,
    val date: Long,
    val categoryId: String?,
    val originalSms: String?,
    val confidence: Float,
    val isIgnored: Boolean = false,
    val isReviewed: Boolean = false,
    val accountId: String,
)

data class Category(
    val id: String,
    val name: String,
    val iconId: String? = null,
)
