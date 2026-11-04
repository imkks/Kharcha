package com.kharcha.core.common

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.io.BufferedReader
import java.io.PushbackReader
import java.io.Reader
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CsvTransactionImporter {

    companion object {
        private const val DEFAULT_CATEGORY = "Others"
        private const val DEFAULT_CURRENCY = "INR"

        private val ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val ISO_TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
        private val DMY_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val DMY_DASH = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }

    data class ImportParseResult(
        val transactions: List<Transaction>,
        val failedCount: Int,
        val failureReasons: List<String> = emptyList(),
    )

    fun parse(reader: Reader): ImportParseResult {
        val rows = parseCsvRows(reader)
        if (rows.isEmpty()) {
            return ImportParseResult(emptyList(), 0)
        }

        val header = rows.first()
        val columnIndex = HashMap<String, Int>()
        header.forEachIndexed { index, name ->
            columnIndex[name.removePrefix("\uFEFF").trim().lowercase(Locale.ROOT)] = index
        }

        fun indexOf(vararg candidates: String): Int? {
            for (c in candidates) {
                val idx = columnIndex[c.lowercase(Locale.ROOT)]
                if (idx != null) return idx
            }
            return null
        }

        val dateIdx = indexOf("date", "date_time", "transaction date", "txndate")
        val epochIdx = indexOf("epoch_ms", "epoch", "timestamp")
        val amountPaiseIdx = indexOf("amount_paise")
        val amountIdx = amountPaiseIdx ?: indexOf("amount", "amount_inr", "inr", "transaction amount")
        val typeIdx = indexOf("type", "transaction type", "dr_cr", "dr/cr", "cr/dr")

        if ((dateIdx == null && epochIdx == null) || amountIdx == null || typeIdx == null) {
            val missing = buildList {
                if (dateIdx == null && epochIdx == null) add("Date")
                if (amountIdx == null) add("Amount")
                if (typeIdx == null) add("Type")
            }.joinToString(", ")
            return ImportParseResult(
                transactions = emptyList(),
                failedCount = 0,
                failureReasons = listOf("CSV is missing required column(s): $missing"),
            )
        }

        val timeIdx = indexOf("time", "txn time")
        val merchantIdx = indexOf("merchant", "merchant_normalised", "merchant_raw", "payee", "party", "description")
        val categoryIdx = indexOf("category", "category_source")
        val currencyIdx = indexOf("currency")
        val bankIdx = indexOf("bank", "bank_name")
        val accountIdx = indexOf("account", "account_last4", "account_number")
        val channelIdx = indexOf("channel", "payment_mode")
        val balancePaiseIdx = indexOf("balance_after_paise")
        val balanceIdx = balancePaiseIdx ?: indexOf("balance after", "balance_after_inr", "balance")
        val noteIdx = indexOf("description", "note", "raw_sms_body", "remarks")
        val refIdx = indexOf("ref_number", "reference", "ref", "rrn", "upi_ref")

        val transactions = mutableListOf<Transaction>()
        val failureReasons = mutableListOf<String>()
        var failedCount = 0

        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.all { it.isBlank() }) continue

            try {
                transactions.add(
                    parseRow(
                        row = row,
                        dateIdx = dateIdx,
                        epochIdx = epochIdx,
                        timeIdx = timeIdx,
                        amountIdx = amountIdx,
                        isAmountPaise = (amountIdx == amountPaiseIdx),
                        typeIdx = typeIdx,
                        merchantIdx = merchantIdx,
                        categoryIdx = categoryIdx,
                        currencyIdx = currencyIdx,
                        bankIdx = bankIdx,
                        accountIdx = accountIdx,
                        channelIdx = channelIdx,
                        balanceIdx = balanceIdx,
                        isBalancePaise = (balanceIdx != null && balanceIdx == balancePaiseIdx),
                        noteIdx = noteIdx,
                        refIdx = refIdx,
                    ),
                )
            } catch (e: Exception) {
                failedCount++
                failureReasons.add("Row $i: ${e.message}")
            }
        }

        return ImportParseResult(transactions, failedCount, failureReasons)
    }

    private fun parseRow(
        row: List<String>,
        dateIdx: Int?,
        epochIdx: Int?,
        timeIdx: Int?,
        amountIdx: Int,
        isAmountPaise: Boolean,
        typeIdx: Int,
        merchantIdx: Int?,
        categoryIdx: Int?,
        currencyIdx: Int?,
        bankIdx: Int?,
        accountIdx: Int?,
        channelIdx: Int?,
        balanceIdx: Int?,
        isBalancePaise: Boolean,
        noteIdx: Int?,
        refIdx: Int?,
    ): Transaction {
        val dateTimeEpoch = parseDateTime(
            epochText = epochIdx?.let { row.cell(it) },
            dateText = dateIdx?.let { row.cell(it) },
            timeText = timeIdx?.let { row.cell(it) },
        )

        val amountMinor = parseAmountMinor(
            amountText = row.cell(amountIdx) ?: throw IllegalArgumentException("Missing Amount"),
            isPaise = isAmountPaise,
        )

        val rawType = row.cell(typeIdx) ?: throw IllegalArgumentException("Missing Type")
        val (type, isTransfer) = parseType(rawType)

        val merchant = row.cell(merchantIdx)?.takeIf { it.isNotBlank() } ?: "Unknown"
        val category = if (isTransfer) "Transfer" else (row.cell(categoryIdx)?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY)
        val currency = row.cell(currencyIdx)?.takeIf { it.isNotBlank() } ?: DEFAULT_CURRENCY
        val bank = row.cell(bankIdx)?.takeIf { it.isNotBlank() }
        val account = row.cell(accountIdx)?.takeIf { it.isNotBlank() }
        val refNumber = row.cell(refIdx)?.takeIf { it.isNotBlank() }
        val note = row.cell(noteIdx)?.takeIf { it.isNotBlank() }
        val channel = parseChannel(channelIdx?.let { row.cell(it) })

        val balanceAfterMinor = balanceIdx?.let { idx ->
            row.cell(idx)?.let { parseBalanceMinor(it, isBalancePaise) }
        }

        val hash = generateImportHash(
            dateTime = dateTimeEpoch,
            merchant = merchant,
            amountMinor = amountMinor,
            type = type,
            account = account,
        )

        return Transaction(
            amount = amountMinor,
            type = type,
            merchantRaw = merchant,
            merchantNormalised = merchant,
            categorySource = category,
            dateTime = dateTimeEpoch,
            channel = channel,
            bankName = bank,
            accountLast4 = account?.takeLast(4),
            refNumber = refNumber,
            balanceAfter = balanceAfterMinor,
            note = note,
            currency = currency,
            smsHash = hash,
            isReviewed = true,
            isCounted = !isTransfer,
        )
    }

    private fun parseAmountMinor(amountText: String, isPaise: Boolean): Long {
        val clean = amountText.replace(",", "").replace("₹", "").replace("$", "").trim()
        if (isPaise) {
            val paise = clean.toLongOrNull()
            if (paise != null && paise > 0L) return paise
        }
        val minor = MoneyFormatter.parseMinorUnits(clean)
            ?: throw IllegalArgumentException("Invalid Amount '$amountText'")
        if (minor <= 0L) {
            throw IllegalArgumentException("Amount must be positive: '$amountText'")
        }
        return minor
    }

    private fun parseBalanceMinor(balanceText: String, isPaise: Boolean): Long? {
        val clean = balanceText.replace(",", "").replace("₹", "").replace("$", "").trim()
        if (isPaise) return clean.toLongOrNull()
        return MoneyFormatter.parseMinorUnits(clean)
    }

    private fun parseType(raw: String): Pair<TransactionType, Boolean> {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        return when (normalized) {
            "income", "credit", "cr" -> Pair(TransactionType.Income, false)
            "expense", "debit", "dr", "credit card" -> Pair(TransactionType.Expense, false)
            "transfer" -> Pair(TransactionType.Expense, true)
            else -> throw IllegalArgumentException("Unknown Type '$raw'")
        }
    }

    private fun parseChannel(raw: String?): Channel {
        if (raw == null) return Channel.UPI
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "card", "credit card", "debit card" -> Channel.CARD
            "atm", "cash" -> Channel.ATM
            "neft" -> Channel.NEFT
            "imps" -> Channel.IMPS
            "wallet" -> Channel.WALLET
            else -> Channel.UPI
        }
    }

    private fun parseDateTime(epochText: String?, dateText: String?, timeText: String?): Long {
        if (!epochText.isNullOrBlank()) {
            val epoch = epochText.trim().toLongOrNull()
            if (epoch != null && epoch > 0L) return epoch
        }

        if (dateText.isNullOrBlank()) {
            throw IllegalArgumentException("Missing Date")
        }

        val trimmedDate = dateText.trim()
        val zoneId = ZoneId.systemDefault()

        // Check if date contains time (e.g. "yyyy-MM-dd HH:mm:ss" or ISO)
        if (trimmedDate.contains(" ") || trimmedDate.contains("T")) {
            val normalized = trimmedDate.replace("T", " ")
            try {
                val ldt = LocalDateTime.parse(normalized, ISO_DATE_TIME)
                return ldt.atZone(zoneId).toInstant().toEpochMilli()
            } catch (_: Exception) {
                try {
                    val instant = Instant.parse(trimmedDate)
                    return instant.toEpochMilli()
                } catch (_: Exception) {
                    // Try date-only fallback below
                }
            }
        }

        val localDate = try {
            LocalDate.parse(trimmedDate, ISO_DATE)
        } catch (_: Exception) {
            try {
                LocalDate.parse(trimmedDate, DMY_SLASH)
            } catch (_: Exception) {
                try {
                    LocalDate.parse(trimmedDate, DMY_DASH)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid Date '$dateText'")
                }
            }
        }

        val localTime = if (timeText.isNullOrBlank()) {
            LocalTime.MIDNIGHT
        } else {
            try {
                LocalTime.parse(timeText.trim(), ISO_TIME)
            } catch (_: Exception) {
                LocalTime.MIDNIGHT
            }
        }

        return LocalDateTime.of(localDate, localTime).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun generateImportHash(
        dateTime: Long,
        merchant: String,
        amountMinor: Long,
        type: TransactionType,
        account: String?,
    ): String {
        val data = "CSV_v1_${dateTime}_${merchant}_${amountMinor}_${type.name}_${account ?: ""}"
        return MessageDigest.getInstance("MD5")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun parseCsvRows(reader: Reader): List<List<String>> {
        val pushback = PushbackReader(if (reader is BufferedReader) reader else BufferedReader(reader))
        val rows = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        val currentRow = mutableListOf<String>()
        var inQuotes = false

        while (true) {
            val code = pushback.read()
            if (code == -1) break
            val c = code.toChar()

            if (inQuotes) {
                if (c == '"') {
                    val next = pushback.read()
                    if (next != -1 && next.toChar() == '"') {
                        currentField.append('"')
                    } else {
                        inQuotes = false
                        if (next != -1) {
                            pushback.unread(next)
                        }
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        val next = pushback.read()
                        if (next != -1 && next.toChar() != '\n') {
                            pushback.unread(next)
                        }
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(ArrayList(currentRow))
                        }
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(ArrayList(currentRow))
                        }
                        currentRow.clear()
                    }
                    else -> currentField.append(c)
                }
            }
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(ArrayList(currentRow))
            }
        }
        return rows
    }

    private fun List<String>.cell(index: Int?): String? {
        if (index == null || index >= size) return null
        return this[index].trim().takeIf { it.isNotEmpty() }
    }
}
