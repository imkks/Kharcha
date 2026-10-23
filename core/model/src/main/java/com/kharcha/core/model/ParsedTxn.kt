package com.kharcha.core.model

data class ParsedTxn(
    val amountMinor: Long,
    val currency: String = "INR",
    val direction: Direction,
    val dateTime: Long?,
    val accountLast4: String?,
    val bankName: String?,
    val merchantRaw: String?,
    val channel: Channel?,
    val balanceAfterMinor: Long?,
    val refNumber: String?,
    val upiId: String?,
    val confidence: Float,
    val templateId: String?,
)
