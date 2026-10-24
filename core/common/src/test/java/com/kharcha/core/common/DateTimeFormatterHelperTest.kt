package com.kharcha.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeFormatterHelperTest {
    private val zoneId = ZoneId.of("Asia/Kolkata")
    private val fixedToday = LocalDate.of(2026, 10, 25)

    @Test
    fun testFormatRelativeTime_today() {
        val todayDateTime = ZonedDateTime.of(2026, 10, 25, 12, 8, 0, 0, zoneId)
        val result = DateTimeFormatterHelper.formatRelativeTime(
            epochMillis = todayDateTime.toInstant().toEpochMilli(),
            zoneId = zoneId,
            today = fixedToday,
        )
        // Should be formatted as 12:08 PM or pm
        assertTrue(result.contains("12:08"))
    }

    @Test
    fun testFormatRelativeTime_yesterday() {
        val yesterdayDateTime = ZonedDateTime.of(2026, 10, 24, 15, 30, 0, 0, zoneId)
        val result = DateTimeFormatterHelper.formatRelativeTime(
            epochMillis = yesterdayDateTime.toInstant().toEpochMilli(),
            zoneId = zoneId,
            today = fixedToday,
        )
        assertEquals("Yesterday", result)
    }

    @Test
    fun testFormatRelativeTime_sameYear() {
        val earlierThisYear = ZonedDateTime.of(2026, 9, 27, 10, 0, 0, 0, zoneId)
        val result = DateTimeFormatterHelper.formatRelativeTime(
            epochMillis = earlierThisYear.toInstant().toEpochMilli(),
            zoneId = zoneId,
            today = fixedToday,
        )
        assertEquals("27 Sep", result)
    }

    @Test
    fun testFormatRelativeTime_pastYear() {
        val lastYear = ZonedDateTime.of(2025, 12, 15, 9, 0, 0, 0, zoneId)
        val result = DateTimeFormatterHelper.formatRelativeTime(
            epochMillis = lastYear.toInstant().toEpochMilli(),
            zoneId = zoneId,
            today = fixedToday,
        )
        assertEquals("15 Dec 2025", result)
    }

    @Test
    fun testFormatRelativeTime_zeroOrNegative() {
        assertEquals("", DateTimeFormatterHelper.formatRelativeTime(0L))
        assertEquals("", DateTimeFormatterHelper.formatRelativeTime(-100L))
    }
}
