package com.kharcha.core.common

import android.content.Context
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DefaultTransactionRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TransactionRepository {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val file: File
        get() {
            val dir =
                try {
                    context.filesDir
                } catch (_: Exception) {
                    null
                } ?: File(System.getProperty("java.io.tmpdir", "."))
            return File(dir, "transactions.json")
        }

    init {
        loadFromDisk()
    }

    override fun getTransactions(): Flow<List<Transaction>> = _transactions.asStateFlow()

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        if (transactions.isEmpty()) return
        withContext(ioDispatcher) {
            val current = _transactions.value.toMutableList()
            val existingIds = current.map { it.id }.toMutableSet()
            val existingHashes = current.mapNotNull { it.smsHash }.toMutableSet()

            for (txn in transactions) {
                val hasHashConflict = txn.smsHash != null && existingHashes.contains(txn.smsHash)
                val hasIdConflict = existingIds.contains(txn.id)

                if (!hasHashConflict && !hasIdConflict) {
                    current.add(txn)
                    existingIds.add(txn.id)
                    txn.smsHash?.let { existingHashes.add(it) }
                }
            }

            val updated = current.sortedByDescending { it.dateTime }
            _transactions.value = updated
            saveToDisk(updated)
        }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        insertTransactions(listOf(transaction))
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        withContext(ioDispatcher) {
            val current = _transactions.value.toMutableList()
            val index = current.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                current[index] = transaction.copy(updatedAt = System.currentTimeMillis())
                val updated = current.sortedByDescending { it.dateTime }
                _transactions.value = updated
                saveToDisk(updated)
            }
        }
    }

    override suspend fun clearTransactions() {
        withContext(ioDispatcher) {
            _transactions.value = emptyList()
            if (file.exists()) {
                file.delete()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadFromDisk() {
        try {
            if (!file.exists()) return
            val text = file.readText()
            if (text.isBlank()) return
            val jsonArray = JSONArray(text)
            val list = mutableListOf<Transaction>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(jsonToTransaction(obj))
            }
            _transactions.value = list.sortedByDescending { it.dateTime }
        } catch (_: Exception) {
            // Keep empty list if corrupted
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun saveToDisk(list: List<Transaction>) {
        try {
            val jsonArray = JSONArray()
            for (txn in list) {
                jsonArray.put(transactionToJson(txn))
            }
            file.writeText(jsonArray.toString())
        } catch (_: Exception) {
            // Log or ignore disk errors gracefully
        }
    }

    private fun transactionToJson(txn: Transaction): JSONObject {
        val obj = JSONObject()
        obj.put("id", txn.id)
        obj.put("smsHash", txn.smsHash)
        obj.put("accountId", txn.accountId)
        obj.put("type", txn.type.name)
        obj.put("amount", txn.amount)
        obj.put("currency", txn.currency)
        obj.put("dateTime", txn.dateTime)
        obj.put("merchantRaw", txn.merchantRaw)
        obj.put("merchantNormalised", txn.merchantNormalised)
        obj.put("channel", txn.channel?.name)
        obj.put("refNumber", txn.refNumber)
        obj.put("upiId", txn.upiId)
        obj.put("balanceAfter", txn.balanceAfter)
        obj.put("note", txn.note)
        obj.put("categorySource", txn.categorySource)
        obj.put("isReviewed", txn.isReviewed)
        txn.parseConfidence?.let { obj.put("parseConfidence", it.toDouble()) }
        return obj
    }

    @Suppress("TooGenericExceptionCaught")
    private fun jsonToTransaction(json: JSONObject): Transaction {
        val typeStr = json.optString("type", TransactionType.Expense.name)
        val channelStr = if (json.has("channel") && !json.isNull("channel")) json.getString("channel") else null

        val txnType =
            try {
                TransactionType.valueOf(typeStr)
            } catch (_: Exception) {
                TransactionType.Expense
            }

        val parsedChannel =
            if (!channelStr.isNullOrEmpty()) {
                try {
                    Channel.valueOf(channelStr)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

        val parseConf =
            if (json.has("parseConfidence") && !json.isNull("parseConfidence")) {
                json.getDouble("parseConfidence").toFloat()
            } else {
                null
            }

        return Transaction(
            id = json.optLong("id"),
            smsHash = if (json.has("smsHash") && !json.isNull("smsHash")) json.getString("smsHash") else null,
            accountId = json.optLong("accountId"),
            type = txnType,
            amount = json.optLong("amount"),
            currency = json.optString("currency", "INR"),
            dateTime = json.optLong("dateTime"),
            merchantRaw =
                if (json.has("merchantRaw") && !json.isNull("merchantRaw")) json.getString("merchantRaw") else null,
            merchantNormalised =
                if (json.has("merchantNormalised") && !json.isNull("merchantNormalised")) {
                    json.getString("merchantNormalised")
                } else {
                    null
                },
            channel = parsedChannel,
            refNumber = if (json.has("refNumber") && !json.isNull("refNumber")) json.getString("refNumber") else null,
            upiId = if (json.has("upiId") && !json.isNull("upiId")) json.getString("upiId") else null,
            balanceAfter =
                if (json.has("balanceAfter") && !json.isNull("balanceAfter")) json.getLong("balanceAfter") else null,
            note = if (json.has("note") && !json.isNull("note")) json.getString("note") else null,
            categorySource =
                if (json.has("categorySource") && !json.isNull("categorySource")) {
                    json.getString("categorySource")
                } else {
                    null
                },
            isReviewed = json.optBoolean("isReviewed", false),
            parseConfidence = parseConf,
        )
    }

    companion object {
        @Volatile
        private var instance: DefaultTransactionRepository? = null

        fun getInstance(context: Context): DefaultTransactionRepository {
            return instance ?: synchronized(this) {
                val appContext =
                    try {
                        context.applicationContext ?: context
                    } catch (_: Exception) {
                        context
                    }
                instance ?: DefaultTransactionRepository(appContext).also {
                    instance = it
                }
            }
        }
    }
}
