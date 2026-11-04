package com.kharcha.core.common

import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class CsvTransactionImporterTest {

    private val importer = CsvTransactionImporter()

    @Test
    fun parsesPennyWiseCsvFormat() {
        val csv = """
            Date,Time,Merchant,Category,Type,Amount,Currency,Bank,Account,Balance After,Description
            2026-03-15,14:30:00,Starbucks,Food,Expense,350.50,INR,HDFC,1234,45000.00,Coffee with team
            2026-03-16,09:15:00,Tech Corp,Salary,Income,125000.00,INR,ICICI,5678,,Monthly salary
        """.trimIndent()

        val result = importer.parse(StringReader(csv))
        assertEquals(0, result.failedCount)
        assertEquals(2, result.transactions.size)

        val t1 = result.transactions[0]
        assertEquals("Starbucks", t1.merchantNormalised)
        assertEquals(35050L, t1.amount) // 350.50 INR in paise
        assertEquals(TransactionType.Expense, t1.type)
        assertEquals("Food", t1.categorySource)
        assertEquals("HDFC", t1.bankName)
        assertEquals("1234", t1.accountLast4)
        assertEquals(4500000L, t1.balanceAfter)
        assertEquals("Coffee with team", t1.note)
        assertTrue(t1.isCounted)

        val t2 = result.transactions[1]
        assertEquals("Tech Corp", t2.merchantNormalised)
        assertEquals(12500000L, t2.amount)
        assertEquals(TransactionType.Income, t2.type)
        assertEquals("Salary", t2.categorySource)
        assertTrue(t2.isCounted)
    }

    @Test
    fun parsesKharchaCsvFormatWithPaiseAndEpoch() {
        val csv = """
            id,date,epoch_ms,amount_paise,amount_inr,type,channel,category,merchant_normalised,bank_name,account_last4
            1,2026-03-15 14:30:00,1773565200000,99900,999.00,Expense,UPI,Bills & Utilities,Airtel,SBI,4321
        """.trimIndent()

        val result = importer.parse(StringReader(csv))
        assertEquals(0, result.failedCount)
        assertEquals(1, result.transactions.size)

        val t = result.transactions[0]
        assertEquals("Airtel", t.merchantNormalised)
        assertEquals(99900L, t.amount)
        assertEquals(1773565200000L, t.dateTime)
        assertEquals("Bills & Utilities", t.categorySource)
        assertEquals("SBI", t.bankName)
        assertEquals("4321", t.accountLast4)
    }

    @Test
    fun handlesTransferTypeAsNotCounted() {
        val csv = """
            Date,Amount,Type,Merchant
            2026-03-10,5000.00,Transfer,Self Transfer
        """.trimIndent()

        val result = importer.parse(StringReader(csv))
        assertEquals(1, result.transactions.size)
        val txn = result.transactions[0]
        assertEquals("Transfer", txn.categorySource)
        assertFalse(txn.isCounted)
    }

    @Test
    fun handlesQuotedFieldsWithCommas() {
        val csv = """
            Date,Amount,Type,Merchant,Description
            2026-03-10,120.00,Expense,"Swiggy, Bangalore","Delivered to ""Home"" address"
        """.trimIndent()

        val result = importer.parse(StringReader(csv))
        assertEquals(1, result.transactions.size)
        val txn = result.transactions[0]
        assertEquals("Swiggy, Bangalore", txn.merchantNormalised)
        assertEquals("Delivered to \"Home\" address", txn.note)
    }

    @Test
    fun missingRequiredColumnsReturnsFailureReason() {
        val csv = """
            Merchant,Category,Note
            Swiggy,Food,Dinner
        """.trimIndent()

        val result = importer.parse(StringReader(csv))
        assertEquals(0, result.transactions.size)
        assertTrue(result.failureReasons.isNotEmpty())
        assertTrue(result.failureReasons.first().contains("missing required column", ignoreCase = true))
    }
}
