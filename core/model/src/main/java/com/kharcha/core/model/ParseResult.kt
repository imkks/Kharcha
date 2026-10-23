package com.kharcha.core.model

sealed interface ParseResult {
    data class Success(val txn: ParsedTxn) : ParseResult

    data class Ignored(val reason: IgnoreReason) : ParseResult

    object NoMatch : ParseResult
}
