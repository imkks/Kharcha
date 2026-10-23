package com.kharcha.engine.categorizer

import com.kharcha.core.model.Rule
import java.util.UUID
import javax.inject.Inject

class CategorizationLearner
    @Inject
    constructor() {
        fun applyCorrection(
            transactionId: String,
            merchant: String,
            categoryId: String,
            applyToPast: Boolean,
        ): Rule? {
            if (!applyToPast) {
                return null
            }

            // Return a rule to be saved
            return Rule(
                id = UUID.randomUUID().toString(),
                merchantPattern = merchant,
                targetCategoryId = categoryId,
            )
        }
    }
