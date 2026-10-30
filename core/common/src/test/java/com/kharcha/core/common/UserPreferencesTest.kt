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
    fun testPrivacyMode_defaultAndSet() {
        every { sharedPreferences.getBoolean("key_privacy_mode_enabled", false) } returns false
        assertFalse(userPreferences.isPrivacyModeEnabled)

        every { sharedPreferences.getBoolean("key_privacy_mode_enabled", false) } returns true
        userPreferences.isPrivacyModeEnabled = true
        verify { editor.putBoolean("key_privacy_mode_enabled", true) }
        assertTrue(userPreferences.isPrivacyModeEnabled)
    }

    @Test
    fun profileFieldsHaveNoPrepopulatedPersonalDetails() {
        every { sharedPreferences.getString("key_user_name", "") } returns ""
        every { sharedPreferences.getString("key_user_phone", "") } returns ""
        assertEquals("", userPreferences.userName)
        assertEquals("", userPreferences.userPhone)

        userPreferences.userName = "Asha"
        userPreferences.userPhone = "+919876543210"
        verify { editor.putString("key_user_name", "Asha") }
        verify { editor.putString("key_user_phone", "+919876543210") }
    }

    @Test
    fun expenseNotifications_defaultToEnabledAndCanBeDisabled() {
        every { sharedPreferences.getBoolean("key_expense_notifications_enabled", true) } returns true
        assertTrue(userPreferences.isExpenseNotificationsEnabled)

        userPreferences.isExpenseNotificationsEnabled = false
        verify { editor.putBoolean("key_expense_notifications_enabled", false) }

        every { sharedPreferences.getBoolean("key_expense_notifications_enabled", true) } returns false
        assertFalse(userPreferences.isExpenseNotificationsEnabled)
    }

    @Test
    fun reminderAndSecurityPreferencesHaveScreenshotDefaults() {
        every { sharedPreferences.getBoolean("key_hide_default_sms_notifications", false) } returns false
        every { sharedPreferences.getBoolean("key_daily_summary_enabled", true) } returns true
        every { sharedPreferences.getInt("key_daily_summary_time", 22 * 60) } returns 22 * 60
        every { sharedPreferences.getBoolean("key_weekly_summary_enabled", true) } returns true
        every { sharedPreferences.getInt("key_weekly_summary_time", 11 * 60) } returns 11 * 60
        every { sharedPreferences.getBoolean("key_bill_reminder_enabled", true) } returns true
        every { sharedPreferences.getBoolean("key_app_lock_enabled", false) } returns false
        every { sharedPreferences.getBoolean("key_income_hidden", false) } returns false

        assertFalse(userPreferences.hideDefaultSmsNotifications)
        assertTrue(userPreferences.isDailySummaryEnabled)
        assertEquals(22 * 60, userPreferences.dailySummaryMinuteOfDay)
        assertTrue(userPreferences.isWeeklySummaryEnabled)
        assertEquals(11 * 60, userPreferences.weeklySummaryMinuteOfDay)
        assertTrue(userPreferences.isBillReminderEnabled)
        assertFalse(userPreferences.isAppLockEnabled)
        assertFalse(userPreferences.isIncomeHidden)

        userPreferences.dailySummaryMinuteOfDay = 1500
        userPreferences.weeklySummaryMinuteOfDay = -1
        verify { editor.putInt("key_daily_summary_time", 1439) }
        verify { editor.putInt("key_weekly_summary_time", 0) }
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

        userPreferences.removeMerchantCategoryRule("swiggy")
        verify { editor.putString("key_merchant_rules_json", any()) }
    }

    @Test
    fun testCustomCategories() {
        every { sharedPreferences.getString("key_custom_spend_categories_json", null) } returns null
        every { sharedPreferences.getString("key_custom_categories_json", null) } returns null
        assertTrue(userPreferences.getCustomCategories().isEmpty())

        userPreferences.addCustomCategory("Chicken")
        verify { editor.putString("key_custom_spend_categories_json", any()) }

        every { sharedPreferences.getString("key_custom_spend_categories_json", null) } returns
            """["Chicken","Gym"]"""
        val customCats = userPreferences.getCustomCategories()
        assertEquals(2, customCats.size)
        assertTrue(customCats.contains("Chicken"))
        assertTrue(customCats.contains("Gym"))

        userPreferences.removeCustomCategory("Chicken")
        verify { editor.putString("key_custom_spend_categories_json", any()) }
    }

    @Test
    fun testSpendAndCreditCategories() {
        every { sharedPreferences.getString("key_custom_spend_categories_json", null) } returns null
        every { sharedPreferences.getString("key_custom_categories_json", null) } returns null
        assertTrue(userPreferences.getCustomSpendCategories().isEmpty())

        userPreferences.addCustomSpendCategory("Groceries Special")
        verify { editor.putString("key_custom_spend_categories_json", any()) }

        every { sharedPreferences.getString("key_custom_credit_categories_json", null) } returns null
        assertTrue(userPreferences.getCustomCreditCategories().isEmpty())

        userPreferences.addCustomCreditCategory("Crypto Dividend")
        verify { editor.putString("key_custom_credit_categories_json", any()) }
    }

    @Test
    fun testPreferencesToggles() {
        every { sharedPreferences.getBoolean("key_launch_in_money_manager", true) } returns true
        assertTrue(userPreferences.isLaunchInMoneyManagerEnabled)

        userPreferences.isLaunchInMoneyManagerEnabled = false
        verify { editor.putBoolean("key_launch_in_money_manager", false) }

        every { sharedPreferences.getLong("key_auto_transfer_threshold", 10_000_00L) } returns 10_000_00L
        assertEquals(10_000_00L, userPreferences.autoTransferThresholdPaise)

        userPreferences.autoTransferThresholdPaise = 25_000_00L
        verify { editor.putLong("key_auto_transfer_threshold", 25_000_00L) }
    }

    @Test
    fun testCategoryBudgets() {
        every { sharedPreferences.getString("key_category_budgets_json", null) } returns null
        assertTrue(userPreferences.getAllCategoryBudgets().isEmpty())
        assertEquals(0L, userPreferences.getCategoryBudgetMinor("Food & Dining"))

        userPreferences.setCategoryBudgetMinor("Food & Dining", 15_000_00L)
        verify { editor.putString("key_category_budgets_json", any()) }

        every { sharedPreferences.getString("key_category_budgets_json", null) } returns
            """{"food & dining":1500000,"shopping":800000}"""
        val budgets = userPreferences.getAllCategoryBudgets()
        assertEquals(2, budgets.size)
        assertEquals(15_000_00L, userPreferences.getCategoryBudgetMinor("Food & Dining"))
        assertEquals(8_000_00L, userPreferences.getCategoryBudgetMinor("Shopping"))
    }

    @Test
    fun testClear() {
        userPreferences.clear()
        verify { editor.clear() }
    }
}
