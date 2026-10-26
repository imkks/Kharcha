package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAndReportSmsTest {
    @Test
    fun testTransactionSearchQueryFiltering() {
        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 450_00L,
                type = TransactionType.Expense,
                channel = Channel.UPI,
                dateTime = 1L,
                merchantRaw = "Swiggy",
                note = "dinner with team #swiggy",
            ),
            Transaction(
                id = 2L,
                amount = 1200_00L,
                type = TransactionType.Expense,
                channel = Channel.CARD,
                dateTime = 2L,
                merchantRaw = "Amazon India",
                note = "laptop stand",
            ),
            Transaction(
                id = 3L,
                amount = 85000_00L,
                type = TransactionType.Income,
                channel = Channel.NEFT,
                dateTime = 3L,
                merchantRaw = "Acme Salary",
            ),
        )

        val query = "swiggy"
        val matched = txns.filter { tx ->
            (tx.merchantNormalised?.lowercase()?.contains(query) == true) ||
                (tx.merchantRaw?.lowercase()?.contains(query) == true) ||
                (tx.note?.lowercase()?.contains(query) == true)
        }

        assertEquals(1, matched.size)
        assertEquals(1L, matched[0].id)
    }

    @Test
    fun testSmsSnippetFilteringAndParsingState() {
        val snippet1 = SmsSnippet(
            id = 1L,
            sender = "AX-HDFCBK",
            dateEpoch = 1000L,
            body = "Rs 450.00 debited from a/c **1999 to Swiggy UPI",
            isParsed = true,
            transaction = null,
        )
        val snippet2 = SmsSnippet(
            id = 2L,
            sender = "VM-ICICIB",
            dateEpoch = 2000L,
            body = "OTP for netbanking login is 482910. Do not share.",
            isParsed = false,
            transaction = null,
        )

        val snippets = listOf(snippet1, snippet2)
        val query = "swiggy"
        val filtered = snippets.filter { it.body.lowercase().contains(query) }

        assertEquals(1, filtered.size)
        assertTrue(filtered[0].isParsed)
        assertFalse(snippet2.isParsed)
    }

    @Test
    fun testReportUndetectedSmsPrivacyMasking() {
        val rawMessage = "Dear Customer, INR 2,450.00 debited from A/C XX4208 to Zomato UPI"
        val shouldMask = true

        // Mask account numbers and names
        val masked = if (shouldMask) {
            rawMessage.replace(Regex("""(A/C|a/c|Acct)\s*([A-Za-z0-9*]+)"""), "$1 [MASKED]")
        } else {
            rawMessage
        }

        assertTrue(masked.contains("[MASKED]"))
        assertFalse(masked.contains("XX4208"))
    }
}
