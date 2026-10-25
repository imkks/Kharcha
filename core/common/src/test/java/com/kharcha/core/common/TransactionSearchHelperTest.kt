package com.kharcha.core.common

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionSearchHelperTest {
    private val testTxn =
        Transaction(
            id = 1L,
            accountId = 10L,
            type = TransactionType.Expense,
            amount = 450_00L,
            dateTime = 1790447880000L,
            merchantRaw = "SWIGGY*ORDER123",
            merchantNormalised = "Swiggy",
            categorySource = "Food & Dining",
            channel = Channel.UPI,
            refNumber = "UPI987654",
            upiId = "swiggy@icici",
            note = "Dinner with friends",
        )

    @Test
    fun testEmptyQueryMatches() {
        assertTrue(TransactionSearchHelper.matches(testTxn, ""))
        assertTrue(TransactionSearchHelper.matches(testTxn, "   "))
    }

    @Test
    fun testMerchantMatching() {
        assertTrue(TransactionSearchHelper.matches(testTxn, "swiggy"))
        assertTrue(TransactionSearchHelper.matches(testTxn, "SWIGGY"))
        assertTrue(TransactionSearchHelper.matches(testTxn, "order123"))
        assertFalse(TransactionSearchHelper.matches(testTxn, "zomato"))
    }

    @Test
    fun testAmountThresholdMatching() {
        assertTrue(TransactionSearchHelper.matches(testTxn, ">400"))
        assertTrue(TransactionSearchHelper.matches(testTxn, ">=450"))
        assertTrue(TransactionSearchHelper.matches(testTxn, "<500"))
        assertTrue(TransactionSearchHelper.matches(testTxn, "<=450"))

        assertFalse(TransactionSearchHelper.matches(testTxn, ">500"))
        assertFalse(TransactionSearchHelper.matches(testTxn, "<400"))
    }

    @Test
    fun testExactAmountMatching() {
        assertTrue(TransactionSearchHelper.matches(testTxn, "450"))
        assertTrue(TransactionSearchHelper.matches(testTxn, "450.0"))
        assertFalse(TransactionSearchHelper.matches(testTxn, "500"))
    }

    @Test
    fun testCategoryTokenMatching() {
        assertTrue(TransactionSearchHelper.matches(testTxn, "#food"))
        assertTrue(TransactionSearchHelper.matches(testTxn, "#dining"))
        assertFalse(TransactionSearchHelper.matches(testTxn, "#travel"))
    }

    @Test
    fun testChannelTokenMatching() {
        assertTrue(TransactionSearchHelper.matches(testTxn, "@upi"))
        assertFalse(TransactionSearchHelper.matches(testTxn, "@card"))
    }

    @Test
    fun testNoteAndRefMatching() {
        assertTrue(TransactionSearchHelper.matches(testTxn, "dinner"))
        assertTrue(TransactionSearchHelper.matches(testTxn, "987654"))
        assertTrue(TransactionSearchHelper.matches(testTxn, "icici"))
    }

    @Test
    fun testMerchantSuggestions() {
        val txns =
            listOf(
                testTxn,
                testTxn.copy(id = 2L, merchantNormalised = "Swiggy Instamart"),
                testTxn.copy(id = 3L, merchantNormalised = "Starbucks"),
                testTxn.copy(id = 4L, merchantNormalised = "Subway"),
            )

        val suggestions = TransactionSearchHelper.extractMerchantSuggestions(txns, "sw")
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.contains("Swiggy"))
        assertTrue(suggestions.contains("Swiggy Instamart"))

        val sSuggestions = TransactionSearchHelper.extractMerchantSuggestions(txns, "s", limit = 3)
        assertEquals(3, sSuggestions.size)
    }
}
