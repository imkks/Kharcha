package com.kharcha.core.common

import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportHelperTest {
    @Test
    fun testGenerateTransactionsCsvHeadersAndEscaping() {
        val txns =
            listOf(
                Transaction(
                    id = 101L,
                    accountId = 1L,
                    type = TransactionType.EXPENSE,
                    amount = 18596_70L,
                    currency = "INR",
                    dateTime = 1790447880000L,
                    merchantRaw = "RAZ*Optick Labs, Inc",
                    merchantNormalised = "Raz Optick Labs",
                    categorySource = "Shopping",
                    channel = Channel.CARD,
                    refNumber = "REF12345",
                    note = "INR 18596.70 spent on your card \"ending\" 5625 at RAZ*Optick Labs, Inc",
                    isReviewed = true,
                    parseConfidence = 0.95f,
                    templateId = "hdfc_card_spend_v1",
                    accountLast4 = "5625",
                    bankName = "HDFC",
                    sender = "VM-HDFCBK",
                ),
            )

        val csv = CsvExportHelper.generateTransactionsCsv(txns)
        val lines = csv.trim().split("\n")
        assertEquals(2, lines.size)

        // Header check
        assertTrue(lines[0].startsWith("id,date,epoch_ms,amount_paise,amount_inr,type,channel,category"))
        assertTrue(lines[0].contains("parser_template"))
        assertTrue(lines[0].contains("bank_name"))
        assertTrue(lines[0].contains("account_last4"))
        assertTrue(lines[0].contains("sender"))
        assertTrue(lines[0].contains("balance_after_inr"))

        // Row values check
        val row = lines[1]
        assertTrue(row.contains("101"))
        assertTrue(row.contains("1859670"))
        assertTrue(row.contains("18596.70"))
        assertTrue(row.contains("EXPENSE"))
        assertTrue(row.contains("CARD"))
        assertTrue(row.contains("Shopping"))
        assertTrue(row.contains("hdfc_card_spend_v1"))
        assertTrue(row.contains("HDFC"))
        assertTrue(row.contains("5625"))
        assertTrue(row.contains("VM-HDFCBK"))
        // Escaped fields containing comma
        assertTrue(row.contains("\"RAZ*Optick Labs, Inc\""))
        // Escaped quotes in raw note
        assertTrue(row.contains("\"\"ending\"\""))
    }

    @Test
    fun testGenerateLlmBatchExportJsonDeduplication() {
        val txns =
            listOf(
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    type = TransactionType.EXPENSE,
                    amount = 250_00L,
                    dateTime = 1790447880000L,
                    merchantNormalised = "Unknown Cafe",
                    categorySource = "Other",
                    channel = Channel.UPI,
                    note = "Paid Rs 250 to Unknown Cafe",
                    isReviewed = false,
                ),
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    type = TransactionType.EXPENSE,
                    amount = 350_00L,
                    dateTime = 1790447880000L,
                    merchantNormalised = "Unknown Cafe",
                    categorySource = "Other",
                    channel = Channel.UPI,
                    note = "Paid Rs 350 to Unknown Cafe",
                    isReviewed = false,
                ),
                Transaction(
                    id = 3L,
                    accountId = 1L,
                    type = TransactionType.EXPENSE,
                    amount = 500_00L,
                    dateTime = 1790447880000L,
                    merchantNormalised = "Swiggy",
                    categorySource = "Food & Dining",
                    isReviewed = true, // Reviewed -> should NOT be in LLM unreviewed batch
                ),
            )

        val json = CsvExportHelper.generateLlmBatchExportJson(txns)
        assertTrue(json.contains("Unknown Cafe"))
        assertTrue(json.contains("\"count\": 2"))
        assertTrue(json.contains("\"total_inr\": \"600.00\""))
        // Swiggy is reviewed, so it should not appear in leftover batch
        assertTrue(!json.contains("Swiggy"))
    }
}
