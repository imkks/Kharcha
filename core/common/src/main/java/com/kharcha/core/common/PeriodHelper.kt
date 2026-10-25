package com.kharcha.core.common

import com.kharcha.core.model.TimePeriod
import com.kharcha.core.model.Transaction
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object PeriodHelper {

    /**
     * Returns the epoch millisecond start and end boundaries (inclusive) for a given [TimePeriod].
     * Returns null for [TimePeriod.ALL_TIME].
     */
    fun getPeriodDateRange(
        period: TimePeriod,
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Pair<Long, Long>? {
        return when (period) {
            TimePeriod.THIS_MONTH -> {
                val start =
                    referenceDate
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()
                val end =
                    referenceDate
                        .with(TemporalAdjusters.lastDayOfMonth())
                        .atTime(23, 59, 59, 999_000_000)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                Pair(start, end)
            }
            TimePeriod.LAST_MONTH -> {
                val lastMonth = referenceDate.minusMonths(1)
                val start =
                    lastMonth
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()
                val end =
                    lastMonth
                        .with(TemporalAdjusters.lastDayOfMonth())
                        .atTime(23, 59, 59, 999_000_000)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                Pair(start, end)
            }
            TimePeriod.ALL_TIME -> null
        }
    }

    /**
     * Filters a transaction list according to the specified [TimePeriod].
     */
    fun filterTransactionsByPeriod(
        transactions: List<Transaction>,
        period: TimePeriod,
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Transaction> {
        val range = getPeriodDateRange(period, referenceDate, zoneId) ?: return transactions
        return transactions.filter { it.dateTime in range.first..range.second }
    }
}
