package com.kharcha.feature.insights

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentChannelChartTest {

    private fun createTxn(
        id: Long,
        amount: Long,
        channel: Channel,
    ): Transaction =
        Transaction(
            id = id,
            accountId = 1L,
            type = TransactionType.EXPENSE,
            amount = amount,
            dateTime = 1792900000000L,
            channel = channel,
            createdAt = 1792900000000L,
            updatedAt = 1792900000000L,
        )

    @Test
    fun testGetChannelColorDistinct() {
        val upiColor = getChannelColor("UPI")
        val cardColor = getChannelColor("CARD")
        val atmColor = getChannelColor("ATM")
        val otherColor = getChannelColor("OTHER")

        assertNotNull(upiColor)
        assertNotNull(cardColor)
        assertNotNull(atmColor)
        assertNotNull(otherColor)

        assertNotEquals(upiColor, cardColor)
        assertNotEquals(upiColor, atmColor)
    }

    @Test
    fun testChannelSharesTransactionCountAndPercentages() {
        val txns = listOf(
            createTxn(1L, 600_00L, Channel.UPI),
            createTxn(2L, 400_00L, Channel.UPI),
            createTxn(3L, 1000_00L, Channel.CARD),
        )

        val shares = InsightsAnalyticsHelper.calculateChannelShares(txns)
        assertEquals(2, shares.size)

        val upi = shares.first { it.channelName == "UPI" }
        assertEquals(1000_00L, upi.totalMinor)
        assertEquals(0.5f, upi.percentage, 0.001f)
        assertEquals(2, upi.transactionCount)

        val card = shares.first { it.channelName == "CARD" }
        assertEquals(1000_00L, card.totalMinor)
        assertEquals(0.5f, card.percentage, 0.001f)
        assertEquals(1, card.transactionCount)
    }

    @Test
    fun testInsightsTabEnumHasThreeTabs() {
        assertEquals(3, InsightsTab.entries.size)
        assertTrue(InsightsTab.entries.contains(InsightsTab.OVERVIEW))
        assertTrue(InsightsTab.entries.contains(InsightsTab.BUDGETS))
        assertTrue(InsightsTab.entries.contains(InsightsTab.RECURRING))
    }
}
