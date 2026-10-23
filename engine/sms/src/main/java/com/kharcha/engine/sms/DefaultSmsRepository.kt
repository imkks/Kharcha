package com.kharcha.engine.sms

import com.kharcha.core.model.IgnoredMessage
import com.kharcha.core.model.RawSms
import javax.inject.Inject

// Placeholder implementation to satisfy DI until core:database is connected
class DefaultSmsRepository
    @Inject
    constructor() : SmsRepository {
        override suspend fun saveCheckpoint(lastProcessedId: Long) {}

        override suspend fun getCheckpoint(): Long = 0L

        override suspend fun isDuplicate(smsHash: String): Boolean = false

        override suspend fun saveIgnoredMessage(ignoredMessage: IgnoredMessage) {}

        override suspend fun saveRawSms(rawSms: RawSms) {}
    }
