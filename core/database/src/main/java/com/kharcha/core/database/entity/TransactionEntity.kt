package com.kharcha.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kharcha.core.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val timestamp: Long,
    val type: TransactionType,
    val categoryId: String?,
    val merchantName: String?,
    val accountId: String?,
    val channel: String?,
    val note: String?,
    val confidence: Float,
    val needsReview: Boolean,
)
