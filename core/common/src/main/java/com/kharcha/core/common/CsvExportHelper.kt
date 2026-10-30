package com.kharcha.core.common

import com.kharcha.core.model.Transaction
import com.kharcha.core.model.isCounted
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
                "category_confidence",
                "merchant_raw",
                "merchant_normalised",
                "bank_name",
                "account_last4",
                "account_id",
                "ref_number",
                "upi_id",
                "balance_after_paise",
                "balance_after_inr",
                "sender",
                "parser_template",
                "parse_confidence",
                "is_transfer",
                "is_counted",
                "linked_txn_id",
                "pipeline_steps",
                "is_reviewed",
                "raw_sms_body",
            ).joinToString(","),
        ).append("\n")

        for (t in transactions) {
            val dateStr = dateFormatter.format(Instant.ofEpochMilli(t.dateTime))
            val amountInr = String.format(Locale.US, "%.2f", t.amount / 100.0)
            val balanceInr =
                t.balanceAfter?.let {
                    String.format(Locale.US, "%.2f", it / 100.0)
                } ?: ""
            val bank = t.bankName ?: inferBankFromNote(t.note)
            val last4 = t.accountLast4 ?: inferAccountLast4FromNote(t.note)
            val sender = t.sender ?: inferSenderFromHash(t.smsHash)
            val template = t.templateId ?: "unknown"
            val pipelineSteps = buildPipelineSteps(t)

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
                    t.categoryConfidence?.toString() ?: "",
                    escapeCsv(t.merchantRaw ?: ""),
                    escapeCsv(t.merchantNormalised ?: ""),
                    escapeCsv(bank),
                    escapeCsv(last4),
                    t.accountId.toString(),
                    escapeCsv(t.refNumber ?: ""),
                    escapeCsv(t.upiId ?: ""),
                    t.balanceAfter?.toString() ?: "",
                    balanceInr,
                    escapeCsv(sender),
                    escapeCsv(template),
                    t.parseConfidence?.toString() ?: "",
                    t.isTransfer.toString(),
                    t.isCounted.toString(),
                    t.linkedTransactionId?.toString() ?: "",
                    escapeCsv(pipelineSteps),
                    t.isReviewed.toString(),
                    escapeCsv(t.note ?: ""),
                )
            sb.append(row.joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    private fun buildPipelineSteps(t: Transaction): String {
        val parser = t.templateId ?: t.bankName ?: "unknown"
        val channel = t.channel?.name ?: "UNKNOWN"
        val type = t.type.name
        val cat = t.categorySource ?: "Other"
        val isTransfer = t.isTransfer
        val isCounted = t.isCounted
        val linked = t.linkedTransactionId?.let { "LinkedTo($it)" } ?: "Unlinked"

        return "Parser($parser) -> Channel($channel) -> Type($type) -> Category($cat) -> Transfer($isTransfer) -> Reconciled($linked) -> CountedInTotal($isCounted)"
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

    private fun inferBankFromNote(note: String?): String {
        if (note.isNullOrBlank()) return ""
        val lower = note.lowercase()
        return when {
            lower.contains("hdfc") -> "HDFC"
            lower.contains("sbi") || lower.contains("state bank") -> "SBI"
            lower.contains("icici") -> "ICICI"
            lower.contains("axis") -> "Axis"
            lower.contains("kotak") -> "Kotak"
            lower.contains("baroda") || lower.contains("bob") -> "BOB"
            lower.contains("pnb") || lower.contains("punjab") -> "PNB"
            lower.contains("canara") -> "Canara"
            lower.contains("union bank") -> "Union Bank"
            lower.contains("idfc") -> "IDFC"
            lower.contains("indusind") -> "IndusInd"
            lower.contains("federal") -> "Federal"
            lower.contains("yes bank") -> "Yes Bank"
            lower.contains("onecard") -> "OneCard"
            lower.contains("slice") -> "Slice"
            lower.contains("paytm") -> "Paytm"
            lower.contains("airtel") -> "Airtel"
            else -> ""
        }
    }

    private fun inferAccountLast4FromNote(note: String?): String {
        if (note.isNullOrBlank()) return ""
        val match =
            Regex(
                """(?i)(?:a/c|acct|account|card|ending|no\.?)[*xX.\s\-]*([0-9]{3,4})\b""",
            ).find(note)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun inferSenderFromHash(smsHash: String?): String {
        if (smsHash.isNullOrBlank() || !smsHash.contains("|")) return ""
        return smsHash.split("|").firstOrNull() ?: ""
    }
}
