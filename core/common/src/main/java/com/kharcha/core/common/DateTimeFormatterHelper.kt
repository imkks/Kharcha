package com.kharcha.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats epoch timestamps into human-readable relative dates and times
 * for transaction rows, headers, and detail views.
 */
object DateTimeFormatterHelper {
    private val timeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val sameYearDateFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val fullDateFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val detailDateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm a", Locale.getDefault())

    fun formatRelativeTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
    ): String {
        if (epochMillis <= 0L) return ""
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val date = zonedDateTime.toLocalDate()

        return when {
            date == today -> zonedDateTime.format(timeFormatter)
            date == today.minusDays(1) -> "Yesterday"
            date.year == today.year -> zonedDateTime.format(sameYearDateFormatter)
            else -> zonedDateTime.format(fullDateFormatter)
        }
    }

    fun formatDetailDateTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        if (epochMillis <= 0L) return ""
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return zonedDateTime.format(detailDateTimeFormatter)
    }
}
