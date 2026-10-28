package com.kharcha.engine.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderFilterTest {
    @Test
    fun isFinancialSenderreturnsfalseforstandardmobilenumbers() {
        assertFalse(SenderFilter().isFinancialSender("9876543210"))
        assertFalse(SenderFilter().isFinancialSender("+919876543210"))
    }

    @Test
    fun isFinancialSenderreturnstrueforfinancialsenderIDs() {
        assertTrue(SenderFilter().isFinancialSender("XX-HDFCBK"))
        assertTrue(SenderFilter().isFinancialSender("VM-SBIUPI"))
        assertTrue(SenderFilter().isFinancialSender("AX-ICICIB"))
        assertTrue(SenderFilter().isFinancialSender("AD-ICICIT-S"))
        assertTrue(SenderFilter().isFinancialSender("AD-IDFCFB-S"))
        assertTrue(SenderFilter().isFinancialSender("AD-KOTAKB"))
    }

    @Test
    fun isFinancialSenderreturnsfalsefornonalphabeticIDs() {
        assertFalse(SenderFilter().isFinancialSender("123456"))
    }
}
