package com.kharcha.core.model

data class IgnoredMessage(
    val id: Long = 0,
    val smsHash: String,
    val reason: IgnoreReason,
    val receivedAt: Long,
)
