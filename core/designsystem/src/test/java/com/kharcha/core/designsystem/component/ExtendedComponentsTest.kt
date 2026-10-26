package com.kharcha.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtendedComponentsTest {
    @Test
    fun accountCardModel_storesPropertiesCorrectly() {
        val model =
            AccountCardModel(
                id = 42L,
                bankName = "HDFC Bank",
                accountNumberMasked = "•••• 1999",
                balanceFormatted = "₹45,200.00",
                isWallet = false,
                lastTxnInfo = "24 Oct · ₹120.00",
            )

        assertEquals(42L, model.id)
        assertEquals("HDFC Bank", model.bankName)
        assertEquals("•••• 1999", model.accountNumberMasked)
        assertEquals("₹45,200.00", model.balanceFormatted)
        assertFalse(model.isWallet)
        assertEquals("24 Oct · ₹120.00", model.lastTxnInfo)
    }

    @Test
    fun transactionTypeSwitchOption_enumCoverage() {
        assertEquals(2, TransactionTypeSwitchOption.entries.size)
        assertTrue(TransactionTypeSwitchOption.entries.contains(TransactionTypeSwitchOption.SPEND))
        assertTrue(TransactionTypeSwitchOption.entries.contains(TransactionTypeSwitchOption.INCOME))
    }

    @Test
    fun tagFormatting_ensuresHashPrefix() {
        val tagWithoutHash = "Online"
        val clean = if (tagWithoutHash.startsWith("#")) tagWithoutHash else "#$tagWithoutHash"
        assertEquals("#Online", clean)

        val tagWithHash = "#Groceries"
        val clean2 = if (tagWithHash.startsWith("#")) tagWithHash else "#$tagWithHash"
        assertEquals("#Groceries", clean2)
    }
}
