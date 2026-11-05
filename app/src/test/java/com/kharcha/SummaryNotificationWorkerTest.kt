package com.kharcha

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.util.Calendar
import java.util.concurrent.TimeUnit
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
    fun dailyScheduleUsesTodayWhenConfiguredTimeIsStillAhead() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val expectedDelay = (21 - 14) * 60 * 60 * 1000L
        assertEquals(expectedDelay, SummaryNotificationWorker.millisUntilNextRun(21 * 60, now = now))
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

    @Test
    fun dailySummaryCalculatesTodayAndCurrentMonthSpendSeparately() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis

        // 1. Transaction today
        val todayTxn = Transaction(
            id = 1L,
            amount = 500_00L,
            type = TransactionType.Expense,
            dateTime = now - TimeUnit.HOURS.toMillis(2),
            isCounted = true,
        )

        // 2. Transaction earlier this month (Nov 5)
        val earlierMonthCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 5, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val earlierMonthTxn = Transaction(
            id = 2L,
            amount = 1500_00L,
            type = TransactionType.Expense,
            dateTime = earlierMonthCal.timeInMillis,
            isCounted = true,
        )

        // 3. Transaction last month (Oct 28) - should be excluded
        val lastMonthCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 28, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lastMonthTxn = Transaction(
            id = 3L,
            amount = 3000_00L,
            type = TransactionType.Expense,
            dateTime = lastMonthCal.timeInMillis,
            isCounted = true,
        )

        // 4. Ignored transaction today (transfer / isCounted = false)
        val transferTxn = Transaction(
            id = 4L,
            amount = 8000_00L,
            type = TransactionType.Expense,
            dateTime = now - TimeUnit.HOURS.toMillis(1),
            isCounted = false,
        )

        val txns = listOf(todayTxn, earlierMonthTxn, lastMonthTxn, transferTxn)

        val (todayTotal, monthTotal) = SummaryNotificationWorker.calculateSummarySpends(
            transactions = txns,
            now = now,
            weekly = false,
        )

        assertEquals(500_00L, todayTotal)
        assertEquals(2000_00L, monthTotal) // 500 + 1500
    }

    @Test
    fun weeklySummaryCalculatesPastSevenDaysSpend() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis

        // 3 days ago (within 7 days)
        val recentTxn = Transaction(
            id = 1L,
            amount = 1200_00L,
            type = TransactionType.Expense,
            dateTime = now - TimeUnit.DAYS.toMillis(3),
            isCounted = true,
        )

        // 10 days ago (outside 7 days)
        val olderTxn = Transaction(
            id = 2L,
            amount = 800_00L,
            type = TransactionType.Expense,
            dateTime = now - TimeUnit.DAYS.toMillis(10),
            isCounted = true,
        )

        val txns = listOf(recentTxn, olderTxn)

        val (weekTotal, _) = SummaryNotificationWorker.calculateSummarySpends(
            transactions = txns,
            now = now,
            weekly = true,
        )

        assertEquals(1200_00L, weekTotal)
    }
}
