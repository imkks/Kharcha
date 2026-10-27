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
    private val fuelRegex = Regex("(?i)\\b(fuel|petrol|diesel|petroleum|filling\\s+station|hpcl|bpcl|iocl)\\b")
    private val meatGroceriesRegex = Regex("(?i)\\b(chicke[a-z]*|chicken|mutton|meat|fish|poultry)\\b")
    private val personalCareRegex = Regex("(?i)\\b(saloo[a-z]*|salon|beauty\\s+parlour|hair\\s+style|spa)\\b")
    private val pharmacyRegex = Regex("(?i)\\b(medic[a-z]*|pharm[a-z]*|chemist)\\b")
    private val stationeryRegex = Regex("(?i)\\b(statio[a-z]*|xerox|printers?)\\b")
    private val bakeryDiningRegex = Regex("(?i)\\b(bakery|bakers|sweets?|mithai|pastry)\\b")

    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        val channel = input.channel?.uppercase()
        val merchant = input.merchantRaw ?: ""

        // 1. Channel checks
        if (channel == "ATM") {
            return CategorizationResult(
                categoryId = "Cash Withdrawal",
                source = CategorySource.Heuristics,
                confidence = 0.85f,
            )
        }

        // 2. Keyword heuristics with whole-word/root matching
        if (fuelRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Fuel",
                source = CategorySource.Heuristics,
                confidence = 0.85f,
            )
        }

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

        if (meatGroceriesRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Groceries",
                source = CategorySource.Heuristics,
                confidence = 0.80f,
            )
        }

        if (pharmacyRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Pharmacy",
                source = CategorySource.Heuristics,
                confidence = 0.80f,
            )
        }

        if (personalCareRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Personal Care",
                source = CategorySource.Heuristics,
                confidence = 0.80f,
            )
        }

        if (stationeryRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Education",
                source = CategorySource.Heuristics,
                confidence = 0.80f,
            )
        }

        if (bakeryDiningRegex.containsMatchIn(merchant)) {
            return CategorizationResult(
                categoryId = "Food & Dining",
                source = CategorySource.Heuristics,
                confidence = 0.80f,
            )
        }

        return null
    }
}
