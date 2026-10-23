package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult

interface CategorizationLayer {
    suspend fun categorize(input: CategorizationInput): CategorizationResult?
}
