package com.kharcha.core.database.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kharcha.core.model.Channel
import com.kharcha.core.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: Long = 0L,
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
    val createdAt: Long,
    val updatedAt: Long,
)
