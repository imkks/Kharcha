package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult
import com.kharcha.core.model.CategorySource
import kotlinx.serialization.json.Json
import java.io.InputStream

class DictionaryLayer(private val inputStreamProvider: () -> InputStream) : CategorizationLayer {
    override val name: String = "Dictionary"

    private val json = Json { ignoreUnknownKeys = true }
    private var dictionary: Map<String, String>? = null
    private var sortedEntries: List<Pair<String, String>>? = null

    private fun loadDictionary(): Pair<Map<String, String>, List<Pair<String, String>>> {
        val existingDict = dictionary
        val existingSorted = sortedEntries
        if (existingDict != null && existingSorted != null) {
            return Pair(existingDict, existingSorted)
        }

        return synchronized(this) {
            val d = dictionary
            val s = sortedEntries
            if (d != null && s != null) {
                Pair(d, s)
            } else {
                val map = mutableMapOf<String, String>()
                try {
                    val jsonStr = inputStreamProvider().bufferedReader().use { it.readText() }
                    val parsed = json.decodeFromString<Map<String, String>>(jsonStr)
                    for ((key, value) in parsed) {
                        val upper = key.trim().uppercase()
                        map[upper] = value
                        val normalized = MerchantNormalizer.normalize(upper)
                        if (normalized.isNotBlank()) {
                            map[normalized] = value
                        }
                    }
                } catch (@Suppress("SwallowedException") e: Exception) {
                    // Fall back to empty map
                }
                val sorted = map.toList().sortedByDescending { it.first.length }
                dictionary = map
                sortedEntries = sorted
                Pair(map, sorted)
            }
        }
    }

    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        if (input.merchantRaw.isNullOrBlank()) return null

        val normalized = MerchantNormalizer.normalize(input.merchantRaw)
        if (normalized.isBlank()) return null

        val (dict, sorted) = loadDictionary()

        // 1. Exact match on normalized or raw uppercase
        val exactMatch = dict[normalized] ?: dict[input.merchantRaw.trim().uppercase()]
        if (exactMatch != null) {
            return CategorizationResult(
                categoryId = exactMatch,
                source = CategorySource.Dictionary,
                confidence = 0.85f,
            )
        }

        // 2. Whole-word match preferring longer dictionary keys
        for ((key, categoryId) in sorted) {
            // Require key to be at least 3 characters to avoid short false positives
            if (key.length >= 3) {
                val wordRegex = Regex("(?i)\\b${Regex.escape(key)}\\b")
                if (wordRegex.containsMatchIn(normalized) || wordRegex.containsMatchIn(input.merchantRaw)) {
                    return CategorizationResult(
                        categoryId = categoryId,
                        source = CategorySource.Dictionary,
                        confidence = 0.80f,
                    )
                }
            }
        }

        return null
    }
}
