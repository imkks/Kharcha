package com.pennywiseai.parser.core.test

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.BankParserFactory
import com.pennywiseai.parser.core.bank.ComBankParser
import com.pennywiseai.parser.core.bank.OrangeMoneyJordanParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class AdditionalBankWalletParserTest {
    @Test
    fun combankParsesLkrCardPurchaseAndFactorySender() {
        val sms = "Dear Cardholder, Purchase at Dialog Axiata Colombo LK for LKR 672.00 on " +
            "27/09/26 09:32 PM has been authorised on your debit card ending #5555."

        val parsed = BankParserFactory.parse(sms, "AD-ComBank-S", 1L)

        assertNotNull(parsed)
        assertEquals("Commercial Bank of Ceylon", parsed?.bankName)
        assertEquals(BigDecimal("672.00"), parsed?.amount)
        assertEquals("LKR", parsed?.currency)
        assertEquals("5555", parsed?.accountLast4)
        assertEquals(TransactionType.EXPENSE, parsed?.type)
        assertTrue(parsed?.isFromCard == true)
        assertTrue(ComBankParser().canHandle("COMBANK"))
        assertNull(BankParserFactory.getParser("COMMERCIALBANK").takeIf { it is ComBankParser })
    }

    @Test
    fun orangeMoneyJordanParsesArabicWalletPurchaseAndFactorySender() {
        val sms = "تمت عملية شراء من محفظتك 07XXXXXXXX بمبلغ 10 دينار " +
            "من OJO Payment Channel بالرقم المرجعي OM20260916223921000000000000 " +
            "رصيد المحفظة المتاح هو 25.500 دينار أردني"

        val parsed = BankParserFactory.parse(sms, "OrangeMoney", 1L)

        assertNotNull(parsed)
        assertEquals("Orange Money", parsed?.bankName)
        assertEquals(BigDecimal("10"), parsed?.amount)
        assertEquals("JOD", parsed?.currency)
        assertEquals("OM20260916223921000000000000", parsed?.reference)
        assertEquals(BigDecimal("25.500"), parsed?.balance)
        assertEquals(TransactionType.EXPENSE, parsed?.type)
        assertTrue(parsed?.isMobileWallet == true)
        assertTrue(OrangeMoneyJordanParser().canHandle("Orange Money"))
    }
}
