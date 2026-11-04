package com.kharcha.core.common.statement

import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.opencsv.CSVReaderBuilder
import com.opencsv.RFC4180ParserBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.math.BigDecimal
import java.security.MessageDigest
import java.text.ParsePosition
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * How a bank's spreadsheet statement maps onto transactions: which row
 * holds the column names and which column is which.
 */
@Serializable
data class StatementColumnMapping(
    val headerRow: Int = 0,
    val dateColumn: Int = NONE,
    /** One of [StatementTableParser.DATE_FORMATS]; null = detect from the column. */
    val dateFormat: String? = null,
    val descriptionColumn: Int = NONE,
    /** Single signed amount (with an optional Dr/Cr [typeColumn])… */
    val amountColumn: Int = NONE,
    val typeColumn: Int = NONE,
    /** …or separate debit and credit columns. */
    val debitColumn: Int = NONE,
    val creditColumn: Int = NONE,
    val balanceColumn: Int = NONE,
    /** Cheque/UTR/reference number; a UPI/IMPS/NEFT reference in the narration is used when absent. */
    val referenceColumn: Int = NONE,
    /** The account the statement belongs to (bank name + last 4), if linked. */
    val accountBankName: String? = null,
    val accountLast4: String? = null,
) {
    val usesDebitCredit: Boolean get() = debitColumn != NONE || creditColumn != NONE
    val isComplete: Boolean get() = dateColumn != NONE && (amountColumn != NONE || usesDebitCredit)

    companion object {
        const val NONE = -1
    }
}

/** Account the imported rows are booked on; [currency] tags every amount. */
data class StatementAccount(
    val bankName: String,
    val last4: String?,
    val currency: String = "INR",
)

/** A cell's parsed amount in minor units (paise); [negative] is null when nothing (sign, Dr/Cr, brackets) marked it. */
data class CellAmount(val valueMinor: Long, val negative: Boolean?)

data class TableParseResult(val transactions: List<Transaction>, val skippedRows: Int)

/** Pure: spreadsheet rows + a [StatementColumnMapping] → transactions. */
object StatementTableParser {

    /** Formats offered in the picker, in auto-detect priority (day-first before month-first). */
    val DATE_FORMATS = listOf(
        "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy", "dd/MM/yy", "dd-MM-yy",
        "dd-MMM-yy", "dd-MMM-yyyy", "dd MMM yyyy", "dd MMM yy", "dd/MMM/yyyy",
        "yyyy-MM-dd", "yyyy/MM/dd", "MM/dd/yyyy", "MMM dd, yyyy",
    )

    /** Pseudo-format: the Date column holds Excel day numbers. */
    const val EXCEL_SERIAL = "excel-serial"
    private val YEARS = 1990..2100

    const val STATEMENT_SENDER = "STATEMENT"
    private const val HEADER_SCAN_ROWS = 50
    private const val MAX_MERCHANT = 60
    private val REFERENCE = Regex("""(?<!\d)\d{12}(?!\d)""")
    private val CHANNEL_REFERENCE = Regex(
        """^(?:UPI|IMPS|NEFT|RTGS)[/\-: ]+([A-Za-z0-9]*\d[A-Za-z0-9]{5,})""",
        RegexOption.IGNORE_CASE,
    )
    private val REFERENCE_TOKEN = Regex("""[A-Za-z0-9]{6,32}""")
    private val DIRECTION_HEADERS = listOf("dr/cr", "cr/dr", "dr / cr", "cr / dr", "debit/credit", "credit/debit")
    private val WHITESPACE = Regex("""\s+""")
    private val CHANNELS = setOf("UPI", "IMPS", "NEFT", "RTGS", "POS", "ACH", "NACH", "BIL", "VIN", "MMT", "CMS", "INF")
    private val IFSC = Regex("""[A-Z]{4}0[A-Z0-9]{6}""")
    private val LEADING_MINUS = Regex("""^[^\d]*-""")
    private val PLAIN_NUMBER = Regex("""[+-]?\d{1,20}(\.\d{1,20})?([eE][+-]?\d{1,2})?""")
    private val EXPONENT = Regex("""\d[eE][+-]?\d""")
    private const val MAX_INTEGER_DIGITS = 13
    private val json = Json { ignoreUnknownKeys = true }

    // ── Reading ──────────────────────────────────────────────────────────────

    class LegacyXlsException : IllegalArgumentException("Legacy .xls isn't supported. Save it as .xlsx or .csv and import that.")
    class TooManyRowsException : IllegalArgumentException("This statement has more than ${XlsxReader.MAX_ROWS} rows. Export a shorter period and import that.")

    /** .xlsx (zip) or CSV text, told apart by content, not by file name. */
    fun readTable(bytes: ByteArray): List<List<String>> = when {
        bytes.startsWith(0x50, 0x4B, 0x03, 0x04) -> XlsxReader.readRows(ByteArrayInputStream(bytes))
        bytes.startsWith(0xD0, 0xCF, 0x11, 0xE0) -> throw LegacyXlsException()
        else -> readCsv(String(bytes, Charsets.UTF_8).removePrefix("\uFEFF"))
    }

    fun readCsv(text: String): List<List<String>> {
        val separator = detectSeparator(text.lineSequence().take(20).joinToString("\n"))
        val reader = CSVReaderBuilder(StringReader(text))
            .withCSVParser(RFC4180ParserBuilder().withSeparator(separator).build())
            .build()
        val rows = reader.use { r ->
            generateSequence { r.readNext() }.take(XlsxReader.MAX_ROWS + 1).map { it.toList() }.toList()
        }
        if (rows.size > XlsxReader.MAX_ROWS) throw TooManyRowsException()
        return rows
    }

    /** Comma, semicolon or tab: whichever appears most outside quoted fields. */
    internal fun detectSeparator(sample: String): Char {
        val counts = IntArray(3)
        var quoted = false
        for (ch in sample) {
            when {
                ch == '"' -> quoted = !quoted
                quoted -> Unit
                ch == ',' -> counts[0]++
                ch == ';' -> counts[1]++
                ch == '\t' -> counts[2]++
            }
        }
        return ",;\t"[counts.indices.maxByOrNull { counts[it] } ?: 0]
    }

    private fun ByteArray.startsWith(vararg magic: Int) =
        size >= magic.size && magic.indices.all { this[it] == magic[it].toByte() }

    // ── Header + mapping ────────────────────────────────────────────────────

    fun signature(row: List<String>): String =
        row.map { it.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ") }
            .dropLastWhile { it.isEmpty() }
            .joinToString("|")

    fun findSavedMapping(rows: List<List<String>>, saved: Map<String, StatementColumnMapping>): StatementColumnMapping? {
        if (saved.isEmpty()) return null
        for (i in 0 until minOf(HEADER_SCAN_ROWS, rows.size)) {
            val sig = signature(rows[i])
            if (sig.isNotEmpty()) saved[sig]?.let { return it.copy(headerRow = i) }
        }
        return null
    }

    fun preamble(rows: List<List<String>>, headerRow: Int): String =
        rows.take(headerRow).joinToString("\n") { row -> row.filter { it.isNotBlank() }.joinToString(" ") }

    fun namesAccount(preamble: String, last4: String): Boolean {
        if (last4.length < 3 || last4.any { !it.isDigit() }) return false
        val digits = Regex.escape(last4)
        return Regex("""(?<![A-Za-z])[Xx*•][Xx*• ]*+$digits(?!\d)""").containsMatchIn(preamble) ||
            Regex("""(?i)(?:a/c|acc(?:oun)?t|acct|card)(?: ?(?:no|number|num)\b| ending(?: in)?)?\.?[ :#-]*[Xx*•.\d -]*?$digits(?!\d)""")
                .containsMatchIn(preamble)
    }

    fun guessHeaderRow(rows: List<List<String>>): Int {
        val scan = rows.take(HEADER_SCAN_ROWS)
        scan.indexOfFirst { row ->
            row.count { it.isNotBlank() } >= 3 && row.any { "date" in it.lowercase(Locale.ROOT) }
        }.takeIf { it >= 0 }?.let { return it }
        return scan.indices.maxByOrNull { scan[it].count { c -> c.isNotBlank() } } ?: 0
    }

    fun guessMapping(rows: List<List<String>>, headerRow: Int = guessHeaderRow(rows)): StatementColumnMapping {
        val header = rows.getOrNull(headerRow).orEmpty().map { it.trim().lowercase(Locale.ROOT) }
        fun find(vararg keys: String, exact: Set<String> = emptySet()): Int =
            keys.firstNotNullOfOrNull { k -> header.indexOfFirst { k in it }.takeIf { it >= 0 } }
                ?: header.indexOfFirst { it in exact }.takeIf { it >= 0 }
                ?: StatementColumnMapping.NONE

        val debit = find("debit", "withdrawal", "paid out", exact = setOf("dr"))
        val credit = find("credit", "deposit", "paid in", exact = setOf("cr"))
        val separate = debit != StatementColumnMapping.NONE && credit != StatementColumnMapping.NONE && debit != credit
        return StatementColumnMapping(
            headerRow = headerRow,
            dateColumn = find("transaction date", "txn date", "tran date", "date"),
            descriptionColumn = find("narration", "description", "particulars", "details", "remarks"),
            amountColumn = if (separate) StatementColumnMapping.NONE else find("amount", "amt"),
            typeColumn = if (separate) StatementColumnMapping.NONE else guessTypeColumn(rows, headerRow, header),
            debitColumn = if (separate) debit else StatementColumnMapping.NONE,
            creditColumn = if (separate) credit else StatementColumnMapping.NONE,
            balanceColumn = find("balance"),
            referenceColumn = find("utr", "ref no", "ref.", "reference", "chq", "cheque", "ref"),
        )
    }

    private fun guessTypeColumn(rows: List<List<String>>, headerRow: Int, header: List<String>): Int {
        header.indexOfFirst { h -> DIRECTION_HEADERS.any { it in h } }.takeIf { it >= 0 }?.let { return it }
        val typed = header.indexOfFirst { "type" in it }.takeIf { it >= 0 } ?: return StatementColumnMapping.NONE
        val cells = rows.drop(headerRow + 1).map { it.cell(typed) }.filter { it.isNotBlank() }.take(200)
        return if (cells.isNotEmpty() && cells.count { directionWord(it) != null } * 10 >= cells.size * 9) {
            typed
        } else {
            StatementColumnMapping.NONE
        }
    }

    internal fun directionWord(raw: String): Boolean? =
        when (raw.trim().lowercase(Locale.ROOT).replace(".", "")) {
            "dr", "d", "debit", "withdrawal" -> true
            "cr", "c", "credit", "deposit" -> false
            else -> null
        }

    fun encodeMappings(mappings: Map<String, StatementColumnMapping>): String = json.encodeToString(mappings)

    fun decodeMappings(raw: String?): Map<String, StatementColumnMapping> =
        raw?.let { runCatching { json.decodeFromString<Map<String, StatementColumnMapping>>(it) }.getOrNull() }.orEmpty()

    // ── Cells ────────────────────────────────────────────────────────────────

    fun parseAmount(raw: String?): CellAmount? {
        var s = raw?.trim()?.replace('\u00A0', ' ').orEmpty()
        if (s.isEmpty()) return null
        if (PLAIN_NUMBER.matches(s)) {
            val n = BigDecimal(s)
            if (n.precision() - n.scale() > MAX_INTEGER_DIGITS) return null
            val paise = n.multiply(BigDecimal(100)).toLong()
            return CellAmount(kotlin.math.abs(paise), if (n.signum() < 0) true else if (s.startsWith("+")) false else null)
        }
        if (EXPONENT.containsMatchIn(s)) return null
        var negative: Boolean? = null
        val lower = s.lowercase(Locale.ROOT).trimEnd('.')
        when {
            lower.endsWith("dr") -> { negative = true; s = s.substring(0, lower.length - 2) }
            lower.endsWith("cr") -> { negative = false; s = s.substring(0, lower.length - 2) }
        }
        s = s.trim()
        if (s.startsWith("(") && s.endsWith(")")) negative = true
        if (LEADING_MINUS.containsMatchIn(s) || s.endsWith("-")) negative = true
        if (s.startsWith("+")) negative = false

        var digits = s.dropWhile { !it.isDigit() }.dropLastWhile { !it.isDigit() }
            .filter { it.isDigit() || it == '.' || it == ',' }
        if (digits.none { it.isDigit() }) return null
        val lastComma = digits.lastIndexOf(',')
        val lastDot = digits.lastIndexOf('.')
        digits = if (lastComma > lastDot && digits.length - lastComma - 1 in 1..2) {
            digits.replace(".", "").replace(',', '.')
        } else {
            digits.replace(",", "")
        }
        val paise = MoneyFormatter.parseMinorUnits(digits) ?: return null
        return CellAmount(kotlin.math.abs(paise), negative)
    }

    private val formatterCache = ConcurrentHashMap<String, DateTimeFormatter>()

    private fun formatter(pattern: String): DateTimeFormatter = formatterCache.getOrPut(pattern) {
        val lenient = pattern.replace("dd", "d").replace(Regex("""(?<!M)MM(?!M)"""), "M")
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(lenient).toFormatter(Locale.ENGLISH)
    }

    fun parseDate(raw: String?, pattern: String?): LocalDate? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || pattern == null) return null
        if (pattern == EXCEL_SERIAL) {
            val serial = text.toDoubleOrNull()?.takeIf { it in SERIAL_RANGE } ?: return null
            return EXCEL_EPOCH.plusDays(serial.toLong())
        }
        val position = ParsePosition(0)
        val parsed = runCatching { formatter(pattern).parse(text, position) }.getOrNull() ?: return null
        if (position.errorIndex >= 0) return null
        val rest = text.substring(position.index)
        if (rest.isNotEmpty() && !rest[0].isWhitespace() && rest[0] != 'T' && rest[0] != ',') return null
        return runCatching { LocalDate.from(parsed) }.getOrNull()?.takeIf { it.year in YEARS }
    }

    fun detectDateFormat(cells: List<String>): String? {
        val filled = cells.filter { it.isNotBlank() }.take(200)
        if (filled.isEmpty()) return null
        if (filled.count { it.trim().toDoubleOrNull() != null } * 2 > filled.size) return EXCEL_SERIAL
        val sample = filled.filter { it.trim().toDoubleOrNull() == null }
        return DATE_FORMATS
            .map { f -> f to sample.count { parseDate(it, f) != null } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }?.first
    }

    fun effectiveDateFormat(rows: List<List<String>>, mapping: StatementColumnMapping): String? =
        mapping.dateFormat ?: detectDateFormat(rows.drop(mapping.headerRow + 1).map { it.cell(mapping.dateColumn) })

    // ── Rows → transactions ─────────────────────────────────────────────────

    fun parse(rows: List<List<String>>, mapping: StatementColumnMapping, account: StatementAccount): TableParseResult {
        val format = effectiveDateFormat(rows, mapping)
        val seen = HashMap<String, Int>()
        var skipped = 0
        val parsed = rows.drop(mapping.headerRow + 1).mapNotNull { row ->
            if (row.all { it.isBlank() }) return@mapNotNull null
            val txn = parseRow(row, mapping, format, account, seen)
            if (txn == null) skipped++
            txn
        }
        val dates = parsed.map { LocalDate.ofEpochDay(it.dateTime / (24 * 60 * 60 * 1000L)) }
        val isolated = (if (dates.size < 3) emptySet() else isolatedDates(dates.sorted()))
            .takeIf { set -> dates.count { it in set } * 4 < dates.size }
            .orEmpty()
        val transactions = parsed.filterIndexed { i, _ -> dates[i] !in isolated }
        return TableParseResult(transactions, skipped + parsed.size - transactions.size)
    }

    private fun isolatedDates(sorted: List<LocalDate>): Set<LocalDate> {
        fun farApart(a: LocalDate?, b: LocalDate?) = a == null || b == null || ChronoUnit.DAYS.between(a, b) > ISOLATION_DAYS
        return sorted.indices
            .filter { i -> farApart(sorted.getOrNull(i - 1), sorted[i]) && farApart(sorted[i], sorted.getOrNull(i + 1)) }
            .mapTo(HashSet()) { sorted[it] }
    }

    private fun parseRow(
        row: List<String>,
        mapping: StatementColumnMapping,
        format: String?,
        account: StatementAccount,
        seen: MutableMap<String, Int>,
    ): Transaction? {
        val date = parseDate(row.cell(mapping.dateColumn), format) ?: return null
        val (amountPaise, type) = amountAndType(row, mapping) ?: return null
        val balancePaise = parseAmount(row.cell(mapping.balanceColumn))?.let {
            if (it.negative == true) -it.valueMinor else it.valueMinor
        }
        val description = row.cell(mapping.descriptionColumn).trim().replace(WHITESPACE, " ")

        val direction = if (type == TransactionType.Income) "Cr" else "Dr"
        val key = listOf(date, description, amountPaise, direction, balancePaise, account.bankName, account.last4)
            .joinToString("|")
        val occurrence = seen.merge(key, 1, Int::plus) ?: 1
        val body = buildString {
            append("$date · $description · $direction ₹${amountPaise / 100}.${"%02d".format(Locale.ROOT, amountPaise % 100)}")
            balancePaise?.let { append(" · balance ₹${it / 100}.${"%02d".format(Locale.ROOT, kotlin.math.abs(it) % 100)}") }
            append(" · ${account.bankName}${account.last4?.let { " $it" }.orEmpty()}")
            if (occurrence > 1) append(" · #$occurrence")
        }

        val timestamp = date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val hash = generateHash(timestamp, amountPaise, type, body)

        return Transaction(
            amount = amountPaise,
            type = type,
            merchantRaw = merchantFrom(description) ?: "Unknown",
            merchantNormalised = merchantFrom(description) ?: "Unknown",
            refNumber = referenceFrom(row.cell(mapping.referenceColumn), description),
            accountLast4 = account.last4,
            balanceAfter = balancePaise,
            smsBody = body,
            sender = STATEMENT_SENDER,
            dateTime = timestamp,
            bankName = account.bankName,
            currency = account.currency,
            smsHash = hash,
            isReviewed = true,
            isCounted = true,
        )
    }

    internal fun merchantFrom(description: String): String? {
        val parts = description.split('/').map { it.trim() }.filter { it.isNotEmpty() }
        val payee = if (parts.size >= 2 && parts[0].uppercase(Locale.ROOT) in CHANNELS) {
            parts.drop(1).firstOrNull { p -> p.any(Char::isLetter) && '@' !in p && !IFSC.matches(p) }
        } else {
            null
        }
        return (payee ?: description).take(MAX_MERCHANT).ifBlank { null }
    }

    internal fun referenceFrom(referenceCell: String, description: String): String? {
        referenceCell.trim().takeIf { REFERENCE_TOKEN.matches(it) && it.any(Char::isDigit) }?.let { return it }
        CHANNEL_REFERENCE.find(description)?.let { return it.groupValues[1] }
        return REFERENCE.findAll(description).lastOrNull()?.value
    }

    private fun amountAndType(row: List<String>, mapping: StatementColumnMapping): Pair<Long, TransactionType>? {
        if (mapping.usesDebitCredit) {
            val debit = parseAmount(row.cell(mapping.debitColumn))?.valueMinor
            val credit = parseAmount(row.cell(mapping.creditColumn))?.valueMinor
            return when {
                debit != null && debit > 0L -> debit to TransactionType.Expense
                credit != null && credit > 0L -> credit to TransactionType.Income
                else -> null
            }
        }
        val cell = parseAmount(row.cell(mapping.amountColumn)) ?: return null
        if (cell.valueMinor == 0L) return null
        val debit = directionWord(row.cell(mapping.typeColumn)) ?: (cell.negative == true)
        val type = if (debit) TransactionType.Expense else TransactionType.Income
        return cell.valueMinor to type
    }

    private fun generateHash(timestamp: Long, amount: Long, type: TransactionType, body: String): String {
        val data = "$timestamp|$amount|${type.name}|$body"
        return MessageDigest.getInstance("MD5")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun List<String>.cell(index: Int): String = if (index < 0) "" else getOrNull(index).orEmpty()

    private val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)

    private val SERIAL_RANGE: ClosedFloatingPointRange<Double> =
        ChronoUnit.DAYS.between(EXCEL_EPOCH, LocalDate.of(YEARS.first, 1, 1)).toDouble()..
            ChronoUnit.DAYS.between(EXCEL_EPOCH, LocalDate.of(YEARS.last, 12, 31)).toDouble()

    private const val ISOLATION_DAYS = 366L
}
