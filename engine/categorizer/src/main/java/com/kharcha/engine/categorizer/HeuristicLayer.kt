package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult
import com.kharcha.core.model.CategorySource

class HeuristicLayer : CategorizationLayer {
    override val name: String = "Heuristic"

    private val emiRegex = Regex("(?i)\\b(emi|loans?)\\b")
    private val insuranceRegex = Regex("(?i)\\b(insurance|lic)\\b")
    private val rechargeRegex = Regex("(?i)\\b(recharge|airtel|jio|vi|vodafone|bsnl)\\b")
    private val electricityRegex = Regex("(?i)\\b(electricity|bescom|tneb|msedcl|cesc|power)\\b")

    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        val channel = input.channel?.uppercase()
        val merchant = input.merchantRaw ?: ""

        // Check channel first
        if (channel == "ATM") {
            return CategorizationResult(
                categoryId = "Cash Withdrawal",
                source = CategorySource.Heuristics,
                confidence = 0.7f,
            )
        }

        // Keyword heuristics with whole-word matching
        if (emiRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Loans and EMI",
                source = CategorySource.Heuristics,
                confidence = 0.7f,
            )
        }

        if (insuranceRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Insurance",
                source = CategorySource.Heuristics,
                confidence = 0.7f,
            )
        }

        if (rechargeRegex.containsMatchIn(merchant) || electricityRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Bills and Utilities",
                source = CategorySource.Heuristics,
                confidence = 0.7f,
            )
        }

        return null
    }
}
