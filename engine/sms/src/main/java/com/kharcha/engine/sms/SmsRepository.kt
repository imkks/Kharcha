package com.kharcha.engine.sms

import com.kharcha.core.model.IgnoredMessage
import com.kharcha.core.model.RawSms

interface SmsRepository {
    suspend fun saveCheckpoint(lastProcessedId: Long)

    suspend fun getCheckpoint(): Long

    suspend fun isDuplicate(smsHash: String): Boolean

    suspend fun saveIgnoredMessage(ignoredMessage: IgnoredMessage)

    suspend fun saveRawSms(rawSms: RawSms)
}
