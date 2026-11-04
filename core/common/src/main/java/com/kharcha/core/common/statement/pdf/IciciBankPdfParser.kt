package com.kharcha.core.common.statement.pdf

import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.statement.PdfStatementParser
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal const val ICICI_PDF_SENDER = "ICICI PDF"

class IciciBankPdfParser : PdfStatementParser {

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return "icici" in lower && "particulars" in lower && "withdrawals" in lower && "deposits" in lower
    }

    override fun parse(text: String): List<Transaction> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val rows = findRows(lines)
        var previousBalance = openingBalance(lines)
        return rows.mapIndexedNotNull { i, row ->
            val description = descriptionFor(rows, i, lines)
            val type = when {
                previousBalance != null && row.balance == (previousBalance!! - row.amount) -> TransactionType.Expense
                previousBalance != null && row.balance == (previousBalance!! + row.amount) -> TransactionType.Income
                else -> guessType(description)
            }
            previousBalance = row.balance

            val merchant = merchantFrom(description) ?: "Unknown"
            val ref = REFERENCE.findAll(description.joinToString(" ")).lastOrNull()?.value
            val timestamp = row.date.atTime(LocalTime.NOON).atZone(ist).toInstant().toEpochMilli()
            val hash = generateHash(timestamp, row.amount, type, row.date.format(dateFormatter) + "|" + row.balance)

            val body = (listOf("${row.date.format(dateFormatter)} · balance ₹${row.balance / 100}.${"%02d".format(Locale.ROOT, kotlin.math.abs(row.balance) % 100)}") + description)
                .joinToString("\n")

            Transaction(
                amount = row.amount,
                type = type,
                merchantRaw = merchant,
                merchantNormalised = merchant,
                refNumber = ref,
                accountLast4 = null,
                balanceAfter = row.balance,
                smsBody = body,
                sender = ICICI_PDF_SENDER,
                dateTime = timestamp,
                bankName = "ICICI Bank",
                currency = "INR",
                smsHash = hash,
                isReviewed = true,
                isCounted = true,
            )
        }
    }

    private data class Row(val date: LocalDate, val amount: Long, val balance: Long, val dateLine: Int, val balanceLine: Int)

    private fun findRows(lines: List<String>): List<Row> {
        val rows = mutableListOf<Row>()
        var i = 0
        while (i < lines.size) {
            val date = parseDate(lines[i])
            if (date == null) { i++; continue }

            val amounts = (i + 1 until minOf(i + 1 + REACH, lines.size))
                .filter { AMOUNT.matches(lines[it]) && parseDate(lines[it]) == null }
                .take(2)
            if (amounts.size == 2 && lines.subList(i + 1, amounts[1]).none { parseDate(it) != null }) {
                val amt = MoneyFormatter.parseMinorUnits(lines[amounts[0]]) ?: 0L
                val bal = MoneyFormatter.parseMinorUnits(lines[amounts[1]]) ?: 0L
                rows += Row(date, amt, bal, i, amounts[1])
                i = amounts[1] + 1
            } else {
                i++
            }
        }
        return rows
    }

    private fun descriptionFor(rows: List<Row>, index: Int, lines: List<String>): List<String> {
        val row = rows[index]
        val leadStart = if (index == 0) headerEnd(lines, row.dateLine) else leadingStartAfter(rows[index - 1], row, lines)
        val leading = lines.subList(leadStart, row.dateLine)
        val middle = lines.subList(row.dateLine + 1, row.balanceLine).filterNot { AMOUNT.matches(it) }
        val trailingEnd = if (index + 1 < rows.size) leadingStartAfter(row, rows[index + 1], lines) else lines.size
        val trailing = lines.subList(row.balanceLine + 1, trailingEnd).takeWhile { !isNoise(it) }
        return (leading + middle + trailing).filterNot { isNoise(it) }
    }

    private fun leadingStartAfter(previous: Row, next: Row, lines: List<String>): Int {
        val from = previous.balanceLine + 1
        return (from until next.dateLine).firstOrNull { idx -> STARTS.any { lines[idx].startsWith(it, ignoreCase = true) } }
            ?: next.dateLine
    }

    private fun headerEnd(lines: List<String>, firstDateLine: Int): Int {
        val header = (0 until firstDateLine).lastOrNull { lines[it].equals("BALANCE", ignoreCase = true) }
        val start = (header ?: -1) + 1
        return (start until firstDateLine).firstOrNull { idx -> STARTS.any { lines[idx].startsWith(it, ignoreCase = true) } }
            ?: firstDateLine
    }

    private fun openingBalance(lines: List<String>): Long? {
        val at = lines.indexOfFirst { OPENING.containsMatchIn(it) }
        if (at < 0) return null
        return (at until minOf(at + REACH, lines.size)).firstNotNullOfOrNull { idx ->
            AMOUNT_IN_LINE.findAll(lines[idx]).lastOrNull()?.value?.let { MoneyFormatter.parseMinorUnits(it) }
        }
    }

    private fun merchantFrom(description: List<String>): String? {
        description.firstNotNullOfOrNull { CHANNEL_PAYEE.find(it)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotEmpty) }
            ?.let { return it }
        if (description.any { it.startsWith("Int on", ignoreCase = true) || it.startsWith("Int.Pd", ignoreCase = true) }) {
            return "Interest"
        }
        return description.firstOrNull()?.take(MAX_MERCHANT)
    }

    private fun guessType(description: List<String>): TransactionType {
        val text = description.joinToString(" ").lowercase(Locale.ROOT)
        return if (INCOME_HINTS.any { it in text }) TransactionType.Income else TransactionType.Expense
    }

    private fun isNoise(line: String) = NOISE.any { it.containsMatchIn(line) }

    private fun parseDate(line: String): LocalDate? =
        if (DATE.matches(line)) runCatching { LocalDate.parse(line, dateFormatter) }.getOrNull() else null

    private fun generateHash(timestamp: Long, amount: Long, type: TransactionType, uniquePart: String): String {
        val data = "$timestamp|$amount|${type.name}|$uniquePart"
        return MessageDigest.getInstance("MD5")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val ist: ZoneId = ZoneId.of("Asia/Kolkata")
        val DATE = Regex("""\d{2}-\d{2}-\d{4}""")
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val AMOUNT = Regex("""[\d,]+\.\d{2}""")
        val AMOUNT_IN_LINE = Regex("""\d[\d,]*\.\d{2}""")
        val REFERENCE = Regex("""(?<!\d)\d{12}(?!\d)""")
        val CHANNEL_PAYEE = Regex("""^(?:UPI|VIN|VSI|BIL|IMPS|NEFT|RTGS)/([^/]+)""", RegexOption.IGNORE_CASE)
        val OPENING = Regex("""\bB/F\b|opening balance""", RegexOption.IGNORE_CASE)
        val STARTS = listOf("UPI/", "VIN/", "VSI/", "BIL/", "IMPS/", "NEFT", "RTGS", "MMT/", "ACH/", "NFS/", "ATM/", "CMS/", "INF/", "Int on", "Int.Pd", "CLG/", "BY CASH", "TO CASH")
        val INCOME_HINTS = listOf("int on", "int.pd", "salary", "refund", "reversal", "cashback", "by cash")
        val NOISE = listOf(Regex("""^page \d+""", RegexOption.IGNORE_CASE), Regex("""^(DATE|MODE\**|PARTICULARS|DEPOSITS|WITHDRAWALS|BALANCE)$"""))
        const val REACH = 6
        const val MAX_MERCHANT = 40
    }
}
