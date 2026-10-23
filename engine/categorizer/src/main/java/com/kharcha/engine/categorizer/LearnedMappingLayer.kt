package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult

// Stub implementation for now.
class LearnedMappingLayer : CategorizationLayer {
    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        // TODO: Query local DB for mappings learned from past user corrections.
        return null
    }
}
