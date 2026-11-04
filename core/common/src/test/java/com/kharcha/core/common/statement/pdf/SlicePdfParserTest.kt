package com.kharcha.core.common.statement.pdf

import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlicePdfParserTest {

    private val parser = SlicePdfParser()

    private val statement = """
        01 Jan '26 - 31 Jan '26
        1/1
        Mr TEST PERSON
        Customer ID 100000000001
        Account SAVINGS
        A/C number 999900005678
        IFSC NESF0000999
        Opening balance Total credits Interest earned Total debits Closing balance
        + + - =
        ₹10,000 ₹6,000 ₹1.23 ₹5,679.1 ₹10,322.13
        DATE DETAILS REF NO. AMOUNT BALANCE
        02 Jan '26 Interest Cr. for 01-Jan-2026 111122223333 ₹1.23 ₹10,001.23
        02 Jan '26 Transfer from Holiday fund atom 444455556 ₹6,000 ₹16,001.23
        02 Jan '26 UPI-Debit-999900001111-SOME MERCHA
        NT NAME-ABCD0EFGH11-somevpa@bank-Paid V
        ia Elements
        2026010212345678 -₹5,000 ₹11,001.23
        03 Jan '26 Auto save to Daily saver atom 444455557 -₹100 ₹10,901.23
        03 Jan '26 Round ups 444455558 -₹18.20 ₹10,883.03
        04 Jan '26 UPI-Debit-999900002222-OTHER SHOP-WXYZ0AB1234-shop@upi-123456 2026010412345679 -₹560.90 ₹10,322.13
        Generated on 31 Jan '26
        Need help? Contact our support team at help@slice.bank.in or +91-0000000000 slice small finance bank
    """.trimIndent()

    @Test
    fun handlesSliceStatementsOnly() {
        assertTrue(parser.canHandle(statement))
        assertFalse(parser.canHandle("Paid to Somewhere UPI transaction ID 123 google pay"))
    }

    @Test
    fun parsesAllRowsWithAmountsDatesAndRefs() {
        val txns = parser.parse(statement)
        assertEquals(6, txns.size)

        val interest = txns[0]
        assertEquals(1_23L, interest.amount)
        assertEquals(TransactionType.Income, interest.type)
        assertEquals("111122223333", interest.refNumber)
        assertEquals("5678", interest.accountLast4)
        assertEquals("Slice", interest.bankName)
        assertEquals(10001_23L, interest.balanceAfter)

        assertEquals(1767335400000L, interest.dateTime)
    }

    @Test
    fun potMovementsAreTransfersNotCountedInTotals() {
        val txns = parser.parse(statement)
        val potIn = txns[1]
        val autoSave = txns[3]
        val roundUps = txns[4]

        assertEquals(false, potIn.isCounted)
        assertEquals("Transfer", potIn.categorySource)
        assertEquals("Holiday fund", potIn.merchantNormalised)

        assertEquals(false, autoSave.isCounted)
        assertEquals("Transfer", autoSave.categorySource)
        assertEquals("Daily saver", autoSave.merchantNormalised)

        assertEquals(false, roundUps.isCounted)
        assertEquals("Transfer", roundUps.categorySource)
        assertEquals(18_20L, roundUps.amount)
    }

    @Test
    fun multilineUpiDebitReassemblesMerchantBetweenRefAndIfsc() {
        val txns = parser.parse(statement)
        val upi = txns[2]
        assertEquals(TransactionType.Expense, upi.type)
        assertEquals(5000_00L, upi.amount)
        assertEquals("2026010212345678", upi.refNumber)
        assertEquals("SOME MERCHA NT NAME", upi.merchantNormalised)
    }
}
