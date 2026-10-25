package com.kharcha.engine.parser

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.IOException

object TemplateLoader {
    private val json = Json { ignoreUnknownKeys = true }
    private var cachedTemplates: List<ParserTemplate>? = null

    @Synchronized
    fun loadFromAssets(context: Context): List<ParserTemplate> {
        cachedTemplates?.let { return it }

        val templates = mutableListOf<ParserTemplate>()
        try {
            val assetManager = context.assets
            val files = assetManager.list("templates") ?: emptyArray()
            for (file in files) {
                if (file.endsWith(".json")) {
                    try {
                        val content =
                            assetManager.open("templates/$file").bufferedReader().use {
                                it.readText()
                            }
                        val template = json.decodeFromString<ParserTemplate>(content)
                        templates.add(template)
                    } catch (e: Exception) {
                        // Skip malformed template to protect ingestion pipeline
                    }
                }
            }
        } catch (e: IOException) {
            // Asset reading failure; fallback will still operate
        }

        cachedTemplates = templates
        return templates
    }

    fun createParser(context: Context? = null): SmsParserImpl {
        val templates = context?.let { loadFromAssets(it) } ?: emptyList()
        return SmsParserImpl(templates)
    }
}
