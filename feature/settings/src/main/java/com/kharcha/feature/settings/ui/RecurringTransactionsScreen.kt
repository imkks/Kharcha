package com.kharcha.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.RecurringFrequency
import com.kharcha.core.model.RecurringTransaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.settings.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreenView(
    recurringTransactions: List<RecurringTransaction>,
    categories: List<String>,
    onSave: (RecurringTransaction) -> Unit,
    onDelete: (Long) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<RecurringTransaction?>(null) }
    val active = recurringTransactions.filter { it.isActive }
    val paused = recurringTransactions.filterNot { it.isActive }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.recurring_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.recurring_back),
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { editing = RecurringTransaction(category = categories.firstOrNull() ?: "Others") }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.recurring_add),
                            tint = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = RecurringTransaction(category = categories.firstOrNull() ?: "Others") },
                containerColor = MaterialTheme.financialColors.limeAccent,
                contentColor = Color.Black,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.recurring_add))
            }
        },
    ) { padding ->
        if (recurringTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = DesignR.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.recurring_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.recurring_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (active.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.recurring_section_active),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(active, key = { it.id }) { item ->
                        RecurringItemCard(
                            item = item,
                            onEdit = { editing = item },
                            onToggleActive = { onToggleActive(item.id, it) },
                            onDelete = { onDelete(item.id) },
                        )
                    }
                }

                if (paused.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.recurring_section_paused),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(paused, key = { it.id }) { item ->
                        RecurringItemCard(
                            item = item,
                            onEdit = { editing = item },
                            onToggleActive = { onToggleActive(item.id, it) },
                            onDelete = { onDelete(item.id) },
                        )
                    }
                }
            }
        }
    }

    editing?.let { item ->
        RecurringEditorDialog(
            initial = item,
            categories = categories,
            onDismiss = { editing = null },
            onSave = {
                onSave(it)
                editing = null
            },
        )
    }
}

@Composable
private fun RecurringFrequency.label(): String = stringResource(
    when (this) {
        RecurringFrequency.DAILY -> R.string.recurring_frequency_daily
        RecurringFrequency.WEEKLY -> R.string.recurring_frequency_weekly
        RecurringFrequency.MONTHLY -> R.string.recurring_frequency_monthly
    },
)

@Composable
private fun RecurringItemCard(
    item: RecurringTransaction,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val nextDateStr = remember(item.nextDueDateEpoch) {
        val ldt = Instant.ofEpochMilli(item.nextDueDateEpoch).atZone(ZoneId.systemDefault()).toLocalDate()
        ldt.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.financialColors.cardBackground,
        ),
        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.merchantName.ifBlank { stringResource(R.string.recurring_untitled) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.recurring_item_schedule, item.frequency.label(), nextDateStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.financialColors.textSecondary,
                )
                if (item.category.isNotBlank()) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                }
            }

            Text(
                text = MoneyFormatter.formatMinorUnits(item.amountMinor),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (item.type == TransactionType.Income) {
                    MaterialTheme.financialColors.limeAccent
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.recurring_more_options),
                        tint = MaterialTheme.financialColors.textSecondary,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (item.isActive) stringResource(R.string.recurring_pause)
                                else stringResource(R.string.recurring_resume),
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleActive(!item.isActive)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recurring_edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.recurring_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        },
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.recurring_delete_title)) },
            text = { Text(stringResource(R.string.recurring_delete_message, item.merchantName)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(
                        stringResource(R.string.recurring_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.recurring_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEditorDialog(
    initial: RecurringTransaction,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (RecurringTransaction) -> Unit,
) {
    var merchantName by remember { mutableStateOf(initial.merchantName) }
    var amountInput by remember {
        mutableStateOf(
            if (initial.amountMinor > 0L) {
                val rupees = initial.amountMinor / 100L
                val paise = initial.amountMinor % 100L
                if (paise == 0L) "$rupees" else "$rupees.${String.format(Locale.ROOT, "%02d", paise)}"
            } else ""
        )
    }
    var type by remember { mutableStateOf(initial.type) }
    var category by remember { mutableStateOf(initial.category) }
    var frequency by remember { mutableStateOf(initial.frequency) }
    var dayOfMonth by remember { mutableStateOf(initial.dayOfMonth ?: 1) }
    var dayOfWeek by remember { mutableStateOf(initial.dayOfWeek ?: 1) }
    var note by remember { mutableStateOf(initial.note ?: "") }
    var isActive by remember { mutableStateOf(initial.isActive) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var dowExpanded by remember { mutableStateOf(false) }

    val locale = LocalConfiguration.current.locales[0]
    val dayOfWeekNames = remember(locale) {
        DayOfWeek.values().map { it.getDisplayName(TextStyle.FULL, locale) }
    }

    val parsedMinor = remember(amountInput) {
        MoneyFormatter.parseMinorUnits(amountInput) ?: 0L
    }
    val isValid = merchantName.isNotBlank() && parsedMinor > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial.id == 0L) stringResource(R.string.recurring_editor_new)
                else stringResource(R.string.recurring_editor_edit),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text(stringResource(R.string.recurring_field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            amountInput = filtered
                        }
                    },
                    label = { Text(stringResource(R.string.recurring_field_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = type == TransactionType.Expense,
                        onClick = { type = TransactionType.Expense },
                        label = { Text(stringResource(R.string.recurring_expense)) },
                    )
                    FilterChip(
                        selected = type == TransactionType.Income,
                        onClick = { type = TransactionType.Income },
                        label = { Text(stringResource(R.string.recurring_income)) },
                    )
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.recurring_field_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }

                // Frequency Dropdown
                ExposedDropdownMenuBox(
                    expanded = frequencyExpanded,
                    onExpandedChange = { frequencyExpanded = it },
                ) {
                    OutlinedTextField(
                        value = frequency.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.recurring_field_frequency)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(frequencyExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = frequencyExpanded,
                        onDismissRequest = { frequencyExpanded = false },
                    ) {
                        RecurringFrequency.values().forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.label()) },
                                onClick = {
                                    frequency = freq
                                    frequencyExpanded = false
                                },
                            )
                        }
                    }
                }

                when (frequency) {
                    RecurringFrequency.MONTHLY -> {
                        OutlinedTextField(
                            value = dayOfMonth.toString(),
                            onValueChange = { input ->
                                val n = input.filter { it.isDigit() }.toIntOrNull()
                                dayOfMonth = n?.coerceIn(1, 31) ?: 1
                            },
                            label = { Text(stringResource(R.string.recurring_field_day_of_month)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    RecurringFrequency.WEEKLY -> {
                        ExposedDropdownMenuBox(
                            expanded = dowExpanded,
                            onExpandedChange = { dowExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = dayOfWeekNames.getOrElse(dayOfWeek - 1) { "" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.recurring_field_day_of_week)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dowExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = dowExpanded,
                                onDismissRequest = { dowExpanded = false },
                            ) {
                                dayOfWeekNames.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            dayOfWeek = index + 1
                                            dowExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    RecurringFrequency.DAILY -> {}
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.recurring_field_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.recurring_field_active),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val candidate = initial.copy(
                        merchantName = merchantName.trim(),
                        amountMinor = parsedMinor,
                        type = type,
                        category = category,
                        frequency = frequency,
                        dayOfMonth = if (frequency == RecurringFrequency.MONTHLY) dayOfMonth else null,
                        dayOfWeek = if (frequency == RecurringFrequency.WEEKLY) dayOfWeek else null,
                        note = note.trim().ifBlank { null },
                        isActive = isActive,
                    )
                    val scheduleChanged = initial.id == 0L ||
                        initial.frequency != frequency ||
                        initial.dayOfMonth != candidate.dayOfMonth ||
                        initial.dayOfWeek != candidate.dayOfWeek
                    val nextDueEpoch = if (scheduleChanged) {
                        candidate.computeInitialDueDate()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    } else {
                        initial.nextDueDateEpoch
                    }
                    onSave(candidate.copy(nextDueDateEpoch = nextDueEpoch))
                },
            ) {
                Text(stringResource(R.string.recurring_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.recurring_cancel))
            }
        },
    )
}
