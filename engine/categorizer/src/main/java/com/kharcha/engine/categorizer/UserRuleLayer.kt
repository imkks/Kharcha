package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult
import com.kharcha.core.model.CategorySource
import com.kharcha.core.model.LocalRule

class UserRuleLayer(
    private val ruleProvider: suspend () -> List<LocalRule> = { emptyList() },
) : CategorizationLayer {
    override val name: String = "UserRule"

    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        val rules = ruleProvider()
        if (rules.isEmpty()) return null

        val rawMerchant = input.merchantRaw.orEmpty()
        val normalizedMerchant = MerchantNormalizer.normalize(rawMerchant)
        val upiId = input.upiId?.trim()?.uppercase()
        val amount = input.amount

        // Sort rules so that:
        // 1. Higher priority wins
        // 2. Specific amount-bound rules win over generic rules for the same priority
        // 3. Newer rules win ties
        val sortedRules =
            rules.sortedWith(
                compareByDescending<LocalRule> { it.priority }
                    .thenByDescending { it.minAmount != null || it.maxAmount != null }
                    .thenByDescending { it.id },
            )

        for (rule in sortedRules) {
            val minAmt = rule.minAmount
            val maxAmt = rule.maxAmount
            val ruleUpi = rule.upiId
            val ruleMerchant = rule.merchantNormalised?.trim()?.uppercase()

            // Check amount range if defined
            if (minAmt != null && amount < minAmt) continue
            if (maxAmt != null && amount > maxAmt) continue

            // 1. Match exact UPI ID if rule specifies UPI ID
            if (!ruleUpi.isNullOrBlank() && upiId != null) {
                if (ruleUpi.trim().equals(upiId, ignoreCase = true)) {
                    return CategorizationResult(
                        categoryId = rule.categoryId,
                        subcategoryId = rule.subcategoryId,
                        source = CategorySource.UserRule,
                        confidence = 1.0f,
                    )
                }
            }

            // 2. Match merchant name (against normalized or raw)
            if (!ruleMerchant.isNullOrBlank()) {
                val matchesNormalized =
                    normalizedMerchant.isNotBlank() &&
                        (normalizedMerchant == ruleMerchant || normalizedMerchant.startsWith(ruleMerchant))
                val matchesRaw =
                    rawMerchant.isNotBlank() &&
                        rawMerchant.trim().uppercase().contains(ruleMerchant)

                if (matchesNormalized || matchesRaw) {
                    return CategorizationResult(
                        categoryId = rule.categoryId,
                        subcategoryId = rule.subcategoryId,
                        source = CategorySource.UserRule,
                        confidence = 1.0f,
                    )
                }
            }
        }

        return null
    }
}
