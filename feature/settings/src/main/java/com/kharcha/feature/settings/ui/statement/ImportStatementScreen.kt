package com.kharcha.feature.settings.ui.statement

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.statement.StatementBalanceCheck
import com.kharcha.core.common.statement.StatementColumnMapping
import com.kharcha.core.common.statement.StatementImportResult
import com.kharcha.core.common.statement.StatementTableParser
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.settings.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SPREADSHEET_TYPES = arrayOf(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    "text/csv",
    "text/comma-separated-values",
    "application/csv",
    "text/plain",
)

private val ROW_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStatementScreen(
    viewModel: ImportStatementViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { viewModel.importStatement(it) } },
    )
    val spreadsheetPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.importSpreadsheet(it) } },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_statement_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.import_statement_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val current = uiState
        if (current is ImportStatementUiState.Preview) {
            PreviewContent(
                state = current,
                onToggle = viewModel::toggleRow,
                onConfirm = viewModel::confirmImport,
                onEditMapping = viewModel::editMapping,
                onCancel = viewModel::resetState,
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (val state = current) {
                    is ImportStatementUiState.Idle -> IdleContent(
                        onSelectPdf = { pdfPicker.launch("application/pdf") },
                        onSelectSpreadsheet = { spreadsheetPicker.launch(SPREADSHEET_TYPES) },
                    )
                    is ImportStatementUiState.Loading -> LoadingContent()
                    is ImportStatementUiState.Mapping -> MappingContent(
                        state = state,
                        onMappingChange = viewModel::updateMapping,
                        onHeaderRow = viewModel::selectHeaderRow,
                        onConfirm = viewModel::confirmMapping,
                        onCancel = viewModel::resetState,
                    )
                    is ImportStatementUiState.Preview -> Unit
                    is ImportStatementUiState.Success -> SuccessContent(
                        result = state.result,
                        balanceCheck = state.balanceCheck,
                        balanceApplied = state.balanceApplied,
                        onApplyBalance = viewModel::applyStatementBalance,
                        onImportAnother = viewModel::resetState,
                        onDone = onNavigateBack,
                    )
                    is ImportStatementUiState.PasswordRequired -> {
                        IdleContent(
                            onSelectPdf = { pdfPicker.launch("application/pdf") },
                            onSelectSpreadsheet = { spreadsheetPicker.launch(SPREADSHEET_TYPES) },
                        )
                        PdfPasswordDialog(
                            wrongPassword = state.wrongPassword,
                            onSubmit = { password -> viewModel.importStatement(state.uri, password) },
                            onDismiss = viewModel::resetState,
                        )
                    }
                    is ImportStatementUiState.Error -> ErrorContent(
                        message = state.message,
                        onTryAgain = viewModel::resetState,
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onSelectPdf: () -> Unit, onSelectSpreadsheet: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))

    Icon(
        painter = painterResource(DesignR.drawable.ic_receipt_photo),
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.import_statement_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Text(
        text = stringResource(R.string.import_statement_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onSelectPdf,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_bank),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.import_statement_select_pdf))
    }

    FilledTonalButton(
        onClick = onSelectSpreadsheet,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_copy),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.import_statement_select_spreadsheet))
    }

    Text(
        text = stringResource(R.string.import_statement_xls_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LoadingContent() {
    Spacer(modifier = Modifier.height(48.dp))

    CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        strokeWidth = 4.dp,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.import_statement_loading_title),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )

    Text(
        text = stringResource(R.string.import_statement_loading_body),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MappingContent(
    state: ImportStatementUiState.Mapping,
    onMappingChange: (StatementColumnMapping) -> Unit,
    onHeaderRow: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val mapping = state.mapping
    val header = state.rows.getOrNull(mapping.headerRow).orEmpty()
    val columnCount = state.rows.drop(mapping.headerRow).take(6).maxOfOrNull { it.size } ?: 0
    val columns = (0 until columnCount).map { i ->
        val letter = columnLetter(i)
        val name = header.getOrNull(i)?.trim().orEmpty()
        i to if (name.isEmpty()) stringResource(R.string.import_statement_column_label, letter) else "$letter · $name"
    }
    val none = StatementColumnMapping.NONE to stringResource(R.string.import_statement_col_none)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.import_statement_map_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.import_statement_map_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.import_statement_header_row, mapping.headerRow + 1),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onHeaderRow(mapping.headerRow - 1) }, enabled = mapping.headerRow > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.import_statement_header_row_up))
                    }
                    IconButton(
                        onClick = { onHeaderRow(mapping.headerRow + 1) },
                        enabled = mapping.headerRow < state.rows.lastIndex,
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.import_statement_header_row_down))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TablePreview(
                    rows = state.rows.drop(mapping.headerRow).take(6),
                    columnCount = columnCount,
                    serialDateColumn = mapping.dateColumn.takeIf { state.detectedDateFormat == StatementTableParser.EXCEL_SERIAL },
                )
            }
        }

        ColumnPicker(stringResource(R.string.import_statement_col_date), listOf(none) + columns, mapping.dateColumn) {
            onMappingChange(mapping.copy(dateColumn = it))
        }

        val detected = state.detectedDateFormat?.takeIf { mapping.dateFormat == null }
            ?.let { if (it == StatementTableParser.EXCEL_SERIAL) stringResource(R.string.import_statement_date_excel) else it }
        val autoLabel = detected?.let { stringResource(R.string.import_statement_date_auto_found, it) }
            ?: stringResource(R.string.import_statement_date_auto)
        val formats = listOf<Pair<String?, String>>(null to autoLabel) + StatementTableParser.DATE_FORMATS.map { it to it }
        Picker(stringResource(R.string.import_statement_col_date_format), formats, mapping.dateFormat) {
            onMappingChange(mapping.copy(dateFormat = it))
        }

        ColumnPicker(stringResource(R.string.import_statement_col_description), listOf(none) + columns, mapping.descriptionColumn) {
            onMappingChange(mapping.copy(descriptionColumn = it))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mapping.usesDebitCredit,
                onClick = {
                    if (!mapping.usesDebitCredit) {
                        onMappingChange(
                            mapping.copy(
                                amountColumn = StatementColumnMapping.NONE,
                                typeColumn = StatementColumnMapping.NONE,
                                debitColumn = mapping.amountColumn.takeIf { it >= 0 } ?: 0,
                            ),
                        )
                    }
                },
                label = { Text(stringResource(R.string.import_statement_amount_split)) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = !mapping.usesDebitCredit,
                onClick = {
                    if (mapping.usesDebitCredit) {
                        onMappingChange(
                            mapping.copy(
                                amountColumn = mapping.debitColumn,
                                debitColumn = StatementColumnMapping.NONE,
                                creditColumn = StatementColumnMapping.NONE,
                            ),
                        )
                    }
                },
                label = { Text(stringResource(R.string.import_statement_amount_single)) },
                modifier = Modifier.weight(1f),
            )
        }

        if (mapping.usesDebitCredit) {
            ColumnPicker(stringResource(R.string.import_statement_col_debit), listOf(none) + columns, mapping.debitColumn) {
                onMappingChange(mapping.copy(debitColumn = it))
            }
            ColumnPicker(stringResource(R.string.import_statement_col_credit), listOf(none) + columns, mapping.creditColumn) {
                onMappingChange(mapping.copy(creditColumn = it))
            }
        } else {
            ColumnPicker(stringResource(R.string.import_statement_col_amount), listOf(none) + columns, mapping.amountColumn) {
                onMappingChange(mapping.copy(amountColumn = it))
            }
            ColumnPicker(stringResource(R.string.import_statement_col_type), listOf(none) + columns, mapping.typeColumn) {
                onMappingChange(mapping.copy(typeColumn = it))
            }
        }

        ColumnPicker(stringResource(R.string.import_statement_col_balance), listOf(none) + columns, mapping.balanceColumn) {
            onMappingChange(mapping.copy(balanceColumn = it))
        }
        ColumnPicker(stringResource(R.string.import_statement_col_reference), listOf(none) + columns, mapping.referenceColumn) {
            onMappingChange(mapping.copy(referenceColumn = it))
        }

        val accountOptions = listOf<Pair<Pair<String, String>?, String>>(null to stringResource(R.string.import_statement_account_none)) +
            state.accounts.map { (it.bankName.orEmpty() to it.last4.orEmpty()) to it.name }
        val selectedAccount = mapping.accountBankName?.let { bank -> mapping.accountLast4?.let { bank to it } }
        Picker(stringResource(R.string.import_statement_account), accountOptions, selectedAccount) {
            onMappingChange(mapping.copy(accountBankName = it?.first, accountLast4 = it?.second))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.import_statement_sample_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                if (state.parsedCount == 0) {
                    Text(
                        text = stringResource(R.string.import_statement_sample_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.import_statement_sample_count, state.parsedCount, state.skippedRows),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    state.sample.forEach { TransactionLine(it) }
                }
            }
        }

        Button(
            onClick = onConfirm,
            enabled = state.parsedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.import_statement_preview_button))
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.import_statement_cancel))
        }
    }
}

@Composable
private fun TablePreview(rows: List<List<String>>, columnCount: Int, serialDateColumn: Int?) {
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Row {
            (0 until columnCount).forEach { i ->
                Text(
                    text = columnLetter(i),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(100.dp)
                        .padding(end = 8.dp),
                )
            }
        }
        rows.forEachIndexed { r, row ->
            Row(modifier = Modifier.padding(top = 4.dp)) {
                (0 until columnCount).forEach { i ->
                    val raw = row.getOrNull(i).orEmpty()
                    val serialDate = if (r > 0 && i == serialDateColumn) {
                        StatementTableParser.parseDate(raw, StatementTableParser.EXCEL_SERIAL)
                    } else {
                        null
                    }
                    Text(
                        text = serialDate?.format(ROW_DATE) ?: raw,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (r == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (r == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnPicker(label: String, options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) =
    Picker(label, options, selected, onSelect)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Picker(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewContent(
    state: ImportStatementUiState.Preview,
    onToggle: (Int) -> Unit,
    onConfirm: () -> Unit,
    onEditMapping: () -> Unit,
    onCancel: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val newIndices = state.plan.newIndices
    val selected = newIndices.count { it !in state.excluded }

    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.import_statement_preview_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                state.accountLabel?.let {
                    Text(
                        text = stringResource(R.string.import_statement_preview_account, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountTile(
                        value = newIndices.size,
                        label = stringResource(R.string.import_statement_preview_new),
                        highlight = true,
                        modifier = Modifier.weight(1f),
                    )
                    CountTile(
                        value = state.plan.matchedCount,
                        label = stringResource(R.string.import_statement_preview_matched),
                        highlight = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (newIndices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.import_statement_preview_all_matched),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column {
                        Text(
                            text = stringResource(R.string.import_statement_preview_new_rows),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.import_statement_preview_new_rows_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        itemsIndexed(newIndices, key = { _, index -> index }) { _, index ->
            val checked = index !in state.excluded
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle(index) }),
            ) {
                Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(horizontal = 8.dp))
                TransactionLine(state.plan.rows[index].parsed, modifier = Modifier.weight(1f).padding(end = 8.dp))
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        if (selected > 0) {
                            pluralStringResource(R.plurals.import_statement_confirm, selected, selected)
                        } else {
                            stringResource(R.string.import_statement_confirm_nothing)
                        },
                    )
                }
                if (state.fromSpreadsheet) {
                    TextButton(onClick = onEditMapping, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_statement_edit_mapping))
                    }
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.import_statement_cancel))
                }
            }
        }
    }
}

@Composable
private fun CountTile(value: Int, label: String, highlight: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransactionLine(txn: Transaction, modifier: Modifier = Modifier) {
    val income = txn.type == TransactionType.Income
    val date = StatementBalanceCheck.dateOf(txn)
    Row(modifier = modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.merchantNormalised ?: txn.merchantRaw ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = date.format(ROW_DATE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        val color = if (income) MaterialTheme.financialColors.income else MaterialTheme.colorScheme.onSurface
        Text(
            text = (if (income) "+" else "−") + MoneyFormatter.formatMinorUnits(txn.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun SuccessContent(
    result: StatementImportResult.Success,
    balanceCheck: StatementBalanceCheck?,
    balanceApplied: Boolean,
    onApplyBalance: () -> Unit,
    onImportAnother: () -> Unit,
    onDone: () -> Unit,
) {
    Spacer(modifier = Modifier.height(24.dp))

    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.import_statement_complete),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResultRow(
                label = stringResource(R.string.import_statement_result_imported),
                value = "${result.imported}",
                isHighlighted = true,
            )

            if (result.enriched > 0) {
                ResultRow(
                    label = stringResource(R.string.import_statement_result_enriched),
                    value = "${result.enriched}",
                    isHighlighted = true,
                )
            }

            ResultRow(
                label = stringResource(R.string.import_statement_result_total_parsed),
                value = "${result.totalParsed}",
            )

            if (result.skippedDuplicates > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.import_statement_result_duplicates, result.skippedDuplicates),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (result.skippedByHash > 0) {
                    ResultRow(
                        label = stringResource(R.string.import_statement_result_exact),
                        value = "${result.skippedByHash}",
                        indent = true,
                    )
                }
                if (result.skippedByReference > 0) {
                    ResultRow(
                        label = stringResource(R.string.import_statement_result_by_reference),
                        value = "${result.skippedByReference}",
                        indent = true,
                    )
                }
                if (result.skippedByAmountDate > 0) {
                    ResultRow(
                        label = stringResource(R.string.import_statement_result_by_amount_date),
                        value = "${result.skippedByAmountDate}",
                        indent = true,
                    )
                }
            }
        }
    }

    balanceCheck?.let { BalanceCheckCard(it, balanceApplied, onApplyBalance) }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onImportAnother,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.import_statement_import_another))
    }

    OutlinedButton(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(stringResource(R.string.import_statement_done))
    }
}

@Composable
private fun BalanceCheckCard(check: StatementBalanceCheck, applied: Boolean, onApply: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.import_statement_balance_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.import_statement_balance_subtitle, check.accountLabel, check.asOf.format(ROW_DATE)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ResultRow(stringResource(R.string.import_statement_balance_statement), MoneyFormatter.formatMinorUnits(check.statementBalance))
            ResultRow(
                stringResource(R.string.import_statement_balance_app),
                check.appBalance?.let { MoneyFormatter.formatMinorUnits(it) } ?: stringResource(R.string.import_statement_balance_unknown),
            )
            check.difference?.let { diff ->
                ResultRow(
                    label = stringResource(R.string.import_statement_balance_difference),
                    value = (if (diff > 0) "+" else if (diff < 0) "−" else "") + MoneyFormatter.formatMinorUnits(kotlin.math.abs(diff)),
                    isHighlighted = diff != 0L,
                )
            }
            Spacer(Modifier.height(8.dp))
            val corrected = check.correction
            when {
                applied && corrected != null -> Text(
                    text = stringResource(R.string.import_statement_balance_applied, MoneyFormatter.formatMinorUnits(corrected)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.financialColors.income,
                )
                check.matches -> Text(
                    text = stringResource(R.string.import_statement_balance_match),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.financialColors.income,
                )
                corrected == null -> Text(
                    text = if (check.difference != null) {
                        stringResource(R.string.import_statement_balance_newer)
                    } else {
                        stringResource(R.string.import_statement_balance_unknown_note, check.asOf.format(ROW_DATE))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    FilledTonalButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_statement_balance_set, MoneyFormatter.formatMinorUnits(corrected)))
                    }
                    if (corrected != check.statementBalance) {
                        Text(
                            text = stringResource(R.string.import_statement_balance_set_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    indent: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (indent) Modifier.padding(start = 16.dp) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (isHighlighted) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
        )
        Text(
            text = value,
            style = if (isHighlighted) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onTryAgain: () -> Unit,
) {
    Spacer(modifier = Modifier.height(48.dp))

    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.error,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.import_statement_failed),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onTryAgain,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.import_statement_try_again))
    }
}

@Composable
private fun PdfPasswordDialog(
    wrongPassword: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_statement_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.import_statement_password_body))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.import_statement_password_label)) },
                    singleLine = true,
                    isError = wrongPassword,
                    supportingText = if (wrongPassword) {
                        { Text(stringResource(R.string.import_statement_password_wrong)) }
                    } else {
                        null
                    },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) {
                Text(stringResource(R.string.import_statement_password_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.import_statement_password_cancel)) }
        },
    )
}

private fun columnLetter(index: Int): String {
    var n = index + 1
    val sb = StringBuilder()
    while (n > 0) {
        val rem = (n - 1) % 26
        sb.insert(0, 'A' + rem)
        n = (n - 1) / 26
    }
    return sb.toString()
}
