package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult
import com.kharcha.core.model.CategorySource

class HeuristicLayer : CategorizationLayer {
    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        val channel = input.channel?.uppercase()
        val merchant = input.merchantRaw?.uppercase() ?: ""

        // Check channel first
        if (channel == "ATM") {
            return CategorizationResult(
                categoryId = "Cash Withdrawal",
                source = CategorySource.Heuristics,
                confidence = 0.9f,
            )
        }

        // Keyword heuristics
        if (merchant.contains("EMI") || merchant.contains("LOAN")) {
            return CategorizationResult(
                categoryId = "Loans and EMI",
                source = CategorySource.Heuristics,
                confidence = 0.85f,
            )
        }

        if (merchant.contains("INSURANCE") || merchant.contains("LIC ")) {
            return CategorizationResult(
                categoryId = "Insurance",
                source = CategorySource.Heuristics,
                confidence = 0.85f,
            )
        }

        val utilitiesRecharge = listOf("RECHARGE", "AIRTEL", "JIO", "VI")
        if (utilitiesRecharge.any { merchant.contains(it) }) {
            return CategorizationResult(
                categoryId = "Bills and Utilities",
                source = CategorySource.Heuristics,
                confidence = 0.8f,
            )
        }

        val utilitiesElectric = listOf("ELECTRICITY", "BESCOM", "TNEB", "MSEDCL")
        if (utilitiesElectric.any { merchant.contains(it) }) {
            return CategorizationResult(
                categoryId = "Bills and Utilities",
                source = CategorySource.Heuristics,
                confidence = 0.8f,
            )
        }

        return null
    }
}
