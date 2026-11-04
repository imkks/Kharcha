package com.kharcha.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCategoryTest {

    @Test
    fun expenseCategoriesContainOnlyDebitCategories() {
        val expenseNames = TransactionCategory.Expense.ALL.map { it.name }

        assertTrue(expenseNames.contains("Food & Dining"))
        assertTrue(expenseNames.contains("Groceries"))
        assertTrue(expenseNames.contains("Investments"))
        assertTrue(expenseNames.contains("Fuel"))
        assertTrue(expenseNames.contains("Other"))
        assertTrue(expenseNames.contains("Unknown"))

        // Must NOT contain credit categories
        assertFalse(expenseNames.contains("Salary"))
        assertFalse(expenseNames.contains("Investment Return"))
        assertFalse(expenseNames.contains("Cashback"))
        assertFalse(expenseNames.contains("Allowance"))
    }

    @Test
    fun incomeCategoriesContainOnlyCreditCategories() {
        val incomeNames = TransactionCategory.Income.ALL.map { it.name }

        assertTrue(incomeNames.contains("Salary"))
        assertTrue(incomeNames.contains("Investment Return"))
        assertTrue(incomeNames.contains("Cashback"))
        assertTrue(incomeNames.contains("Refund"))
        assertTrue(incomeNames.contains("Other"))
        assertTrue(incomeNames.contains("Unknown"))

        // Must NOT contain debit categories
        assertFalse(incomeNames.contains("Investments"))
        assertFalse(incomeNames.contains("Food & Dining"))
        assertFalse(incomeNames.contains("Fuel"))
        assertFalse(incomeNames.contains("Groceries"))
    }

    @Test
    fun allowedCategoriesMatchesDirection() {
        val expenseCategories = TransactionType.Expense.allowedCategoryNames()
        val incomeCategories = TransactionType.Income.allowedCategoryNames()

        assertEquals(TransactionCategory.Expense.ALL.map { it.name }, expenseCategories)
        assertEquals(TransactionCategory.Income.ALL.map { it.name }, incomeCategories)
    }

    @Test
    fun commonSuggestionsAreDirectionSpecific() {
        val expenseSuggestions = TransactionType.Expense.commonSuggestions().map { it.name }
        val incomeSuggestions = TransactionType.Income.commonSuggestions().map { it.name }

        assertTrue(expenseSuggestions.contains("Food & Dining"))
        assertFalse(expenseSuggestions.contains("Salary"))

        assertTrue(incomeSuggestions.contains("Salary"))
        assertFalse(incomeSuggestions.contains("Food & Dining"))
    }
}
