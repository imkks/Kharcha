package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult

// In a real application, this would take a repository of rules as a constructor argument.
// For now, it's a stub implementation that allows for future expansion.
class UserRuleLayer : CategorizationLayer {
    override val name: String = "UserRule"

    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        // TODO(T5.7): Implement actual rule checking against database.
        // Return null for now so the pipeline falls through.
        return null
    }
}
