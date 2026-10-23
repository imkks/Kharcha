package com.kharcha.engine.parser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

object DateParser {
    /**
     * Parses a date string and optional time string into an epoch millisecond timestamp.
     *
     * @param dateStr the date string extracted from SMS
     * @param timeStr optional time string extracted from SMS
     * @param formatStrings list of pattern formats provided by the template. E.g., "dd-MM-yy"
     * @param receivedAt fallback SMS reception timestamp to resolve missing times/years
     */
    fun parse(
        dateStr: String?,
        timeStr: String?,
        formatStrings: List<String>,
        receivedAt: Long,
    ): Long {
        if (dateStr.isNullOrBlank()) {
            return receivedAt
        }

        val receivedDateTime = LocalDateTime.ofEpochSecond(receivedAt / 1000, 0, ZoneOffset.UTC)

        for (format in formatStrings) {
            try {
                val dateFormatter =
                    DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern(format)
                        .parseDefaulting(ChronoField.YEAR, receivedDateTime.year.toLong())
                        .toFormatter()

                val parsedDate = LocalDate.parse(dateStr.trim(), dateFormatter)

                var parsedTime = receivedDateTime.toLocalTime()
                if (!timeStr.isNullOrBlank()) {
                    val timeFormats = listOf("HH:mm:ss", "HH:mm", "hh:mm:ss a", "hh:mm a")
                    for (timeFormat in timeFormats) {
                        try {
                            val timeFormatter =
                                DateTimeFormatterBuilder()
                                    .parseCaseInsensitive()
                                    .appendPattern(timeFormat)
                                    .toFormatter()
                            parsedTime = LocalTime.parse(timeStr.trim(), timeFormatter)
                            break
                        } catch (e: DateTimeParseException) {
                            // Try next time format
                        }
                    }
                }

                return LocalDateTime.of(parsedDate, parsedTime).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (e: DateTimeParseException) {
                // Try next date format
            }
        }

        return receivedAt
    }
}
