package com.kharcha.core.common

import android.content.Context
import android.net.Uri
import com.kharcha.core.model.Transaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ImportCsvUseCase(
    private val context: Context,
    private val importer: CsvTransactionImporter,
    private val transactionRepository: TransactionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    sealed class Result {
        data class Success(
            val imported: Int,
            val skippedDuplicate: Int,
            val failed: Int,
        ) : Result()

        data class Error(val message: String) : Result()
    }

    suspend fun execute(uri: Uri): Result = withContext(ioDispatcher) {
        try {
            val parseResult = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    importer.parse(reader)
                }
            } ?: return@withContext Result.Error("Failed to open the selected file")

            if (parseResult.transactions.isEmpty() && parseResult.failureReasons.isNotEmpty()) {
                return@withContext Result.Error(parseResult.failureReasons.first())
            }

            val existingTransactions = transactionRepository.getTransactionsSync()
            val existingHashes = existingTransactions.mapNotNull { it.smsHash }.toHashSet()
            val toInsert = mutableListOf<Transaction>()
            val seenHashes = HashSet<String>()
            var skippedDuplicate = 0

            for (txn in parseResult.transactions) {
                val hash = txn.smsHash ?: ""
                if (hash.isNotEmpty() && (!seenHashes.add(hash) || hash in existingHashes)) {
                    skippedDuplicate++
                    continue
                }
                toInsert.add(txn)
            }

            if (toInsert.isNotEmpty()) {
                transactionRepository.insertTransactions(toInsert)
            }

            Result.Success(
                imported = toInsert.size,
                skippedDuplicate = skippedDuplicate,
                failed = parseResult.failedCount,
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown import error")
        }
    }
}
