package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult

class CategorizationPipeline(
    private val layers: List<CategorizationLayer>,
    private val confidenceThreshold: Float = 0.6f,
) {
    suspend fun run(input: CategorizationInput): CategorizationResult = categorize(input)

    suspend fun categorize(input: CategorizationInput): CategorizationResult {
        for (layer in layers) {
            val result = layer.categorize(input)
            if (result != null && result.confidence >= confidenceThreshold) {
                return result
            }
        }
        // If all layers return null or below threshold (which shouldn't happen if FallbackLayer is last)
        // just return a fallback.
        return FallbackLayer().categorize(input)
    }
}
