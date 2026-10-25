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
            val combined = _transactions.value + transactions
            val deduplicated = deduplicateList(combined).toMutableList()
            reconcileTransfers(deduplicated)
            val updated = deduplicated.sortedByDescending { it.dateTime }
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
                reconcileTransfers(current)
                val updated = current.sortedByDescending { it.dateTime }
                _transactions.value = updated
                saveToDisk(updated)
            }
        }
    }

    override suspend fun updateTransactions(transactions: List<Transaction>) {
        if (transactions.isEmpty()) return
        withContext(ioDispatcher) {
            val current = _transactions.value.toMutableList()
            val map = transactions.associateBy { it.id }
            var changed = false
            for (i in current.indices) {
                val updated = map[current[i].id]
                if (updated != null) {
                    current[i] = updated.copy(updatedAt = System.currentTimeMillis())
                    changed = true
                }
            }
            if (changed) {
                reconcileTransfers(current)
                val sorted = current.sortedByDescending { it.dateTime }
                _transactions.value = sorted
                saveToDisk(sorted)
            }
        }
    }

    override suspend fun updateTransactionAndPropagateMerchant(
        transaction: Transaction,
        newCategory: String,
        newType: TransactionType?,
        userPreferences: UserPreferences?,
    ): Int {
        return withContext(ioDispatcher) {
            val normTarget = transaction.merchantNormalised?.trim()?.lowercase()
            val rawTarget = transaction.merchantRaw?.trim()?.lowercase()

            val primaryMerchant = if (!normTarget.isNullOrBlank()) normTarget else rawTarget
            if (!primaryMerchant.isNullOrBlank() && userPreferences != null) {
                userPreferences.setMerchantCategory(primaryMerchant, newCategory)
                if (!normTarget.isNullOrBlank()) userPreferences.setMerchantCategory(normTarget, newCategory)
                if (!rawTarget.isNullOrBlank()) userPreferences.setMerchantCategory(rawTarget, newCategory)
            }

            val current = _transactions.value.toMutableList()
            var updateCount = 0
            val now = System.currentTimeMillis()

            for (i in current.indices) {
                val t = current[i]
                val tNorm = t.merchantNormalised?.trim()?.lowercase()
                val tRaw = t.merchantRaw?.trim()?.lowercase()

                val matches =
                    t.id == transaction.id ||
                        (!primaryMerchant.isNullOrBlank() && (tNorm == primaryMerchant || tRaw == primaryMerchant))

                if (matches) {
                    val targetType =
                        newType
                            ?: if (newCategory.equals("Transfer", ignoreCase = true)) {
                                TransactionType.Transfer
                            } else {
                                t.type
                            }

                    current[i] =
                        t.copy(
                            categorySource = newCategory,
                            type = targetType,
                            isReviewed = true,
                            updatedAt = now,
                        )
                    updateCount++
                }
            }

            reconcileTransfers(current)
            val updated = current.sortedByDescending { it.dateTime }
            _transactions.value = updated
            saveToDisk(updated)
            updateCount
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
            val deduplicated = deduplicateList(list).toMutableList()
            reconcileTransfers(deduplicated)
            val sorted = deduplicated.sortedByDescending { it.dateTime }
            _transactions.value = sorted
            if (sorted.size < list.size) {
                saveToDisk(sorted)
            }
        } catch (_: Exception) {
            // Keep empty list if corrupted
        }
    }

    private fun isNearDuplicate(existing: Transaction, txn: Transaction): Boolean {
        if (existing.amount != txn.amount || existing.type != txn.type) return false
        val timeDiff = kotlin.math.abs(existing.dateTime - txn.dateTime)
        if (timeDiff > 180_000L) return false // 3 minutes window
        val sameMerchant =
            (!existing.merchantNormalised.isNullOrBlank() &&
                existing.merchantNormalised.equals(txn.merchantNormalised, ignoreCase = true)) ||
                (!existing.merchantRaw.isNullOrBlank() &&
                    existing.merchantRaw.equals(txn.merchantRaw, ignoreCase = true))
        val sameChannel = existing.channel != null && existing.channel == txn.channel
        return sameMerchant || sameChannel
    }

    private fun deduplicateList(transactions: List<Transaction>): List<Transaction> {
        val result = mutableListOf<Transaction>()
        val seenIds = mutableSetOf<Long>()
        val seenHashes = mutableSetOf<String>()
        val seenRefs = mutableSetOf<String>()

        for (txn in transactions) {
            val hasId = seenIds.contains(txn.id)
            val hasHash = txn.smsHash != null && seenHashes.contains(txn.smsHash)
            val ref = txn.refNumber?.trim()
            val hasRef = !ref.isNullOrEmpty() && seenRefs.contains(ref)
            val hasNear = result.any { isNearDuplicate(it, txn) }

            if (!hasId && !hasHash && !hasRef && !hasNear) {
                result.add(txn)
                seenIds.add(txn.id)
                txn.smsHash?.let { seenHashes.add(it) }
                if (!ref.isNullOrEmpty()) seenRefs.add(ref)
            }
        }
        return result
    }

    private fun reconcileTransfers(list: MutableList<Transaction>) {
        for (i in list.indices) {
            val t1 = list[i]
            if (t1.type != TransactionType.Expense) continue
            for (j in list.indices) {
                if (i == j) continue
                val t2 = list[j]
                if (t2.type != TransactionType.Income) continue
                if (t1.amount == t2.amount) {
                    val window =
                        if (t1.channel == Channel.CARD || t2.channel == Channel.CARD) {
                            7L * 24 * 60 * 60 * 1000L
                        } else {
                            10 * 60 * 1000L
                        }
                    if (kotlin.math.abs(t1.dateTime - t2.dateTime) <= window) {
                        list[i] =
                            t1.copy(type = TransactionType.Transfer, categorySource = "Transfer", isReviewed = true)
                        list[j] =
                            t2.copy(type = TransactionType.Transfer, categorySource = "Transfer", isReviewed = true)
                    }
                }
            }
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
