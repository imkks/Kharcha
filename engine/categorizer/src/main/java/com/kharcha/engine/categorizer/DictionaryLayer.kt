package com.kharcha.engine.categorizer

import com.kharcha.core.model.CategorizationInput
import com.kharcha.core.model.CategorizationResult
import com.kharcha.core.model.CategorySource
import org.json.JSONObject
import java.io.InputStream

class DictionaryLayer(private val inputStreamProvider: () -> InputStream) : CategorizationLayer {
    private var dictionary: Map<String, String>? = null

    private fun loadDictionary(): Map<String, String> {
        return dictionary ?: synchronized(this) {
            dictionary ?: run {
                val map = mutableMapOf<String, String>()
                try {
                    val jsonStr = inputStreamProvider().bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key.uppercase()] = json.getString(key)
                    }
                } catch (e: Exception) {
                    // Ignore on failure, return empty map
                }
                map.also { dictionary = it }
            }
        }
    }

    override suspend fun categorize(input: CategorizationInput): CategorizationResult? {
        if (input.merchantRaw.isNullOrBlank()) return null

        val normalized = MerchantNormalizer.normalize(input.merchantRaw)
        if (normalized.isBlank()) return null

        val dict = loadDictionary()

        // Exact match
        dict[normalized]?.let { categoryId ->
            return CategorizationResult(
                categoryId = categoryId,
                source = CategorySource.Dictionary,
                confidence = 0.95f,
            )
        }

        // Partial match (e.g. "SWIGGY INSTAMART" matches "SWIGGY")
        // Not perfect but better than nothing for a simple dictionary lookup.
        val match = dict.entries.find { normalized.contains(it.key) || it.key.contains(normalized) }

        return match?.let {
            CategorizationResult(
                categoryId = it.value,
                source = CategorySource.Dictionary,
                confidence = 0.8f,
            )
        }
    }
}
