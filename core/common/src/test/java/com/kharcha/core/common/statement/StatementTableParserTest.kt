package com.kharcha.core.common.statement

import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class StatementTableParserTest {

    private val account = StatementAccount(bankName = "Sample Bank", last4 = "1234", currency = "INR")

    private val debitCreditCsv = """
        Sample Bank Ltd,,,,,
        Statement of account,,,,,
        Period: 01/01/2025 - 31/01/2025,,,,,
        Txn Date,Narration,Ref No,Withdrawal Amt,Deposit Amt,Closing Balance
        ,Opening Balance,,,,"1,00,000.00"
        02/01/2025,UPI/512345678901/GROCERY MART/okaxis,512345678901,"1,250.50",,"98,749.50"
        03/01/2025,SALARY JAN,,,"1,23,456.78","2,22,206.28"

        05/01/2025,ATM WDL,,"2,000.00",,"2,20,206.28"
        ,,,,,
        Total,,,"3,250.50","1,23,456.78",
    """.trimIndent()

    @Test
    fun separateDebitAndCreditColumnsWithHeaderNotFirstRow() {
        val rows = StatementTableParser.readCsv(debitCreditCsv)
        assertEquals(3, StatementTableParser.guessHeaderRow(rows))
        val mapping = StatementTableParser.guessMapping(rows)
        assertEquals(0, mapping.dateColumn)
        assertEquals(1, mapping.descriptionColumn)
        assertEquals(3, mapping.debitColumn)
        assertEquals(4, mapping.creditColumn)
        assertEquals(5, mapping.balanceColumn)

        val result = StatementTableParser.parse(rows, mapping, account)
        val txns = result.transactions
        assertEquals(3, txns.size)
        assertEquals(2, result.skippedRows) // opening balance + totals; blank lines aren't counted

        assertEquals(1250_50L, txns[0].amount)
        assertEquals(TransactionType.Expense, txns[0].type)
        assertEquals("512345678901", txns[0].refNumber)
        assertEquals(98749_50L, txns[0].balanceAfter)
        assertEquals(123456_78L, txns[1].amount)
        assertEquals(TransactionType.Income, txns[1].type)
        assertEquals(2000_00L, txns[2].amount)
        assertEquals(TransactionType.Expense, txns[2].type)

        txns.forEach {
            assertEquals("INR", it.currency)
            assertEquals("Sample Bank", it.bankName)
            assertEquals("1234", it.accountLast4)
        }
    }

    @Test
    fun singleSignedAmountColumn() {
        val rows = StatementTableParser.readCsv(
            """
            Date,Description,Amount,Balance
            2025-02-01,Coffee,-120.00,880.00
            2025-02-02,Refund,+45.5,925.50
            2025-02-03,Rent,(500.00),425.50
            """.trimIndent(),
        )
        val mapping = StatementTableParser.guessMapping(rows)
        assertEquals(2, mapping.amountColumn)
        val txns = StatementTableParser.parse(rows, mapping, account).transactions
        assertEquals(listOf(TransactionType.Expense, TransactionType.Income, TransactionType.Expense), txns.map { it.type })
        assertEquals(listOf(120_00L, 45_50L, 500_00L), txns.map { it.amount })
    }

    @Test
    fun amountWithDrCrTypeColumn() {
        val rows = StatementTableParser.readCsv(
            """
            Value Date;Particulars;Amount;Dr/Cr
            05-JAN-25;POS SHOP;"1.234,56";DR
            06-Jan-25;INTEREST;10;CR
            """.trimIndent(),
        )
        val mapping = StatementTableParser.guessMapping(rows)
        assertEquals(3, mapping.typeColumn)
        val txns = StatementTableParser.parse(rows, mapping, account).transactions
        assertEquals(TransactionType.Expense, txns[0].type)
        assertEquals(1234_56L, txns[0].amount)
        assertEquals(TransactionType.Income, txns[1].type)
        assertEquals(10_00L, txns[1].amount)
    }

    @Test
    fun parseAmountVariants() {
        assertEquals(500_00L, StatementTableParser.parseAmount("500")?.valueMinor)
        assertEquals(500_00L, StatementTableParser.parseAmount("₹ 500.00")?.valueMinor)
        assertEquals(1250_50L, StatementTableParser.parseAmount("1,250.50 Dr")?.valueMinor)
        assertEquals(true, StatementTableParser.parseAmount("1,250.50 Dr")?.negative)
        assertEquals(false, StatementTableParser.parseAmount("1,250.50 Cr")?.negative)
        assertEquals(true, StatementTableParser.parseAmount("(250.00)")?.negative)
        assertEquals(true, StatementTableParser.parseAmount("-99.00")?.negative)
    }

    @Test
    fun detectSeparator() {
        assertEquals(',', StatementTableParser.detectSeparator("a,b,c\n1,2,3"))
        assertEquals(';', StatementTableParser.detectSeparator("a;b;c\n1;2;3"))
        assertEquals('\t', StatementTableParser.detectSeparator("a\tb\tc\n1\t2\t3"))
    }

    @Test
    fun namesAccountDetection() {
        val preamble = "State Bank of India\nA/C No: XXXXXX4321\nBranch: Bangalore"
        assertTrue(StatementTableParser.namesAccount(preamble, "4321"))
        assertFalse(StatementTableParser.namesAccount(preamble, "9999"))
    }

    @Test
    fun merchantAndReferenceExtraction() {
        val narration1 = "UPI/512345678901/SWIGGY BANGALORE/swiggy@axis"
        assertEquals("SWIGGY BANGALORE", StatementTableParser.merchantFrom(narration1))
        assertEquals("512345678901", StatementTableParser.referenceFrom("", narration1))

        val narration2 = "IMPS-698765432101-JOHN DOE-TRANSFER"
        assertEquals("698765432101", StatementTableParser.referenceFrom("", narration2))
    }
}
