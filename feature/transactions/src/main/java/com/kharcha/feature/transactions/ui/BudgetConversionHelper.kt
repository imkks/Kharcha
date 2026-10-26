package com.kharcha.feature.transactions.ui

import com.kharcha.core.common.MoneyFormatter

object BudgetConversionHelper {
    val PRESET_BUDGETS_MINOR: List<Long> =
        listOf(
            5_000_00L,
            10_000_00L,
            20_000_00L,
            30_000_00L,
        )

    fun parseInputToMinor(input: String): Long {
        val sanitized = input.replace(",", "").replace("₹", "").trim()
        if (sanitized.isEmpty()) return 0L
        return MoneyFormatter.parseMinorUnits(sanitized) ?: 0L
    }

    fun formatMinorToInput(minor: Long): String {
        if (minor <= 0L) return ""
        val rupees = minor / 100L
        return rupees.toString()
    }
}
