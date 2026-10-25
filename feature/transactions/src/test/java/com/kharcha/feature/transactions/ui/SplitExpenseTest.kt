package com.kharcha.feature.transactions.ui

import com.kharcha.core.common.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitExpenseTest {

    @Test
    fun testEqualSplitThreeWaysWithPaiseDivision() {
        val totalAmount = 100_00L // ₹100.00 = 10000 paise
        val participants = listOf("You", "Person 2", "Person 3")
        val shares = SplitExpenseCalculator.calculateShares(totalAmount, participants)

        assertEquals(3, shares.size)
        // First participant receives the extra remainder paise
        assertEquals(33_34L, shares[0].amountMinor)
        assertEquals(33_33L, shares[1].amountMinor)
        assertEquals(33_33L, shares[2].amountMinor)

        // Sum of shares must strictly equal total amount
        assertEquals(totalAmount, shares.sumOf { it.amountMinor })

        // Formatted values match paise representation
        assertEquals("₹33.34", MoneyFormatter.formatMinorUnits(shares[0].amountMinor))
        assertEquals("₹33.33", MoneyFormatter.formatMinorUnits(shares[1].amountMinor))
        assertEquals("₹33.33", MoneyFormatter.formatMinorUnits(shares[2].amountMinor))
    }

    @Test
    fun testEqualSplitTwoWaysEvenDivision() {
        val totalAmount = 100_00L
        val participants = listOf("You", "Rahul")
        val shares = SplitExpenseCalculator.calculateShares(totalAmount, participants)

        assertEquals(2, shares.size)
        assertEquals(50_00L, shares[0].amountMinor)
        assertEquals(50_00L, shares[1].amountMinor)
        assertEquals(totalAmount, shares.sumOf { it.amountMinor })
    }

    @Test
    fun testEqualSplitSixWaysWithPaiseRemainder() {
        val totalAmount = 100_00L
        val participants = (1..6).map { "Person $it" }
        val shares = SplitExpenseCalculator.calculateShares(totalAmount, participants)

        assertEquals(6, shares.size)
        // 10000 / 6 = 1666 paise, remainder = 4 paise
        // First 4 get 1667 paise, last 2 get 1666 paise
        assertEquals(16_67L, shares[0].amountMinor)
        assertEquals(16_67L, shares[1].amountMinor)
        assertEquals(16_67L, shares[2].amountMinor)
        assertEquals(16_67L, shares[3].amountMinor)
        assertEquals(16_66L, shares[4].amountMinor)
        assertEquals(16_66L, shares[5].amountMinor)
        assertEquals(totalAmount, shares.sumOf { it.amountMinor })
    }

    @Test
    fun testShareMessageFormatting() {
        val message = SplitExpenseCalculator.formatShareMessage(
            merchant = "Starbucks",
            totalAmountMinor = 100_00L,
            shareAmountMinor = 33_34L,
        )

        val expected = "Hey! Total bill at Starbucks was ₹100.00. Your share is ₹33.34. (Kharcha expense split)"
        assertEquals(expected, message)
    }

    @Test
    fun testSplitSummaryFormatAndParse() {
        val shares = listOf(
            ParticipantShare(name = "You", amountMinor = 33_34L),
            ParticipantShare(name = "Rahul", amountMinor = 33_33L),
            ParticipantShare(name = "Priya", amountMinor = 33_33L),
        )

        val summary = SplitExpenseCalculator.formatSplitSummary(shares)
        assertEquals("[Split: You: ₹33.34, Rahul: ₹33.33, Priya: ₹33.33]", summary)

        val parsed = SplitExpenseCalculator.parseSplitSummary("Dinner with team\n$summary")
        assertEquals("You: ₹33.34, Rahul: ₹33.33, Priya: ₹33.33", parsed)
        assertTrue(SplitExpenseCalculator.isSplitActive("Dinner with team\n$summary"))
        assertFalse(SplitExpenseCalculator.isSplitActive("Regular note without split"))
    }

    @Test
    fun testAppendOrUpdateSplit() {
        val initialNote = "Team dinner #Party"
        val splitSummary = "[Split: You: ₹50.00, Person 2: ₹50.00]"

        val updatedNote = SplitExpenseCalculator.appendOrUpdateSplit(initialNote, splitSummary)
        assertEquals("Team dinner #Party\n$splitSummary", updatedNote)

        // Updating split replaces previous split block without duplication
        val newSplitSummary = "[Split: You: ₹33.34, Person 2: ₹33.33, Person 3: ₹33.33]"
        val reUpdatedNote = SplitExpenseCalculator.appendOrUpdateSplit(updatedNote, newSplitSummary)
        assertEquals("Team dinner #Party\n$newSplitSummary", reUpdatedNote)
    }
}
