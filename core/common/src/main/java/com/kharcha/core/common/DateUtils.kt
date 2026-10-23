package com.kharcha.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    fun getStartOfDay(timestamp: Long): Long {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getEndOfDay(timestamp: Long): Long {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant().toEpochMilli()
    }

    fun getStartOfMonth(
        monthOffset: Int,
        customStartDay: Int = 1,
    ): Long {
        val today = LocalDate.now(ZoneId.systemDefault())
        var targetMonth = today.plusMonths(monthOffset.toLong())

        if (today.dayOfMonth < customStartDay) {
            targetMonth = targetMonth.minusMonths(1)
        }

        val actualStartDay =
            if (customStartDay > targetMonth.lengthOfMonth()) {
                targetMonth.lengthOfMonth()
            } else {
                customStartDay
            }

        return targetMonth.withDayOfMonth(actualStartDay)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun getEndOfMonth(
        monthOffset: Int,
        customStartDay: Int = 1,
    ): Long {
        val nextMonthStart = getStartOfMonth(monthOffset + 1, customStartDay)
        return nextMonthStart - 1
    }

    fun formatMonthYear(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
}
