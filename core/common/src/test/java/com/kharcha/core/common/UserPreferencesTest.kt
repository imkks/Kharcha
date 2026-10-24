package com.kharcha.core.common

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserPreferencesTest {
    private val context: Context = mockk()
    private val sharedPreferences: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)

    private lateinit var userPreferences: DefaultUserPreferences

    @Before
    fun setUp() {
        every { context.getSharedPreferences("kharcha_user_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        userPreferences = DefaultUserPreferences(context)
    }

    @Test
    fun testIsOnboardingCompleted_defaultIsFalse() {
        every { sharedPreferences.getBoolean("key_onboarding_completed", false) } returns false
        assertFalse(userPreferences.isOnboardingCompleted)
    }

    @Test
    fun testIsOnboardingCompleted_setTrue() {
        every { sharedPreferences.getBoolean("key_onboarding_completed", false) } returns true
        userPreferences.isOnboardingCompleted = true

        verify { editor.putBoolean("key_onboarding_completed", true) }
        assertTrue(userPreferences.isOnboardingCompleted)
    }

    @Test
    fun testMonthlyBudget_defaultAndSet() {
        every { sharedPreferences.getLong("key_monthly_budget_paise", 30_000_00L) } returns 30_000_00L
        assertEquals(30_000_00L, userPreferences.monthlyBudgetMinor)

        userPreferences.monthlyBudgetMinor = 50_000_00L
        verify { editor.putLong("key_monthly_budget_paise", 50_000_00L) }
    }

    @Test
    fun testMerchantCategoryRules() {
        every { sharedPreferences.getString("key_merchant_rules_json", null) } returns null
        assertTrue(userPreferences.getMerchantCategoryRules().isEmpty())

        userPreferences.setMerchantCategory("swiggy", "Food & Dining")
        verify { editor.putString("key_merchant_rules_json", any()) }

        every { sharedPreferences.getString("key_merchant_rules_json", null) } returns
            """{"swiggy":"Food & Dining","netflix":"Entertainment"}"""
        val rules = userPreferences.getMerchantCategoryRules()
        assertEquals(2, rules.size)
        assertEquals("Food & Dining", userPreferences.getCategoryForMerchant("Swiggy"))
        assertEquals("Entertainment", userPreferences.getCategoryForMerchant("Netflix"))
    }

    @Test
    fun testClear() {
        userPreferences.clear()
        verify { editor.clear() }
    }
}
