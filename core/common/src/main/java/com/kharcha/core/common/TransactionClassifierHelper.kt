package com.kharcha.core.common

import com.kharcha.core.model.Direction
import com.kharcha.core.model.TransactionType

object TransactionClassifierHelper {
    fun cleanMerchantDisplay(raw: String?, bank: String?, channel: String?): String {
        if (raw.isNullOrBlank()) {
            return if (!bank.isNullOrBlank()) "$bank ${channel ?: "Txn"}" else "Transaction"
        }
        val withoutHandle = raw.substringBefore("@")
        val cleaned = withoutHandle.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered =
            words.filter { word ->
                word.uppercase() !in setOf("PVT", "LTD", "LIMITED", "PRIVATE", "INC", "CORP", "VPA", "UPI", "POS")
            }
        val finalWords = if (filtered.isNotEmpty()) filtered else words
        return finalWords.take(4).joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun isTransferNarrative(body: String, merchant: String? = null): Boolean {
        val b = body.lowercase()
        val m = merchant?.lowercase() ?: ""
        return b.contains("self transfer") || b.contains("transfer to self") ||
            b.contains("own account") || b.contains("own a/c") ||
            b.contains("towards credit card") || b.contains("credit card payment") ||
            b.contains("card payment") || b.contains("payment to card") ||
            b.contains("payment received for card") || b.contains("cc payment") ||
            m.contains("credit card") || m.contains("self transfer") ||
            m.equals("transfer", ignoreCase = true)
    }

    fun classifyType(direction: Direction, body: String, merchant: String? = null): TransactionType {
        if (isTransferNarrative(body, merchant)) {
            return TransactionType.Transfer
        }
        return if (direction == Direction.CREDIT) TransactionType.Income else TransactionType.Expense
    }

    fun guessCategory(merchant: String, isCredit: Boolean, isTransfer: Boolean = false): String {
        if (isTransfer) return "Transfer"
        if (isCredit) return "Salary"
        val m = merchant.lowercase()
        return when {
            m.contains("swiggy") || m.contains("zomato") || m.contains("mcdonald") ||
                m.contains("kfc") || m.contains("domino") || m.contains("pizza") ||
                m.contains("starbucks") || m.contains("coffee") || m.contains("chai") ||
                m.contains("burger") || m.contains("subway") || m.contains("restaurant") -> "Food & Dining"
            m.contains("blinkit") || m.contains("zepto") || m.contains("instamart") ||
                m.contains("bigbasket") || m.contains("dmart") || m.contains("grocery") ||
                m.contains("supermarket") || m.contains("reliance fresh") -> "Groceries"
            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
                m.contains("ajio") || m.contains("meesho") || m.contains("tata cliq") ||
                m.contains("retail") || m.contains("store") -> "Shopping"
            m.contains("uber") || m.contains("ola") || m.contains("rapido") ||
                m.contains("metro") || m.contains("irctc") || m.contains("petrol") ||
                m.contains("fuel") || m.contains("hpcl") || m.contains("bpcl") ||
                m.contains("ioc") || m.contains("railway") -> "Transport"
            m.contains("airtel") || m.contains("jio") || m.contains("vi ") ||
                m.contains("bescom") || m.contains("electricity") || m.contains("water") ||
                m.contains("gas") || m.contains("broadband") || m.contains("bill") -> "Bills & Utilities"
            m.contains("netflix") || m.contains("spotify") || m.contains("prime") ||
                m.contains("hotstar") || m.contains("bookmyshow") || m.contains("cinema") ||
                m.contains("pvr") || m.contains("inox") -> "Entertainment"
            m.contains("apollo") || m.contains("pharmacy") || m.contains("1mg") ||
                m.contains("pharmeasy") || m.contains("hospital") || m.contains("clinic") ||
                m.contains("medplus") -> "Health"
            m.contains("zerodha") || m.contains("groww") || m.contains("mutual") ||
                m.contains("sip") || m.contains("indmoney") || m.contains("upstox") -> "Investments"
            else -> "Other"
        }
    }
}
