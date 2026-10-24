package com.kharcha.core.common

import android.content.Context
import android.content.SharedPreferences

interface UserPreferences {
    var isOnboardingCompleted: Boolean
    var monthlyBudgetMinor: Long
    fun clear()
}

class DefaultUserPreferences(context: Context) : UserPreferences {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    override var monthlyBudgetMinor: Long
        get() = prefs.getLong(KEY_MONTHLY_BUDGET, DEFAULT_BUDGET_PAISE)
        set(value) = prefs.edit().putLong(KEY_MONTHLY_BUDGET, value).apply()

    override fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "kharcha_user_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_MONTHLY_BUDGET = "key_monthly_budget_paise"
        const val DEFAULT_BUDGET_PAISE: Long = 30_000_00L
    }
}
