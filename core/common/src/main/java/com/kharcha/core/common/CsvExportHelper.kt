package com.kharcha.core.common

import com.kharcha.core.model.Transaction
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExportHelper {
    private val dateFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    fun generateTransactionsCsv(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append(
            listOf(
                "id",
                "date",
                "epoch_ms",
                "amount_paise",
                "amount_inr",
                "type",
                "channel",
                "category",
                "merchant_raw",
                "merchant_normalised",
                "ref_number",
                "upi_id",
                "balance_after_paise",
                "is_reviewed",
                "parse_confidence",
                "raw_sms_body",
            ).joinToString(","),
        ).append("\n")

        for (t in transactions) {
            val dateStr = dateFormatter.format(Instant.ofEpochMilli(t.dateTime))
            val amountInr = String.format(Locale.US, "%.2f", t.amount / 100.0)
            val row =
                listOf(
                    t.id.toString(),
                    escapeCsv(dateStr),
                    t.dateTime.toString(),
                    t.amount.toString(),
                    amountInr,
                    t.type.name,
                    t.channel?.name ?: "",
                    escapeCsv(t.categorySource ?: "Other"),
                    escapeCsv(t.merchantRaw ?: ""),
                    escapeCsv(t.merchantNormalised ?: ""),
                    escapeCsv(t.refNumber ?: ""),
                    escapeCsv(t.upiId ?: ""),
                    t.balanceAfter?.toString() ?: "",
                    t.isReviewed.toString(),
                    t.parseConfidence?.toString() ?: "",
                    escapeCsv(t.note ?: ""),
                )
            sb.append(row.joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    fun generateLlmBatchExportJson(transactions: List<Transaction>): String {
        val unreviewedOrOther =
            transactions.filter {
                !it.isReviewed || it.categorySource == "Other" || (it.parseConfidence ?: 1.0f) < 0.8f
            }

        val grouped =
            unreviewedOrOther.groupBy {
                it.merchantNormalised?.ifBlank { null } ?: it.merchantRaw ?: "Unknown"
            }

        val jsonArray = JSONArray()
        for ((merchant, txns) in grouped) {
            val obj = JSONObject()
            obj.put("merchant", merchant)
            obj.put("count", txns.size)
            val totalMinor = txns.sumOf { it.amount }
            obj.put("total_inr", String.format(Locale.US, "%.2f", totalMinor / 100.0))
            obj.put("channel", txns.firstOrNull()?.channel?.name ?: "UNKNOWN")
            val sampleNote = txns.firstNotNullOfOrNull { it.note?.ifBlank { null } } ?: ""
            obj.put("sample_sms", sampleNote)
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    private fun escapeCsv(value: String): String {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
