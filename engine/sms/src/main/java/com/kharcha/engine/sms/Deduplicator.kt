package com.kharcha.engine.sms

import java.security.MessageDigest
import javax.inject.Inject

class Deduplicator
    @Inject
    constructor(private val smsRepository: SmsRepository) {
        suspend fun isDuplicate(
            sender: String,
            body: String,
            timestamp: Long,
        ): Boolean {
            val hash = generateHash(sender, body, timestamp)
            return smsRepository.isDuplicate(hash)
        }

        fun generateHash(
            sender: String,
            body: String,
            timestamp: Long,
        ): String {
            val input = "$sender|$body|$timestamp"
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
