package com.kharcha.core.model

interface SmsParser {
    fun parse(sms: RawSms): ParseResult
}
