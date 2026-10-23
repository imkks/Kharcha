package com.kharcha.core.model

data class RawSms(
    val id: Long,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val subId: Int?,
)
