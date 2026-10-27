package com.kharcha.engine.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClauseSemanticScoperTest {

    @Test
    fun testSplitIntoClausesPreservesDecimalsAndAbbreviations() {
        val raw = "Dear Customer, Rs. 100.50 debited from A/c. 1234 on 24-Oct. Avail Bal: Rs. 15,200.00"
        val clauses = ClauseSemanticScoper.splitIntoClauses(raw)
        assertEquals(2, clauses.size)
        assertEquals("Dear Customer, Rs. 100.50 debited from A/c. 1234 on 24-Oct", clauses[0])
        assertEquals("Avail Bal: Rs. 15,200.00", clauses[1])
    }

    @Test
    fun testIsPureBalanceClause() {
        assertTrue(ClauseSemanticScoper.isPureBalanceClause("Avail Bal: Rs. 25,000.00"))
        assertTrue(ClauseSemanticScoper.isPureBalanceClause("Available limit: INR 1,00,000"))
        assertTrue(ClauseSemanticScoper.isPureBalanceClause("Total balance in account is Rs 4,500"))
        assertFalse(ClauseSemanticScoper.isPureBalanceClause("Rs 450.00 debited from a/c XX1234"))
        assertFalse(ClauseSemanticScoper.isPureBalanceClause("Your A/c is credited with Rs 5,000.00"))
    }

    @Test
    fun testIsolateTransactionClauseWhenBalancePrecedes() {
        val raw = "Avl Bal Rs 25,000.00. Rs 450 debited from A/c XX1234 for Swiggy."
        val isolated = ClauseSemanticScoper.isolateTransactionClause(raw)
        assertEquals("Rs 450 debited from A/c XX1234 for Swiggy", isolated)
    }

    @Test
    fun testIsolateTransactionClauseWhenBalanceFollows() {
        val raw = "Rs 1,200.00 spent on ICICI Card XX9999 at RELIANCE. Avail Limit: INR 50,000."
        val isolated = ClauseSemanticScoper.isolateTransactionClause(raw)
        assertEquals("Rs 1,200.00 spent on ICICI Card XX9999 at RELIANCE", isolated)
    }

    @Test
    fun testIsolateTransactionClauseWithNewlines() {
        val raw = """
            Alert!
            A/c *1999 debited by Rs. 850.00 on 24-Oct-26
            Available balance: Rs. 42,000.00
        """.trimIndent()
        val isolated = ClauseSemanticScoper.isolateTransactionClause(raw)
        assertEquals("A/c *1999 debited by Rs. 850.00 on 24-Oct-26", isolated)
    }
}
