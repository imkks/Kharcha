package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult
import com.kharcha.core.model.CategorySource

class FallbackLayer : CategorizationLayer {
    override suspend fun categorize(input: CategorizationInput): CategorizationResult {
        return CategorizationResult(
            categoryId = "Uncategorised",
            source = CategorySource.Fallback,
            confidence = 0.0f,
        )
    }
}
