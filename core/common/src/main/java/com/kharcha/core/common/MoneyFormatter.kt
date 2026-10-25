package com.kharcha.core.common

import java.util.Locale

object MoneyFormatter {
    fun formatMinorUnits(
        amountMinor: Long,
        showSign: Boolean = false,
        isCredit: Boolean = false,
    ): String {
        val absAmount = kotlin.math.abs(amountMinor)
        val rupees = absAmount / 100L
        val paise = absAmount % 100L

        // Indian numbering format for rupees part
        val formattedRupees = formatIndianNumber(rupees)
        val formattedPaise = String.format(Locale.ROOT, "%02d", paise)
        val formattedNumber = "$formattedRupees.$formattedPaise"

        val prefix =
            if (showSign) {
                if (isCredit) "+₹" else "-₹"
            } else {
                "₹"
            }

        return "$prefix$formattedNumber"
    }

    fun parseMinorUnits(amountStr: String): Long? {
        val trimmed = amountStr.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val parts = trimmed.split(".")
            val rupees = parts[0].toLongOrNull() ?: 0L
            val paise =
                if (parts.size > 1) {
                    val p = parts[1]
                    when {
                        p.isEmpty() -> 0L
                        p.length == 1 -> (p.toLongOrNull() ?: 0L) * 10L
                        p.length == 2 -> p.toLongOrNull() ?: 0L
                        else -> p.substring(0, 2).toLongOrNull() ?: 0L
                    }
                } else {
                    0L
                }
            (rupees * 100L) + paise
        } catch (_: Exception) {
            null
        }
    }

    fun formatCompactAmount(amountMinor: Long): String {
        val absPaise = kotlin.math.abs(amountMinor)
        if (absPaise == 0L) return "0"
        val rupees = absPaise / 100L
        return when {
            rupees >= 100_000L -> {
                val lValue = rupees / 1000L
                val wholeL = lValue / 100L
                val remL = lValue % 100L
                if (remL == 0L) {
                    "${wholeL}L"
                } else {
                    val formatted = String.format(Locale.ROOT, "%.2f", rupees / 100_000.0)
                        .trimEnd('0')
                        .trimEnd('.')
                    "${formatted}L"
                }
            }
            rupees >= 1_000L -> {
                val remHundred = (rupees % 1_000L) / 100L
                if (remHundred == 0L) {
                    "${rupees / 1_000L}K"
                } else {
                    String.format(Locale.ROOT, "%.1fK", rupees / 1_000.0)
                }
            }
            rupees in 500L..899L -> {
                String.format(Locale.ROOT, "%.1fK", rupees / 1_000.0)
            }
            else -> rupees.toString()
        }
    }

    private fun formatIndianNumber(number: Long): String {
        if (number < 1000L) return number.toString()
        val numStr = number.toString()
        val last3 = numStr.takeLast(3)
        val remaining = numStr.dropLast(3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i > 0) {
                sb.append(',')
            }
        }
        return "${sb.reverse()},$last3"
    }
}
