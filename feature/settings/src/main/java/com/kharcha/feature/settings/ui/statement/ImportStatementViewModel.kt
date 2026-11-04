package com.kharcha.feature.settings.ui.statement

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kharcha.core.common.statement.ImportStatementUseCase
import com.kharcha.core.common.statement.StatementAccount
import com.kharcha.core.common.statement.StatementBalanceCheck
import com.kharcha.core.common.statement.StatementColumnMapping
import com.kharcha.core.common.statement.StatementImportPlan
import com.kharcha.core.common.statement.StatementImportResult
import com.kharcha.core.common.statement.StatementTableParser
import com.kharcha.core.model.Account
import com.kharcha.core.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportStatementUiState {
    data object Idle : ImportStatementUiState()
    data object Loading : ImportStatementUiState()
    data class PasswordRequired(val uri: Uri, val wrongPassword: Boolean) : ImportStatementUiState()

    data class Mapping(
        val rows: List<List<String>>,
        val mapping: StatementColumnMapping,
        val accounts: List<Account>,
        val sample: List<Transaction>,
        val parsedCount: Int,
        val skippedRows: Int,
        val detectedDateFormat: String?,
    ) : ImportStatementUiState()

    data class Preview(
        val plan: StatementImportPlan,
        val excluded: Set<Int> = emptySet(),
        val accountLabel: String?,
        val fromSpreadsheet: Boolean,
    ) : ImportStatementUiState()

    data class Success(
        val result: StatementImportResult.Success,
        val balanceCheck: StatementBalanceCheck? = null,
        val balanceApplied: Boolean = false,
    ) : ImportStatementUiState()

    data class Error(val message: String) : ImportStatementUiState()
}

class ImportStatementViewModel(
    private val importStatementUseCase: ImportStatementUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportStatementUiState>(ImportStatementUiState.Idle)
    val uiState: StateFlow<ImportStatementUiState> = _uiState.asStateFlow()

    private var pending: Pending? = null
    private var mappingJob: Job? = null

    private data class Pending(
        val parsed: List<Transaction>,
        val account: Account?,
        val table: List<List<String>>? = null,
        val mapping: StatementColumnMapping? = null,
    )

    fun importStatement(uri: Uri, password: String? = null) {
        _uiState.value = ImportStatementUiState.Loading
        viewModelScope.launch {
            when (val read = importStatementUseCase.readPdf(uri, password)) {
                is ImportStatementUseCase.PdfRead.Parsed -> {
                    val account = importStatementUseCase.resolveAccount(read.transactions)
                    showPreview(Pending(read.transactions, account))
                }
                is ImportStatementUseCase.PdfRead.Error ->
                    _uiState.value = ImportStatementUiState.Error(read.message)
                is ImportStatementUseCase.PdfRead.PasswordRequired ->
                    _uiState.value = ImportStatementUiState.PasswordRequired(uri, read.wrongPassword)
            }
        }
    }

    fun importSpreadsheet(uri: Uri) {
        _uiState.value = ImportStatementUiState.Loading
        viewModelScope.launch {
            try {
                val rows = importStatementUseCase.readTable(uri)
                val accounts = importStatementUseCase.accounts()
                val saved = StatementTableParser.findSavedMapping(rows, importStatementUseCase.savedMappings())
                if (saved != null && previewSpreadsheet(rows, saved, accounts)) {
                    return@launch
                }
                showMapping(rows, saved ?: StatementTableParser.guessMapping(rows), accounts)
            } catch (e: Exception) {
                _uiState.value = ImportStatementUiState.Error(e.message ?: "Failed to read spreadsheet.")
            }
        }
    }

    fun updateMapping(mapping: StatementColumnMapping) {
        val state = _uiState.value as? ImportStatementUiState.Mapping ?: return
        _uiState.value = state.copy(mapping = mapping)
        mappingJob?.cancel()
        mappingJob = viewModelScope.launch { showMapping(state.rows, mapping, state.accounts) }
    }

    fun selectHeaderRow(row: Int) {
        val state = _uiState.value as? ImportStatementUiState.Mapping ?: return
        val guessed = StatementTableParser.guessMapping(state.rows, row.coerceIn(0, state.rows.lastIndex))
        updateMapping(guessed.copy(accountBankName = state.mapping.accountBankName, accountLast4 = state.mapping.accountLast4))
    }

    fun confirmMapping() {
        val state = _uiState.value as? ImportStatementUiState.Mapping ?: return
        mappingJob?.cancel()
        _uiState.value = ImportStatementUiState.Loading
        viewModelScope.launch {
            importStatementUseCase.saveMapping(state.rows, state.mapping)
            if (!previewSpreadsheet(state.rows, state.mapping, state.accounts)) {
                showMapping(state.rows, state.mapping, state.accounts)
            }
        }
    }

    fun editMapping() {
        val p = pending ?: return
        val rows = p.table ?: return
        val mapping = p.mapping ?: return
        viewModelScope.launch { showMapping(rows, mapping, importStatementUseCase.accounts()) }
    }

    fun toggleRow(index: Int) {
        val state = _uiState.value as? ImportStatementUiState.Preview ?: return
        _uiState.value = state.copy(
            excluded = if (index in state.excluded) state.excluded - index else state.excluded + index,
        )
    }

    fun confirmImport() {
        val state = _uiState.value as? ImportStatementUiState.Preview ?: return
        val p = pending ?: return
        _uiState.value = ImportStatementUiState.Loading
        viewModelScope.launch {
            try {
                val keep = state.plan.newIndices.toSet() - state.excluded
                val result = importStatementUseCase.commit(p.parsed, keep)
                val check = importStatementUseCase.balanceCheck(p.parsed, p.account)
                _uiState.value = ImportStatementUiState.Success(result, check)
            } catch (e: Exception) {
                _uiState.value = ImportStatementUiState.Error(e.message ?: "Failed to import statement.")
            }
        }
    }

    fun applyStatementBalance() {
        val state = _uiState.value as? ImportStatementUiState.Success ?: return
        val p = pending ?: return
        val account = p.account ?: return
        viewModelScope.launch {
            val applied = importStatementUseCase.applyBalance(p.parsed, account)
            if (applied != null) {
                _uiState.value = state.copy(balanceCheck = applied, balanceApplied = true)
            }
        }
    }

    private suspend fun showMapping(rows: List<List<String>>, mapping: StatementColumnMapping, accounts: List<Account>) {
        val target = statementAccount(mapping, accounts)
        val (parsed, dateFormat) = withContext(Dispatchers.Default) {
            val parsed = if (mapping.isComplete) {
                runCatching { StatementTableParser.parse(rows, mapping, target) }.getOrNull()
            } else {
                null
            }
            val format = if (mapping.dateColumn >= 0) {
                runCatching { StatementTableParser.effectiveDateFormat(rows, mapping) }.getOrNull()
            } else {
                null
            }
            parsed to format
        }
        currentCoroutineContext().ensureActive()
        _uiState.value = ImportStatementUiState.Mapping(
            rows = rows,
            mapping = mapping,
            accounts = accounts,
            sample = parsed?.transactions?.take(SAMPLE_ROWS).orEmpty(),
            parsedCount = parsed?.transactions?.size ?: 0,
            skippedRows = parsed?.skippedRows ?: 0,
            detectedDateFormat = dateFormat,
        )
    }

    private suspend fun previewSpreadsheet(
        rows: List<List<String>>,
        mapping: StatementColumnMapping,
        accounts: List<Account>,
    ): Boolean {
        if (!mapping.isComplete) return false
        val target = statementAccount(mapping, accounts)
        val parsed = withContext(Dispatchers.Default) {
            runCatching { StatementTableParser.parse(rows, mapping, target).transactions }.getOrDefault(emptyList())
        }
        if (parsed.isEmpty()) return false
        val account = accounts.firstOrNull { it.bankName == mapping.accountBankName && it.last4 == mapping.accountLast4 }
        showPreview(Pending(parsed, account, rows, mapping))
        return true
    }

    private suspend fun showPreview(p: Pending) {
        pending = p
        _uiState.value = ImportStatementUiState.Preview(
            plan = importStatementUseCase.preview(p.parsed),
            accountLabel = p.account?.name,
            fromSpreadsheet = p.table != null,
        )
    }

    private fun statementAccount(mapping: StatementColumnMapping, accounts: List<Account>): StatementAccount {
        val account = accounts.firstOrNull { it.bankName == mapping.accountBankName && it.last4 == mapping.accountLast4 }
        return if (account != null) {
            StatementAccount(account.bankName ?: account.name, account.last4, "INR")
        } else {
            StatementAccount(UNLINKED_BANK, null, "INR")
        }
    }

    fun resetState() {
        mappingJob?.cancel()
        pending = null
        _uiState.value = ImportStatementUiState.Idle
    }

    companion object {
        private const val SAMPLE_ROWS = 5
        private const val UNLINKED_BANK = "Statement"

        fun factory(useCase: ImportStatementUseCase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ImportStatementViewModel(useCase) as T
                }
            }
    }
}
