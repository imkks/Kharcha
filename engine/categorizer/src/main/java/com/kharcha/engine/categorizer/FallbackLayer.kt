package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult
import com.kharcha.core.model.CategorySource

class FallbackLayer : CategorizationLayer {
    override val name: String = "Fallback"

    override suspend fun categorize(input: CategorizationInput): CategorizationResult {
        return CategorizationResult(
            categoryId = "Unknown Category",
            source = CategorySource.Fallback,
            confidence = 0.0f,
        )
    }
}
