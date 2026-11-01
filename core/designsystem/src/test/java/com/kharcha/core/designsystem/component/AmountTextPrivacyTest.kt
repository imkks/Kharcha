package com.kharcha.core.designsystem.component

import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountTextPrivacyTest {
    @Test
    fun incomePrivacyMasksIncomeButLeavesExpensesVisible() {
        assertTrue(shouldMaskAmount(TransactionType.Income, privacyMode = false, incomeHidden = true, maskIfPrivacy = true))
        assertFalse(shouldMaskAmount(TransactionType.Expense, privacyMode = false, incomeHidden = true, maskIfPrivacy = true))
    }

    @Test
    fun generalPrivacyMasksBothTransactionTypes() {
        assertTrue(shouldMaskAmount(TransactionType.Income, privacyMode = true, incomeHidden = false, maskIfPrivacy = true))
        assertTrue(shouldMaskAmount(TransactionType.Expense, privacyMode = true, incomeHidden = false, maskIfPrivacy = true))
    }
}
