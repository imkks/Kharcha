package com.kharcha.core.database.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val merchant: String,
    val amount: Long,
    val date: Long,
    val categoryId: String?,
    val originalSms: String?,
    val confidence: Float,
    val isIgnored: Boolean,
    val isReviewed: Boolean,
    val accountId: String,
)
