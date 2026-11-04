package com.kharcha.core.common.statement.pdf

import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GPayPdfParserTest {

    private val parser = GPayPdfParser()

    private val ist = TimeZone.getTimeZone("Asia/Kolkata")
    private val dateFormat = SimpleDateFormat("dd MMM, yyyy hh:mm a", Locale.ENGLISH).apply {
        timeZone = ist
    }

    private fun epochForIstDate(text: String): Long = dateFormat.parse(text)!!.time

    private val statementText = """
        Google Pay Statement
        01 Sep,
        2025
        03:02 PM
        Paid to Starbucks
        ₹450
        UPI Transaction ID: 123456789012
        Paid by HDFC Bank 1234
        02 Sep,
        2025
        11:30 AM
        Received from Alice
        ₹2,000
        UPI Transaction ID: 223456789012
        Paid to HDFC Bank 1234
        03 Sep,
        2025
        07:45 PM
        Paid to Amazon
        ₹1,250.50
        UPI Transaction ID: 323456789012
        Paid by HDFC Bank 1234
    """.trimIndent()

    @Test
    fun parserHandlesTheFixture() {
        assertTrue("canHandle should recognise the GPay statement", parser.canHandle(statementText))
    }

    @Test
    fun parsesAllThreeTransactions() {
        val txs = parser.parse(statementText)
        assertEquals(3, txs.size)

        assertEquals(450_00L, txs[0].amount)
        assertEquals(TransactionType.Expense, txs[0].type)
        assertEquals("Starbucks", txs[0].merchantNormalised)
        assertEquals("123456789012", txs[0].refNumber)
        assertEquals("1234", txs[0].accountLast4)

        assertEquals(2000_00L, txs[1].amount)
        assertEquals(TransactionType.Income, txs[1].type)
        assertEquals("Alice", txs[1].merchantNormalised)

        assertEquals(1250_50L, txs[2].amount)
        assertEquals(TransactionType.Expense, txs[2].type)
        assertEquals("Amazon", txs[2].merchantNormalised)

        val expected1 = epochForIstDate("01 Sep, 2025 03:02 PM")
        assertEquals(expected1, txs[0].dateTime)
    }
}
