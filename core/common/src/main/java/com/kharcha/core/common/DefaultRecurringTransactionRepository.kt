package com.kharcha.core.common

import android.content.Context
import com.kharcha.core.model.Channel
import com.kharcha.core.model.RecurringFrequency
import com.kharcha.core.model.RecurringTransaction
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DefaultRecurringTransactionRepository(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RecurringTransactionRepository {

    private val _templates = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    private val file: File
        get() {
            val dir = try {
                context.filesDir
            } catch (_: Exception) {
                null
            } ?: File(System.getProperty("java.io.tmpdir") ?: ".")
            return File(dir, "recurring_transactions.json")
        }

    init {
        loadFromDisk()
    }

    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> = _templates.asStateFlow()

    override fun getRecurringTransactionsSync(): List<RecurringTransaction> = _templates.value

    override suspend fun saveRecurringTransaction(item: RecurringTransaction): Long {
        return withContext(ioDispatcher) {
            val current = _templates.value.toMutableList()
            val now = System.currentTimeMillis()
            val id = if (item.id == 0L) {
                (current.maxOfOrNull { it.id } ?: 0L) + 1L
            } else {
                item.id
            }
            val toSave = item.copy(
                id = id,
                updatedAt = now,
                createdAt = if (item.id == 0L) now else item.createdAt,
            )
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = toSave
            } else {
                current.add(toSave)
            }
            _templates.value = current
            saveToDisk(current)
            id
        }
    }

    override suspend fun deleteRecurringTransaction(id: Long) {
        withContext(ioDispatcher) {
            val current = _templates.value.filter { it.id != id }
            _templates.value = current
            saveToDisk(current)
        }
    }

    override suspend fun setActive(id: Long, isActive: Boolean) {
        withContext(ioDispatcher) {
            val current = _templates.value.toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = current[index].copy(isActive = isActive, updatedAt = System.currentTimeMillis())
                _templates.value = current
                saveToDisk(current)
            }
        }
    }

    override suspend fun materializeDue(today: LocalDate): Int {
        return withContext(ioDispatcher) {
            val zoneId = ZoneId.systemDefault()
            val current = _templates.value.toMutableList()
            var createdCount = 0

            for (i in current.indices) {
                val template = current[i]
                if (!template.isActive) continue

                val dueDate = Instant.ofEpochMilli(template.nextDueDateEpoch).atZone(zoneId).toLocalDate()
                if (!dueDate.isAfter(today)) {
                    val txn = Transaction(
                        amount = template.amountMinor,
                        type = template.type,
                        merchantNormalised = template.merchantName,
                        merchantRaw = template.merchantName,
                        categorySource = template.category,
                        dateTime = template.nextDueDateEpoch,
                        channel = Channel.UPI,
                        bankName = template.bankName,
                        accountLast4 = template.accountLast4,
                        note = template.note ?: "Recurring: ${template.merchantName}",
                        smsHash = "rec_${template.id}_${template.nextDueDateEpoch}",
                        isReviewed = true,
                        isCounted = true,
                    )
                    transactionRepository.insertTransaction(txn)
                    createdCount++

                    var nextDate = template.nextDueAfter(dueDate)
                    while (!nextDate.isAfter(today)) {
                        nextDate = template.nextDueAfter(nextDate)
                    }
                    val nextEpoch = nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    current[i] = template.copy(
                        nextDueDateEpoch = nextEpoch,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
            }

            if (createdCount > 0) {
                _templates.value = current
                saveToDisk(current)
            }
            createdCount
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadFromDisk() {
        try {
            if (!file.exists()) return
            val jsonStr = file.readText()
            if (jsonStr.isBlank()) return
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<RecurringTransaction>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(jsonToTemplate(obj))
            }
            _templates.value = list
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun saveToDisk(list: List<RecurringTransaction>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                jsonArray.put(templateToJson(item))
            }
            file.writeText(jsonArray.toString())
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    private fun templateToJson(item: RecurringTransaction): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("uid", item.uid)
        obj.put("merchantName", item.merchantName)
        obj.put("amountMinor", item.amountMinor)
        obj.put("currency", item.currency)
        obj.put("category", item.category)
        obj.put("type", item.type.name)
        obj.put("frequency", item.frequency.name)
        item.dayOfMonth?.let { obj.put("dayOfMonth", it) }
        item.dayOfWeek?.let { obj.put("dayOfWeek", it) }
        obj.put("nextDueDateEpoch", item.nextDueDateEpoch)
        obj.put("isActive", item.isActive)
        item.accountLast4?.let { obj.put("accountLast4", it) }
        item.bankName?.let { obj.put("bankName", it) }
        item.note?.let { obj.put("note", it) }
        obj.put("createdAt", item.createdAt)
        obj.put("updatedAt", item.updatedAt)
        return obj
    }

    private fun jsonToTemplate(obj: JSONObject): RecurringTransaction {
        return RecurringTransaction(
            id = obj.optLong("id", 0L),
            uid = obj.optString("uid", ""),
            merchantName = obj.optString("merchantName", ""),
            amountMinor = obj.optLong("amountMinor", 0L),
            currency = obj.optString("currency", "INR"),
            category = obj.optString("category", "Others"),
            type = try {
                TransactionType.valueOf(obj.optString("type", "Expense"))
            } catch (_: Exception) {
                TransactionType.Expense
            },
            frequency = try {
                RecurringFrequency.valueOf(obj.optString("frequency", "MONTHLY"))
            } catch (_: Exception) {
                RecurringFrequency.MONTHLY
            },
            dayOfMonth = if (obj.has("dayOfMonth")) obj.getInt("dayOfMonth") else null,
            dayOfWeek = if (obj.has("dayOfWeek")) obj.getInt("dayOfWeek") else null,
            nextDueDateEpoch = obj.optLong("nextDueDateEpoch", System.currentTimeMillis()),
            isActive = obj.optBoolean("isActive", true),
            accountLast4 = if (obj.has("accountLast4")) obj.getString("accountLast4") else null,
            bankName = if (obj.has("bankName")) obj.getString("bankName") else null,
            note = if (obj.has("note")) obj.getString("note") else null,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
        )
    }
}
