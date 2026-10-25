package com.kharcha.feature.transactions.ui

import com.kharcha.core.common.MoneyFormatter

data class ParticipantShare(
    val name: String,
    val amountMinor: Long,
)

object SplitExpenseCalculator {
    const val MIN_PARTICIPANTS = 2
    const val MAX_PARTICIPANTS = 10

    fun calculateShares(
        totalAmountMinor: Long,
        participantNames: List<String>,
    ): List<ParticipantShare> {
        val count = participantNames.size.coerceAtLeast(1)
        val baseShare = totalAmountMinor / count
        val remainder = totalAmountMinor % count

        return participantNames.mapIndexed { index, name ->
            val share = if (index < remainder) baseShare + 1L else baseShare
            val fallbackName = if (index == 0) "You" else "Person ${index + 1}"
            val resolvedName = name.trim().ifEmpty { fallbackName }
            ParticipantShare(name = resolvedName, amountMinor = share)
        }
    }

    fun formatShareMessage(
        merchant: String,
        totalAmountMinor: Long,
        shareAmountMinor: Long,
    ): String {
        val totalFormatted = MoneyFormatter.formatMinorUnits(totalAmountMinor)
        val shareFormatted = MoneyFormatter.formatMinorUnits(shareAmountMinor)
        return "Hey! Total bill at $merchant was $totalFormatted. Your share is $shareFormatted. " +
            "(Kharcha expense split)"
    }

    fun formatSplitSummary(shares: List<ParticipantShare>): String {
        val summary = shares.joinToString(separator = ", ") { share ->
            "${share.name}: ${MoneyFormatter.formatMinorUnits(share.amountMinor)}"
        }
        return "[Split: $summary]"
    }

    fun parseSplitSummary(note: String?): String? {
        if (note.isNullOrBlank()) return null
        val regex = Regex("\\[Split: (.*?)\\]")
        return regex.find(note)?.groupValues?.get(1)
    }

    fun isSplitActive(note: String?): Boolean {
        return parseSplitSummary(note) != null
    }

    fun appendOrUpdateSplit(note: String?, splitSummary: String): String {
        val current = note?.trim() ?: ""
        val regex = Regex("\\[Split: .*?\\]")
        return if (regex.containsMatchIn(current)) {
            current.replace(regex, splitSummary)
        } else if (current.isEmpty()) {
            splitSummary
        } else {
            "$current\n$splitSummary"
        }
    }
}
