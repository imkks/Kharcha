package com.kharcha.core.common.statement.pdf

import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.statement.PdfStatementParser
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SlicePdfParser : PdfStatementParser {

    companion object {
        private val markers = listOf("slice small finance bank", "help@slice.bank.in")

        private val txnStartPattern =
            Regex("""^(\d{1,2}) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) '(\d{2})(?:\s|$)""")

        private val tailPattern =
            Regex("""(\d{6,})\s+(-?)₹([\d,]+(?:\.\d{1,2})?)(?!\d)\s+₹([\d,]+(?:\.\d{1,2})?)(?!\d)\s*$""")

        private val accountNumberPattern = Regex("""A/C number\s+(\d{6,})""")
        private val ifscPattern = Regex("""^[A-Z]{4}0[A-Z0-9]{6}$""")

        private const val TABLE_HEADER = "DATE DETAILS REF NO. AMOUNT BALANCE"
        private const val FOOTER_PREFIX = "Generated on"

        private val pageMarkerPattern = Regex("""^\d+/\d+$""")
        private val rangeHeaderPattern =
            Regex("""^\d{1,2} [A-Z][a-z]{2} '\d{2} ?- ?\d{1,2} [A-Z][a-z]{2} '\d{2}$""")

        private const val BANK_NAME = "Slice"
        private const val MAX_AMOUNT_PAISE = 1_000_000_000_00L // 1 crore

        private val MONTHS = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
            "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
            "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
        )
    }

    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return markers.any { it in lower }
    }

    override fun parse(text: String): List<Transaction> {
        val accountLast4 = accountNumberPattern.find(text)
            ?.groupValues?.getOrNull(1)?.takeLast(4)

        val lines = text.lines()
        val headerIndex = lines.indexOfFirst { it.trim() == TABLE_HEADER }
        if (headerIndex == -1) return emptyList()

        val results = mutableListOf<Transaction>()
        var block: StringBuilder? = null

        fun tryFinalize() {
            val current = block?.toString()?.trim() ?: return
            parseBlock(current, accountLast4)?.let {
                results.add(it)
                block = null
            }
        }

        for (line in lines.drop(headerIndex + 1)) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (isPageFurniture(trimmed)) continue

            if (txnStartPattern.containsMatchIn(trimmed)) {
                block = StringBuilder(trimmed)
            } else {
                block?.append(' ')?.append(trimmed)
            }
            tryFinalize()
        }

        return results
    }

    private fun isPageFurniture(trimmed: String): Boolean =
        trimmed.startsWith(FOOTER_PREFIX) ||
            trimmed.startsWith("Need help?") ||
            trimmed == TABLE_HEADER ||
            pageMarkerPattern.matches(trimmed) ||
            rangeHeaderPattern.matches(trimmed)

    private fun parseBlock(block: String, accountLast4: String?): Transaction? {
        val start = txnStartPattern.find(block) ?: return null
        val tail = tailPattern.find(block) ?: return null
        if (tail.range.first <= start.range.last) return null

        val day = start.groupValues[1].toIntOrNull() ?: return null
        val month = MONTHS[start.groupValues[2].lowercase(Locale.ROOT)] ?: return null
        val year = 2000 + (start.groupValues[3].toIntOrNull() ?: return null)

        val reference = tail.groupValues[1]
        val isDebit = tail.groupValues[2] == "-"
        val amount = MoneyFormatter.parseMinorUnits(tail.groupValues[3].replace(",", "")) ?: return null
        if (amount <= 0 || amount > MAX_AMOUNT_PAISE) return null
        val balance = MoneyFormatter.parseMinorUnits(tail.groupValues[4].replace(",", ""))

        val details = block
            .substring(start.range.last + 1, tail.range.first)
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (details.isEmpty()) return null

        val (type, merchant, isTransfer) = classify(details, isDebit)
        val timestamp = istEpochMillis(year, month, day)
        val hash = generateHash(timestamp, amount, type, reference)

        return Transaction(
            amount = amount,
            type = type,
            merchantRaw = merchant,
            merchantNormalised = merchant,
            refNumber = reference,
            accountLast4 = accountLast4,
            balanceAfter = balance,
            smsBody = block,
            sender = "Slice PDF",
            dateTime = timestamp,
            bankName = BANK_NAME,
            currency = "INR",
            smsHash = hash,
            isReviewed = true,
            isCounted = !isTransfer,
            categorySource = if (isTransfer) "Transfer" else null,
        )
    }

    private data class Classification(val type: TransactionType, val merchant: String, val isTransfer: Boolean)

    private fun classify(details: String, isDebit: Boolean): Classification = when {
        details.startsWith("Interest Cr.", ignoreCase = true) ->
            Classification(TransactionType.Income, "slice interest", isTransfer = false)

        details.startsWith("UPI-", ignoreCase = true) -> {
            val type = if (isDebit) TransactionType.Expense else TransactionType.Income
            Classification(type, upiMerchant(details), isTransfer = false)
        }

        details.startsWith("Auto save to ", ignoreCase = true) ->
            Classification(TransactionType.Expense, stripPotName(details.drop("Auto save to ".length)), isTransfer = true)

        details.startsWith("Transfer from ", ignoreCase = true) && details.endsWith("atom", ignoreCase = true) ->
            Classification(TransactionType.Income, stripPotName(details.drop("Transfer from ".length)), isTransfer = true)

        details.startsWith("Transfer to ", ignoreCase = true) && details.endsWith("atom", ignoreCase = true) ->
            Classification(TransactionType.Expense, stripPotName(details.drop("Transfer to ".length)), isTransfer = true)

        details.equals("Round ups", ignoreCase = true) ->
            Classification(TransactionType.Expense, "Round ups", isTransfer = true)

        else ->
            Classification(if (isDebit) TransactionType.Expense else TransactionType.Income, details, isTransfer = false)
    }

    private fun stripPotName(raw: String): String {
        val trimmed = raw.trim()
        val stripped = if (trimmed.endsWith(" atom", ignoreCase = true)) {
            trimmed.dropLast(" atom".length).trim()
        } else {
            trimmed
        }
        return stripped.ifEmpty { trimmed }
    }

    private fun upiMerchant(details: String): String {
        val parts = details.split("-")
        val refIndex = parts.indexOfFirst { it.trim().length >= 10 && it.trim().all(Char::isDigit) }
        if (refIndex == -1 || refIndex == parts.lastIndex) return details
        val ifscIndex = parts.drop(refIndex + 1)
            .indexOfFirst { ifscPattern.matches(it.trim()) }
            .let { if (it == -1) -1 else it + refIndex + 1 }
        val merchantParts = if (ifscIndex > refIndex + 1) {
            parts.subList(refIndex + 1, ifscIndex)
        } else {
            parts.subList(refIndex + 1, minOf(refIndex + 2, parts.size))
        }
        return merchantParts.joinToString("-").trim().ifEmpty { details }
    }

    private fun istEpochMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(ist).apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }.timeInMillis

    private fun generateHash(timestamp: Long, amount: Long, type: TransactionType, uniquePart: String): String {
        val data = "$timestamp|$amount|${type.name}|$uniquePart"
        return MessageDigest.getInstance("MD5")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
