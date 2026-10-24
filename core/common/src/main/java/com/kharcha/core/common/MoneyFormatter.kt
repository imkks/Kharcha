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
