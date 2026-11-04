package com.kharcha.core.common.statement.pdf

import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaytmPdfParserTest {

    private val parser = PaytmPdfParser()

    private val statementText = """
        Paytm Statement for
        18 JUL'25 - 17 JUL'26
        Passbook Payments History
        Date &
        Time
        Transaction Details Notes & Tags Your Account Amount
        17 Jul
        8:24 PM
        Money sent to Alice Smith
         UPI ID: alicesmith@okbank  on
        UPI Ref No: 310000000001
         Tag:
        # Money Transfer
        State Bank
        Of India - 17
        - Rs.220
        25 Jun
        7:01 AM
        Received from Chetan Verma
         UPI ID: chetanverma@okaxis  on
        UPI Ref No: 617000000007
         Tag:
        # Money Received
        State Bank
        Of India - 17
        + Rs.75
    """.trimIndent()

    @Test
    fun parserDetectsPaytmStatements() {
        assertTrue(parser.canHandle(statementText))
        assertFalse(parser.canHandle("Google Pay Statement\nUPI Transaction ID: 123"))
    }

    @Test
    fun parsesPaytmTransactions() {
        val txs = parser.parse(statementText)
        assertEquals(2, txs.size)

        assertEquals("Alice Smith", txs[0].merchantNormalised)
        assertEquals(220_00L, txs[0].amount)
        assertEquals(TransactionType.Expense, txs[0].type)
        assertEquals("310000000001", txs[0].refNumber)
        assertEquals("17", txs[0].accountLast4)

        assertEquals("Chetan Verma", txs[1].merchantNormalised)
        assertEquals(75_00L, txs[1].amount)
        assertEquals(TransactionType.Income, txs[1].type)
        assertEquals("617000000007", txs[1].refNumber)
    }
}
