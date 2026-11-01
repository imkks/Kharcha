package com.kharcha

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryNotificationWorkerTest {
    @Test
    fun exactAlarmFallbackIsRequiredOnAndroid12WhenAccessIsDenied() {
        assertEquals(false, exactAlarmAllowed(apiLevel = 31, canScheduleExactAlarms = false))
        assertEquals(true, exactAlarmAllowed(apiLevel = 31, canScheduleExactAlarms = true))
        assertEquals(true, exactAlarmAllowed(apiLevel = 30, canScheduleExactAlarms = false))
    }

    @Test
    fun dailyScheduleUsesTomorrowWhenConfiguredTimeHasPassed() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 23, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals(23 * 60 * 60 * 1000L, SummaryNotificationWorker.millisUntilNextRun(22 * 60, now = now))
    }

    @Test
    fun weeklyScheduleTargetsNextSundayAtConfiguredTime() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val expected = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals(expected.timeInMillis - now.timeInMillis, SummaryNotificationWorker.millisUntilNextRun(11 * 60, weekly = true, now = now))
    }

    @Test
    fun weeklyScheduleUsesTodayWhenItIsSundayAndConfiguredTimeIsStillAhead() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val expected = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
        }

        assertEquals(expected.timeInMillis - now.timeInMillis, SummaryNotificationWorker.millisUntilNextRun(11 * 60, weekly = true, now = now))
    }

    @Test
    fun weeklyScheduleMovesToNextSundayWhenConfiguredTimeHasPassed() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val expected = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
        }

        assertEquals(expected.timeInMillis - now.timeInMillis, SummaryNotificationWorker.millisUntilNextRun(11 * 60, weekly = true, now = now))
    }
}
