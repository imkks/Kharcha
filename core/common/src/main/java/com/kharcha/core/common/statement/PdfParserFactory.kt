package com.kharcha.core.common.statement

import com.kharcha.core.common.statement.pdf.GPayPdfParser
import com.kharcha.core.common.statement.pdf.IciciBankPdfParser
import com.kharcha.core.common.statement.pdf.PaytmPdfParser
import com.kharcha.core.common.statement.pdf.PhonePePdfParser
import com.kharcha.core.common.statement.pdf.SlicePdfParser

object PdfParserFactory {

    private val parsers: List<PdfStatementParser> = listOf(
        GPayPdfParser(),
        PhonePePdfParser(),
        PaytmPdfParser(),
        SlicePdfParser(),
        IciciBankPdfParser(),
    )

    fun getParser(extractedText: String): PdfStatementParser? {
        return parsers.firstOrNull { it.canHandle(extractedText) }
    }
}
