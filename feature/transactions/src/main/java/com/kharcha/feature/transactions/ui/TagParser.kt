package com.kharcha.feature.transactions.ui

import com.kharcha.core.model.Channel

object TagParser {
    val PRESET_TAGS = listOf(
        "#Online",
        "#Offline",
        "#Vacation",
        "#TaxDeductible",
        "#Reimbursable",
        "#Party",
    )

    fun extractTags(note: String?, channel: Channel? = null): List<String> {
        val extracted = mutableListOf<String>()
        if (!note.isNullOrBlank()) {
            val regex = Regex("#[A-Za-z0-9_]+")
            extracted.addAll(regex.findAll(note).map { it.value })
        }
        if (channel != null && isOnlineChannel(channel)) {
            if (!extracted.any { it.equals("#Online", ignoreCase = true) }) {
                extracted.add(0, "#Online")
            }
        }
        return extracted.distinct()
    }

    fun isOnlineChannel(channel: Channel): Boolean {
        return channel == Channel.UPI ||
            channel == Channel.CARD ||
            channel == Channel.NEFT ||
            channel == Channel.IMPS ||
            channel == Channel.WALLET
    }

    fun formatTag(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val withoutHash = trimmed.removePrefix("#").trim()
        if (withoutHash.isEmpty()) return ""
        return "#$withoutHash"
    }

    fun addTag(note: String?, rawTag: String): String {
        val tag = formatTag(rawTag)
        if (tag.isEmpty()) return note ?: ""
        val current = note?.trim() ?: ""
        val currentTags = extractTags(current)
        if (currentTags.any { it.equals(tag, ignoreCase = true) }) {
            return current
        }
        return if (current.isEmpty()) {
            tag
        } else {
            "$current $tag"
        }
    }

    fun removeTag(note: String?, rawTag: String): String {
        val tag = formatTag(rawTag)
        if (note.isNullOrBlank() || tag.isEmpty()) return note ?: ""
        val regex = Regex("(?<=\\s|^)${Regex.escape(tag)}(?=\\s|$)")
        return note.replace(regex, "").replace(Regex("\\s+"), " ").trim()
    }
}
