package com.kharcha.core.common.statement.pdf

import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.statement.PdfStatementParser
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GPayPdfParser : PdfStatementParser {

    companion object {
        private const val DATE_FORMAT_PATTERN = "dd MMM, yyyy hh:mm a"
    }

    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    private val merchantAnchorRegex = Regex(
        """^Paid\s+to\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    )
    private val receivedAnchorRegex = Regex(
        """^Received\s+from\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    )
    private val bankAccountLineRegex = Regex(
        """^Paid\s+(?:by|to)\s+(.+)\s+(Bank|Card|A/c)\s+(\d{4})$""",
        RegexOption.IGNORE_CASE,
    )
    private val upiIdRegex = Regex(
        """UPI\s+Transaction\s+ID[:\s]+(\d+)""",
        RegexOption.IGNORE_CASE,
    )
    private val amountRegex = Regex("""^(?:₹|Rs\.?)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)$""")
    private val dateLineRegex = Regex("""^(\d{1,2})\s+(\w{3}),?$""")
    private val fullDateLineRegex = Regex("""^(\d{1,2}\s+\w{3},\s+20\d{2})$""")
    private val datePrefixedRowRegex = Regex("""^(\d{1,2}\s+\w{3},\s+20\d{2})\s+(.+)$""")
    private val yearLineRegex = Regex("""^(20\d{2})$""")
    private val timeLineRegex = Regex("""^(\d{1,2}:\d{2})\s*([AaPp][Mm])(?:\s+(.+))?$""")

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return ("google pay" in lower || "gpay" in lower) && "upi transaction id" in lower
    }

    override fun parse(text: String): List<Transaction> {
        val blocks = splitIntoBlocks(text)
        return blocks.mapIndexedNotNull { index, block ->
            parseBlock(block, index)
        }
    }

    private fun splitIntoBlocks(text: String): List<String> {
        val blocks = mutableListOf<String>()
        val current = StringBuilder()
        var pendingDate: String? = null
        var pendingYear: String? = null
        var pendingTime: String? = null
        var inBlock = false

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val isDate = dateLineRegex.matches(line)
            val isFullDate = fullDateLineRegex.matches(line)
            val isYear = yearLineRegex.matches(line)
            val isTime = timeLineRegex.matches(line)

            val datePrefixedRow = datePrefixedRowRegex.find(line)
            if (datePrefixedRow != null) {
                val fullDate = datePrefixedRow.groupValues[1]
                val rest = datePrefixedRow.groupValues[2].trim()
                val rowParts = splitAnchorAndAmount(rest)
                if (isTransactionAnchor(rowParts.anchorLine)) {
                    if (inBlock && current.isNotEmpty()) {
                        blocks.add(current.toString().trim())
                        current.clear()
                    }
                    current.appendLine(fullDate)
                    current.appendLine(rowParts.anchorLine)
                    rowParts.amountLine?.let { current.appendLine(it) }
                    pendingDate = null
                    pendingYear = null
                    pendingTime = null
                    inBlock = true
                    continue
                }
            }

            if (isTransactionAnchor(line)) {
                if (inBlock && current.isNotEmpty()) {
                    blocks.add(current.toString().trim())
                    current.clear()
                }
                pendingDate?.let { current.appendLine(it) }
                pendingYear?.let { current.appendLine(it) }
                pendingTime?.let { current.appendLine(it) }
                pendingDate = null
                pendingYear = null
                pendingTime = null
                current.appendLine(line)
                inBlock = true
                continue
            }

            if (isDate) { pendingDate = line; continue }
            if (isFullDate) { pendingDate = line; pendingYear = null; continue }
            if (isYear) { pendingYear = line; continue }
            if (isTime && inBlock && !timeLineRegex.find(line)?.groupValues?.getOrNull(3).isNullOrBlank()) {
                current.appendLine(line)
                continue
            }
            if (isTime) { pendingTime = line; continue }

            if (inBlock) current.appendLine(line)
        }

        if (current.isNotEmpty()) blocks.add(current.toString().trim())
        return blocks
    }

    private fun isTransactionAnchor(line: String): Boolean {
        if (receivedAnchorRegex.matches(line)) return true
        if (merchantAnchorRegex.matches(line)) {
            return !bankAccountLineRegex.matches(line)
        }
        return false
    }

    private fun parseBlock(block: String, index: Int): Transaction? {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val anchorLine = lines.firstOrNull { isTransactionAnchor(it) } ?: return null

        val isExpense = merchantAnchorRegex.matches(anchorLine)
        val type = if (isExpense) TransactionType.Expense else TransactionType.Income
        val merchant = extractMerchant(anchorLine, isExpense) ?: return null

        val amount = extractAmount(lines) ?: return null
        val timestamp = extractTimestamp(lines, merchant) ?: System.currentTimeMillis()
        val upiId = lines.firstNotNullOfOrNull { upiIdRegex.find(it)?.groupValues?.get(1) }
        val account = extractAccountInfo(lines)

        val hash = generateHash(timestamp, amount, type, upiId ?: merchant)

        return Transaction(
            amount = amount,
            type = type,
            merchantRaw = merchant,
            merchantNormalised = merchant,
            refNumber = upiId,
            accountLast4 = account.last4,
            balanceAfter = null,
            smsBody = block,
            sender = "GPay PDF",
            dateTime = timestamp,
            bankName = account.bankName ?: "Google Pay",
            currency = "INR",
            smsHash = hash,
            isReviewed = true,
            isCounted = true,
        )
    }

    private fun extractMerchant(anchorLine: String, isExpense: Boolean): String? {
        val regex = if (isExpense) merchantAnchorRegex else receivedAnchorRegex
        return regex.find(anchorLine)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun extractAmount(lines: List<String>): Long? {
        for (line in lines) {
            val raw = amountRegex.find(line)?.groupValues?.get(1) ?: continue
            val cleaned = raw.replace(",", "")
            val parsed = MoneyFormatter.parseMinorUnits(cleaned)
            if (parsed != null) return parsed
        }
        return null
    }

    private fun extractTimestamp(lines: List<String>, merchant: String): Long? {
        var dateLine: String? = null
        var yearLine: String? = null
        var timeLine: String? = null

        for (line in lines) {
            when {
                dateLine == null && (dateLineRegex.matches(line) || fullDateLineRegex.matches(line)) -> {
                    dateLine = line
                }
                yearLine == null && yearLineRegex.matches(line) -> yearLine = line
                timeLine == null && timeLineRegex.matches(line) -> {
                    val match = timeLineRegex.find(line)
                    timeLine = listOfNotNull(
                        match?.groupValues?.getOrNull(1),
                        match?.groupValues?.getOrNull(2),
                    ).joinToString(" ")
                }
            }
            if (dateLine != null && (yearLine != null || fullDateLineRegex.matches(dateLine)) && timeLine != null) break
        }

        if (dateLine == null || timeLine == null || (yearLine == null && !fullDateLineRegex.matches(dateLine))) {
            return null
        }

        val dateClean = dateLine.trimEnd(',', ' ')
        val timeClean = timeLine.replace(Regex("""\s+"""), " ").uppercase(Locale.ROOT)
        val combined = if (fullDateLineRegex.matches(dateLine)) {
            "$dateClean $timeClean"
        } else {
            "$dateClean, $yearLine $timeClean"
        }

        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.ENGLISH).apply {
                timeZone = ist
                isLenient = false
            }
            sdf.parse(combined)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun splitAnchorAndAmount(value: String): RowParts {
        val amountMatch = Regex("""\s+((?:₹|Rs\.?)\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?)$""")
            .find(value)
            ?: return RowParts(value.trim(), null)

        val anchorLine = value.substring(0, amountMatch.range.first).trim()
        return RowParts(anchorLine, amountMatch.groupValues[1].trim())
    }

    private fun extractAccountInfo(lines: List<String>): AccountInfo {
        val accountLine = lines.firstOrNull { bankAccountLineRegex.matches(it) } ?: return AccountInfo(null, null)
        val match = bankAccountLineRegex.find(accountLine)
        val bankName = if (match != null) {
            val first = match.groupValues.getOrNull(1)?.trim()
            val second = match.groupValues.getOrNull(2)?.trim()
            listOfNotNull(first, second).joinToString(" ").takeIf { it.isNotBlank() }
        } else {
            null
        }
        val last4 = match?.groupValues?.getOrNull(3)?.trim()
        return AccountInfo(bankName, last4)
    }

    private fun generateHash(timestamp: Long, amount: Long, type: TransactionType, uniquePart: String): String {
        val data = "$timestamp|$amount|${type.name}|$uniquePart"
        return MessageDigest.getInstance("MD5")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private data class AccountInfo(val bankName: String?, val last4: String?)
    private data class RowParts(val anchorLine: String, val amountLine: String?)
}
