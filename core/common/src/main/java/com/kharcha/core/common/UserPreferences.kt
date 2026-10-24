package com.kharcha.core.common

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

interface UserPreferences {
    var isOnboardingCompleted: Boolean
    var monthlyBudgetMinor: Long
    var isPrivacyModeEnabled: Boolean
    fun getMerchantCategoryRules(): Map<String, String>
    fun setMerchantCategory(merchant: String, category: String)
    fun getCategoryForMerchant(merchant: String): String?
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

    override var isPrivacyModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVACY_MODE_ENABLED, value).apply()

    override fun getMerchantCategoryRules(): Map<String, String> {
        val raw = prefs.getString(KEY_MERCHANT_RULES, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getString(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun setMerchantCategory(merchant: String, category: String) {
        val normalized = merchant.trim().lowercase()
        if (normalized.isBlank()) return
        val current = getMerchantCategoryRules().toMutableMap()
        current[normalized] = category
        val json = JSONObject(current as Map<*, *>)
        prefs.edit().putString(KEY_MERCHANT_RULES, json.toString()).apply()
    }

    override fun getCategoryForMerchant(merchant: String): String? {
        val normalized = merchant.trim().lowercase()
        return getMerchantCategoryRules()[normalized]
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "kharcha_user_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_MONTHLY_BUDGET = "key_monthly_budget_paise"
        private const val KEY_PRIVACY_MODE_ENABLED = "key_privacy_mode_enabled"
        private const val KEY_MERCHANT_RULES = "key_merchant_rules_json"
        const val DEFAULT_BUDGET_PAISE: Long = 30_000_00L
    }
}
