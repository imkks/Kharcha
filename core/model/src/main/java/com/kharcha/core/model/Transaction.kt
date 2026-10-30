package com.kharcha.core.model

data class Transaction(
    val id: Long = 0L,
    val smsHash: String? = null,
    val accountId: Long = 0L,
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
    val linkedTransactionId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val templateId: String? = null,
    val accountLast4: String? = null,
    val bankName: String? = null,
    val sender: String? = null,
    val isCounted: Boolean = true,
) {
    val isTransfer: Boolean
        get() = categorySource.equals("Transfer", ignoreCase = true) ||
            categorySource.equals("A/C Transfer", ignoreCase = true) ||
            (categorySource.equals("Bills", ignoreCase = true) && !isCounted) ||
            categorySource.equals("Bill Payment", ignoreCase = true) ||
            categorySource.equals("Credit Card Bill", ignoreCase = true) ||
            merchantNormalised.equals("Transfer", ignoreCase = true)
}
