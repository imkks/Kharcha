package com.kharcha.core.common.statement

import com.kharcha.core.model.Transaction

interface PdfStatementParser {
    fun canHandle(text: String): Boolean
    fun parse(text: String): List<Transaction>
}
