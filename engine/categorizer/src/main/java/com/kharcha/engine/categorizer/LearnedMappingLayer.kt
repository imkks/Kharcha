package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult

// Stub implementation for now.
class LearnedMappingLayer : CategorizationLayer {
    override val name: String = "LearnedMapping"

    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        // TODO(T4.2): Query local DB for mappings learned from past user corrections.
        return null
    }
}
