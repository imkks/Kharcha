package com.kharcha.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurringTransactionTest {

    @Test
    fun dailyCadenceAdvancesOneDayAtATime() {
        val template = RecurringTransaction(
            merchantName = "Daily Milk",
            amountMinor = 5000L,
            frequency = RecurringFrequency.DAILY,
        )
        val start = LocalDate.of(2026, 3, 1)
        val next = template.nextDueAfter(start)
        assertEquals(LocalDate.of(2026, 3, 2), next)
    }

    @Test
    fun weeklyCadenceAdvancesToChosenDayOfWeek() {
        val template = RecurringTransaction(
            merchantName = "Weekly Cleaner",
            amountMinor = 150000L,
            frequency = RecurringFrequency.WEEKLY,
            dayOfWeek = DayOfWeek.SATURDAY.value,
        )
        // 2026-03-02 is Monday
        val monday = LocalDate.of(2026, 3, 2)
        val saturday = template.nextDueAfter(monday)
        assertEquals(LocalDate.of(2026, 3, 7), saturday)
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)

        // Advancing from Saturday lands on next Saturday
        val nextSaturday = template.nextDueAfter(saturday)
        assertEquals(LocalDate.of(2026, 3, 14), nextSaturday)
    }

    @Test
    fun monthlyCadenceClampsToMonthEndAndPreventsDrift() {
        val template = RecurringTransaction(
            merchantName = "Rent",
            amountMinor = 2500000L,
            frequency = RecurringFrequency.MONTHLY,
            dayOfMonth = 31,
        )
        val jan31 = LocalDate.of(2026, 1, 31)
        val febEnd = template.nextDueAfter(jan31)
        assertEquals(LocalDate.of(2026, 2, 28), febEnd)

        // After Feb, advancing with dayOfMonth=31 restores 31st for March!
        val mar31 = template.nextDueAfter(febEnd)
        assertEquals(LocalDate.of(2026, 3, 31), mar31)
    }

    @Test
    fun computeInitialDueDateRespectsPastAndFutureDays() {
        val today = LocalDate.of(2026, 3, 15)

        // Day of month in future
        val future = RecurringTransaction(
            merchantName = "Internet Bill",
            amountMinor = 99900L,
            frequency = RecurringFrequency.MONTHLY,
            dayOfMonth = 25,
        )
        assertEquals(LocalDate.of(2026, 3, 25), future.computeInitialDueDate(today))

        // Day of month in past: moves to next month
        val past = RecurringTransaction(
            merchantName = "Netflix",
            amountMinor = 64900L,
            frequency = RecurringFrequency.MONTHLY,
            dayOfMonth = 5,
        )
        assertEquals(LocalDate.of(2026, 4, 5), past.computeInitialDueDate(today))

        // Day of month is today: stays today
        val currentDay = RecurringTransaction(
            merchantName = "Spotify",
            amountMinor = 11900L,
            frequency = RecurringFrequency.MONTHLY,
            dayOfMonth = 15,
        )
        assertEquals(LocalDate.of(2026, 3, 15), currentDay.computeInitialDueDate(today))
    }
}
