package com.kharcha.core.common.statement

import android.content.Context
import android.net.Uri
import com.kharcha.core.common.AccountRepository
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.model.Account
import com.kharcha.core.model.Transaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate

class ImportStatementUseCase(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val preferences: UserPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    sealed interface PdfRead {
        data class Parsed(val transactions: List<Transaction>) : PdfRead
        data class Error(val message: String) : PdfRead
        data class PasswordRequired(val wrongPassword: Boolean) : PdfRead
    }

    suspend fun readPdf(uri: Uri, password: String? = null): PdfRead = withContext(ioDispatcher) {
        try {
            val text = PdfTextExtractor.extractText(context, uri, password)
            val parser = PdfParserFactory.getParser(text)
                ?: return@withContext PdfRead.Error("Unsupported statement format. Supported: Google Pay, PhonePe, Paytm, Slice, ICICI Bank.")
            val parsed = parser.parse(text)
            if (parsed.isEmpty()) {
                PdfRead.Error("No transactions found in this statement.")
            } else {
                PdfRead.Parsed(parsed)
            }
        } catch (e: PdfPasswordRequiredException) {
            PdfRead.PasswordRequired(e.wrongPassword)
        } catch (_: Exception) {
            PdfRead.Error("Could not read this PDF statement.")
        }
    }

    suspend fun readTable(uri: Uri): List<List<String>> = withContext(ioDispatcher) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readNBytesCompat(MAX_FILE_BYTES + 1).takeIf { it.size <= MAX_FILE_BYTES }
                ?: throw IllegalArgumentException("This file is too large to import.")
        } ?: throw IllegalArgumentException("Could not read this file.")

        val rows = try {
            StatementTableParser.readTable(bytes)
        } catch (_: StatementTableParser.LegacyXlsException) {
            throw IllegalArgumentException("Legacy .xls isn't supported. Save it as .xlsx or .csv and import that.")
        } catch (e: StatementTableParser.TooManyRowsException) {
            throw e
        } catch (e: XlsxReader.Date1904Exception) {
            throw e
        }

        if (rows.none { row -> row.any(String::isNotBlank) }) {
            throw IllegalArgumentException("This file has no rows.")
        }
        rows
    }

    fun savedMappings(): Map<String, StatementColumnMapping> =
        StatementTableParser.decodeMappings(preferences.statementColumnMappings)

    fun saveMapping(rows: List<List<String>>, mapping: StatementColumnMapping) {
        val signature = StatementTableParser.signature(rows.getOrNull(mapping.headerRow).orEmpty())
        if (signature.isEmpty()) return
        val updated = savedMappings() + (signature to mapping)
        preferences.statementColumnMappings = StatementTableParser.encodeMappings(updated)
    }

    fun accounts(): List<Account> = accountRepository.getAccountsSync().filter { !it.isArchived }

    suspend fun preview(parsed: List<Transaction>): StatementImportPlan = withContext(ioDispatcher) {
        StatementImportProcessor(repositoryStore()).plan(parsed)
    }

    suspend fun commit(parsed: List<Transaction>, keep: Set<Int>): StatementImportResult.Success =
        withContext(ioDispatcher) {
            val processor = StatementImportProcessor(repositoryStore())
            val plan = processor.plan(parsed)
            val excluded = plan.rows.indices.toSet() - keep
            processor.apply(plan, excluded)
        }

    fun resolveAccount(parsed: List<Transaction>): Account? {
        val bank = parsed.mapNotNull { it.bankName }.distinct().singleOrNull() ?: return null
        val last4 = parsed.mapNotNull { it.accountLast4 }.distinct().let { if (it.size > 1) null else it.singleOrNull() }
        val accs = accounts()
        return accs.firstOrNull { it.bankName.equals(bank, ignoreCase = true) && (last4 == null || it.last4 == last4) }
    }

    fun balanceCheck(parsed: List<Transaction>, account: Account?): StatementBalanceCheck? {
        account ?: return null
        val closing = StatementBalanceCheck.closingRow(parsed) ?: return null
        val asOf = StatementBalanceCheck.dateOf(closing)
        val statementBal = closing.balanceAfter ?: return null
        val appBal = account.currentBalance

        val recordedAfter = account.balanceUpdatedAt?.let {
            it > closing.dateTime
        } ?: false

        return StatementBalanceCheck(
            bankName = account.bankName ?: account.name,
            accountLast4 = account.last4 ?: "",
            accountLabel = account.name,
            currency = account.name.takeIf { false } ?: "INR",
            asOf = asOf,
            statementBalance = statementBal,
            appBalance = appBal,
            correction = StatementBalanceCheck.correctionFor(
                statementBalance = statementBal,
                appBalance = appBal,
                currentBalance = account.currentBalance,
                recordedAfterStatement = recordedAfter,
            ),
        )
    }

    suspend fun applyBalance(parsed: List<Transaction>, account: Account): StatementBalanceCheck? =
        withContext(ioDispatcher) {
            val check = balanceCheck(parsed, account) ?: return@withContext null
            val target = check.correction ?: return@withContext null
            val updated = account.copy(
                currentBalance = target,
                balanceUpdatedAt = System.currentTimeMillis(),
            )
            accountRepository.saveAccount(updated)
            check.copy(appBalance = target, correction = null)
        }

    private fun repositoryStore() = object : StatementImportProcessor.TransactionStore {
        override suspend fun getTransactionsByHashes(hashes: List<String>): List<Transaction> {
            val hashSet = hashes.toSet()
            return transactionRepository.getTransactionsSync().filter { it.smsHash in hashSet }
        }

        override suspend fun getTransactionsBetween(startEpochMs: Long, endEpochMs: Long): List<Transaction> {
            return transactionRepository.getTransactionsSync().filter { it.dateTime in startEpochMs..endEpochMs }
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            transactionRepository.updateTransaction(transaction)
        }

        override suspend fun insertTransactions(transactions: List<Transaction>) {
            transactionRepository.insertTransactions(transactions)
        }
    }

    companion object {
        private const val MAX_FILE_BYTES = 20 * 1024 * 1024

        private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (out.size() < limit) {
                val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        }
    }
}
