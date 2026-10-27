package com.kharcha.core.common

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

interface UserPreferences {
    var isOnboardingCompleted: Boolean
    var monthlyBudgetMinor: Long
    var isPrivacyModeEnabled: Boolean
    var isLaunchInMoneyManagerEnabled: Boolean
    var autoTransferThresholdPaise: Long
    fun getMerchantCategoryRules(): Map<String, String>
    fun setMerchantCategory(merchant: String, category: String)
    fun removeMerchantCategoryRule(merchant: String)
    fun getCategoryForMerchant(merchant: String): String?
    fun getCategoryBudgetMinor(category: String): Long
    fun setCategoryBudgetMinor(category: String, budgetMinor: Long)
    fun getAllCategoryBudgets(): Map<String, Long>
    fun getCustomCategories(): Set<String>
    fun addCustomCategory(category: String)
    fun removeCustomCategory(category: String)
    fun getCustomSpendCategories(): Set<String>
    fun addCustomSpendCategory(category: String)
    fun removeCustomSpendCategory(category: String)
    fun getCustomCreditCategories(): Set<String>
    fun addCustomCreditCategory(category: String)
    fun removeCustomCreditCategory(category: String)
    fun getUnlinkedAccountIds(): Set<Long>
    fun setAccountUnlinked(accountId: Long, unlinked: Boolean)
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

    override var isLaunchInMoneyManagerEnabled: Boolean
        get() = prefs.getBoolean(KEY_LAUNCH_IN_MONEY_MANAGER, true)
        set(value) = prefs.edit().putBoolean(KEY_LAUNCH_IN_MONEY_MANAGER, value).apply()

    override var autoTransferThresholdPaise: Long
        get() = prefs.getLong(KEY_AUTO_TRANSFER_THRESHOLD, 10_000_00L)
        set(value) = prefs.edit().putLong(KEY_AUTO_TRANSFER_THRESHOLD, value).apply()

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

    override fun removeMerchantCategoryRule(merchant: String) {
        val normalized = merchant.trim().lowercase()
        if (normalized.isBlank()) return
        val current = getMerchantCategoryRules().toMutableMap()
        current.remove(normalized)
        val json = JSONObject(current as Map<*, *>)
        prefs.edit().putString(KEY_MERCHANT_RULES, json.toString()).apply()
    }

    override fun getCategoryForMerchant(merchant: String): String? {
        val normalized = merchant.trim().lowercase()
        return getMerchantCategoryRules()[normalized]
    }

    override fun getCustomCategories(): Set<String> = getCustomSpendCategories()

    override fun addCustomCategory(category: String) = addCustomSpendCategory(category)

    override fun removeCustomCategory(category: String) = removeCustomSpendCategory(category)

    override fun getCustomSpendCategories(): Set<String> {
        val raw = prefs.getString(KEY_CUSTOM_SPEND_CATEGORIES, null)
            ?: prefs.getString(KEY_CUSTOM_CATEGORIES, null)
            ?: return emptySet()
        return try {
            val jsonArray = org.json.JSONArray(raw)
            val result = mutableSetOf<String>()
            for (i in 0 until jsonArray.length()) {
                val cat = jsonArray.optString(i)?.trim()
                if (!cat.isNullOrBlank()) {
                    result.add(cat)
                }
            }
            result
        } catch (_: Exception) {
            emptySet()
        }
    }

    override fun addCustomSpendCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val current = getCustomSpendCategories().toMutableSet()
        current.add(trimmed)
        val jsonArray = org.json.JSONArray(current.toList())
        prefs.edit().putString(KEY_CUSTOM_SPEND_CATEGORIES, jsonArray.toString()).apply()
    }

    override fun removeCustomSpendCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val current = getCustomSpendCategories().toMutableSet()
        current.remove(trimmed)
        val jsonArray = org.json.JSONArray(current.toList())
        prefs.edit().putString(KEY_CUSTOM_SPEND_CATEGORIES, jsonArray.toString()).apply()
    }

    override fun getCustomCreditCategories(): Set<String> {
        val raw = prefs.getString(KEY_CUSTOM_CREDIT_CATEGORIES, null) ?: return emptySet()
        return try {
            val jsonArray = org.json.JSONArray(raw)
            val result = mutableSetOf<String>()
            for (i in 0 until jsonArray.length()) {
                val cat = jsonArray.optString(i)?.trim()
                if (!cat.isNullOrBlank()) {
                    result.add(cat)
                }
            }
            result
        } catch (_: Exception) {
            emptySet()
        }
    }

    override fun addCustomCreditCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val current = getCustomCreditCategories().toMutableSet()
        current.add(trimmed)
        val jsonArray = org.json.JSONArray(current.toList())
        prefs.edit().putString(KEY_CUSTOM_CREDIT_CATEGORIES, jsonArray.toString()).apply()
    }

    override fun removeCustomCreditCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val current = getCustomCreditCategories().toMutableSet()
        current.remove(trimmed)
        val jsonArray = org.json.JSONArray(current.toList())
        prefs.edit().putString(KEY_CUSTOM_CREDIT_CATEGORIES, jsonArray.toString()).apply()
    }

    override fun getAllCategoryBudgets(): Map<String, Long> {
        val raw = prefs.getString(KEY_CATEGORY_BUDGETS, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, Long>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getLong(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun getCategoryBudgetMinor(category: String): Long {
        val normalized = category.trim().lowercase()
        return getAllCategoryBudgets()[normalized] ?: 0L
    }

    override fun setCategoryBudgetMinor(category: String, budgetMinor: Long) {
        val normalized = category.trim().lowercase()
        if (normalized.isBlank()) return
        val current = getAllCategoryBudgets().toMutableMap()
        if (budgetMinor <= 0L) {
            current.remove(normalized)
        } else {
            current[normalized] = budgetMinor
        }
        val json = JSONObject(current as Map<*, *>)
        prefs.edit().putString(KEY_CATEGORY_BUDGETS, json.toString()).apply()
    }

    override fun getUnlinkedAccountIds(): Set<Long> {
        val raw = prefs.getString(KEY_UNLINKED_ACCOUNT_IDS, null) ?: return emptySet()
        return try {
            val jsonArray = org.json.JSONArray(raw)
            val set = mutableSetOf<Long>()
            for (i in 0 until jsonArray.length()) {
                set.add(jsonArray.getLong(i))
            }
            set
        } catch (_: Exception) {
            emptySet()
        }
    }

    override fun setAccountUnlinked(accountId: Long, unlinked: Boolean) {
        val current = getUnlinkedAccountIds().toMutableSet()
        if (unlinked) {
            current.add(accountId)
        } else {
            current.remove(accountId)
        }
        val jsonArray = org.json.JSONArray(current.toList())
        prefs.edit().putString(KEY_UNLINKED_ACCOUNT_IDS, jsonArray.toString()).apply()
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
        private const val KEY_CATEGORY_BUDGETS = "key_category_budgets_json"
        private const val KEY_CUSTOM_CATEGORIES = "key_custom_categories_json"
        private const val KEY_CUSTOM_SPEND_CATEGORIES = "key_custom_spend_categories_json"
        private const val KEY_CUSTOM_CREDIT_CATEGORIES = "key_custom_credit_categories_json"
        private const val KEY_LAUNCH_IN_MONEY_MANAGER = "key_launch_in_money_manager"
        private const val KEY_AUTO_TRANSFER_THRESHOLD = "key_auto_transfer_threshold"
        private const val KEY_UNLINKED_ACCOUNT_IDS = "key_unlinked_account_ids_json"
        const val DEFAULT_BUDGET_PAISE: Long = 30_000_00L
    }
}
