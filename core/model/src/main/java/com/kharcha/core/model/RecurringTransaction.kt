package com.kharcha.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
}

data class RecurringTransaction(
    val id: Long = 0L,
    val uid: String = java.util.UUID.randomUUID().toString(),
    val merchantName: String = "",
    val amountMinor: Long = 0L,
    val currency: String = "INR",
    val category: String = "Others",
    val type: TransactionType = TransactionType.Expense,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val nextDueDateEpoch: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val accountLast4: String? = null,
    val bankName: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /**
     * Calculates the next due date strictly after [fromDate].
     * Anchored to dayOfMonth or dayOfWeek to prevent drift across short months.
     */
    fun nextDueAfter(fromDate: LocalDate): LocalDate = when (frequency) {
        RecurringFrequency.DAILY -> fromDate.plusDays(1)
        RecurringFrequency.WEEKLY -> {
            val targetDow = dayOfWeek?.let { DayOfWeek.of(it.coerceIn(1, 7)) } ?: fromDate.dayOfWeek
            fromDate.with(TemporalAdjusters.next(targetDow))
        }
        RecurringFrequency.MONTHLY -> {
            val ym = YearMonth.from(fromDate).plusMonths(1)
            val dom = (dayOfMonth ?: fromDate.dayOfMonth).coerceIn(1, ym.lengthOfMonth())
            ym.atDay(dom)
        }
    }

    /**
     * Resolves the first due date on or after [today].
     */
    fun computeInitialDueDate(today: LocalDate = LocalDate.now()): LocalDate = when (frequency) {
        RecurringFrequency.DAILY -> today
        RecurringFrequency.WEEKLY -> {
            val targetDow = dayOfWeek?.let { DayOfWeek.of(it.coerceIn(1, 7)) } ?: today.dayOfWeek
            var d = today
            while (d.dayOfWeek != targetDow) {
                d = d.plusDays(1)
            }
            d
        }
        RecurringFrequency.MONTHLY -> {
            val dom = (dayOfMonth ?: today.dayOfMonth).coerceIn(1, today.lengthOfMonth())
            val candidate = today.withDayOfMonth(dom)
            if (candidate.isBefore(today)) nextDueAfter(candidate) else candidate
        }
    }
}
