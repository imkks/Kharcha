package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult
import com.kharcha.core.model.CategorySource
import com.kharcha.core.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizationPipelineTest {
    @Test
    fun testPipelineRespectsOrderAndThreshold() =
        runBlocking {
            val highConfidenceLayer =
                object : CategorizationLayer {
                    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
                        if (input.merchantRaw == "MATCH") {
                            return CategorizationResult("High", null, CategorySource.UserRule, 0.9f)
                        }
                        return null
                    }
                }

            val lowConfidenceLayer =
                object : CategorizationLayer {
                    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
                        if (input.merchantRaw == "LOW") {
                            return CategorizationResult("Low", null, CategorySource.Heuristics, 0.5f)
                        }
                        return null
                    }
                }

            val pipeline =
                CategorizationPipeline(
                    layers = listOf(highConfidenceLayer, lowConfidenceLayer),
                    confidenceThreshold = 0.6f,
                )

            // 1. High confidence match
            val res1 = pipeline.categorize(CategorizationInput(100L, "MATCH", null, null, TransactionType.Expense))
            assertEquals("High", res1.categoryId)

            // 2. Low confidence falls through to fallback
            val res2 = pipeline.categorize(CategorizationInput(100L, "LOW", null, null, TransactionType.Expense))
            assertEquals("Unknown Category", res2.categoryId)
            assertEquals(CategorySource.Fallback, res2.source)

            // 3. No match falls through to fallback
            val res3 = pipeline.categorize(CategorizationInput(100L, "UNKNOWN", null, null, TransactionType.Expense))
            assertEquals("Unknown Category", res3.categoryId)
            assertEquals(CategorySource.Fallback, res3.source)
        }
}
