package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult

interface CategorizationLayer {
    val name: String get() = this::class.simpleName.orEmpty()

    suspend fun categorize(input: CategorizationInput): CategorizationResult?
}
