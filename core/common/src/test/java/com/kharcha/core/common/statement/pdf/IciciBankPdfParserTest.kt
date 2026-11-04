package com.kharcha.core.common.statement.pdf

import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IciciBankPdfParserTest {

    private val parser = IciciBankPdfParser()

    private val statementText = """
        ICICI Bank Limited
        Statement of Transactions
        DATE
        PARTICULARS
        DEPOSITS
        WITHDRAWALS
        BALANCE
        B/F 1,00,000.00
        UPI/SWIGGY/12345
        17-10-2025
        450.00
        99,550.00
        Int on FD/RD XXX1406
        18-10-2025
        600.00
        1,00,150.00
    """.trimIndent()

    @Test
    fun recognisesIciciStatements() {
        assertTrue(parser.canHandle(statementText))
        assertFalse(parser.canHandle("Google Pay transaction statement"))
    }

    @Test
    fun parsesIciciTransactions() {
        val rows = parser.parse(statementText)
        assertEquals(2, rows.size)

        assertEquals("SWIGGY", rows[0].merchantNormalised)
        assertEquals(450_00L, rows[0].amount)
        assertEquals(TransactionType.Expense, rows[0].type)
        assertEquals(99550_00L, rows[0].balanceAfter)

        assertEquals("Interest", rows[1].merchantNormalised)
        assertEquals(600_00L, rows[1].amount)
        assertEquals(TransactionType.Income, rows[1].type)
        assertEquals(100150_00L, rows[1].balanceAfter)
    }
}
