package com.kharcha.engine.parser

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import kotlinx.serialization.Serializable

@Serializable
data class ParserTemplate(
    val id: String,
    val bank: String,
    val senderPatterns: List<String>,
    val direction: Direction,
    val channel: Channel,
    val regex: String,
    val dateFormats: List<String>,
    val confidence: Float = 0.95f,
)
