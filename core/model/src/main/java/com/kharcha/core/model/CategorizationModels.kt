package com.kharcha.core.model

enum class CategorySource {
    Manual,
    UserRule,
    LearnedMapping,
    Dictionary,
    Heuristics,
    ML,
    Fallback,
}

data class CategorizationInput(
    // Minor units
    val amount: Long,
    val merchantRaw: String?,
    val channel: String?,
    val upiId: String?,
    val type: TransactionType,
)

data class CategorizationResult(
    val categoryId: String,
    val subcategoryId: String? = null,
    val source: CategorySource,
    val confidence: Float,
)

data class LocalRule(
    val id: Long = 0L,
    val upiId: String? = null,
    val merchantNormalised: String? = null,
    // Minor units (Paise/Cents)
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val categoryId: String,
    val subcategoryId: String? = null,
    val priority: Int = 50,
)
