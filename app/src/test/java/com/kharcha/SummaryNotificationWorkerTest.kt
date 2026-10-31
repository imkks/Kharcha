package com.kharcha

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryNotificationWorkerTest {
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
}
