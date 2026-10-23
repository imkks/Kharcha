package com.kharcha.core.model

enum class CategorySource {
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
