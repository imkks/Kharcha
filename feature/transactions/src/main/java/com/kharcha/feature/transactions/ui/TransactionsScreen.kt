package com.kharcha.feature.transactions.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionFilterMatcher
import com.kharcha.core.common.TransactionSearchHelper
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.SwipeableTransactionRow
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.model.AmountRangeFilter
import com.kharcha.core.model.DateRangeSelection
import com.kharcha.core.model.ReviewStatusFilter
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

enum class TransactionFilterType {
    ALL,
    EXPENSE,
    INCOME,
    TRANSFER,
    NEEDS_REVIEW,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    onPrivacyToggle: () -> Unit = {},
    initialCategory: String? = null,
    onClearInitialCategory: () -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit = {},
    onRestoreTransaction: (Transaction) -> Unit = {},
    onReviewTransaction: (Transaction) -> Unit = {},
    onUpdateTransactions: (List<Transaction>) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentFilter by remember {
        mutableStateOf(
            if (initialCategory != null) {
                TransactionFilter(categories = setOf(initialCategory))
            } else {
                TransactionFilter()
            },
        )
    }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var showBulkCategorizeSheet by remember { mutableStateOf(false) }
    var showBulkTypeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
    }

    LaunchedEffect(initialCategory) {
        if (initialCategory != null) {
            currentFilter = currentFilter.copy(categories = setOf(initialCategory))
            onClearInitialCategory()
        }
    }

    val activeFilter =
        remember(currentFilter, searchQuery) {
            currentFilter.copy(query = searchQuery)
        }

    val filteredTransactions =
        remember(transactions, activeFilter) {
            transactions.filter { TransactionFilterMatcher.matches(it, activeFilter) }
        }

    val totalIncome =
        remember(filteredTransactions) {
            filteredTransactions
                .filter { it.type == TransactionType.Income }
                .sumOf { it.amount }
        }

    val totalExpense =
        remember(filteredTransactions) {
            filteredTransactions
                .filter { it.type == TransactionType.Expense }
                .sumOf { it.amount }
        }

    val netTotal = totalIncome - totalExpense

    val groupedByDay =
        remember(filteredTransactions) {
            groupTransactionsByDay(filteredTransactions)
        }

    val merchantSuggestions =
        remember(transactions, searchQuery) {
            if (searchQuery.isNotBlank()) {
                TransactionSearchHelper.extractMerchantSuggestions(transactions, searchQuery)
            } else {
                emptyList()
            }
        }

    val todayStr = stringResource(id = R.string.transactions_today)
    val yesterdayStr = stringResource(id = R.string.transactions_yesterday)

    val selectedQuickFilter =
        when {
            activeFilter.reviewStatus == ReviewStatusFilter.NeedsReviewOnly -> TransactionFilterType.NEEDS_REVIEW
            activeFilter.types == setOf(TransactionType.Expense) -> TransactionFilterType.EXPENSE
            activeFilter.types == setOf(TransactionType.Income) -> TransactionFilterType.INCOME
            activeFilter.types == setOf(TransactionType.Transfer) -> TransactionFilterType.TRANSFER
            activeFilter.types.isEmpty() && activeFilter.reviewStatus == ReviewStatusFilter.All ->
                TransactionFilterType.ALL
            else -> null
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedIds.isNotEmpty()) {
                val allVisibleSelected =
                    filteredTransactions.isNotEmpty() &&
                        filteredTransactions.all { selectedIds.contains(it.id) }
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.multiselect_deselect_all),
                            )
                        }
                    },
                    title = {
                        Text(
                            text =
                                stringResource(
                                    id = R.string.multiselect_selected_count,
                                    selectedIds.size,
                                ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                selectedIds =
                                    if (allVisibleSelected) {
                                        emptySet()
                                    } else {
                                        filteredTransactions.map { it.id }.toSet()
                                    }
                            },
                        ) {
                            Text(
                                text =
                                    stringResource(
                                        id =
                                            if (allVisibleSelected) {
                                                R.string.multiselect_deselect_all
                                            } else {
                                                R.string.multiselect_select_all
                                            },
                                    ),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                )
            } else {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.transactions_title)) },
                    actions = {
                        val isPrivacy = LocalPrivacyMode.current
                        IconButton(onClick = onPrivacyToggle) {
                            Icon(
                                painter =
                                    painterResource(
                                        id =
                                            if (isPrivacy) {
                                                DesignR.drawable.ic_visibility_off
                                            } else {
                                                DesignR.drawable.ic_visibility
                                            },
                                    ),
                                contentDescription =
                                    stringResource(
                                        id =
                                            if (isPrivacy) {
                                                DesignR.string.action_show_amounts
                                            } else {
                                                DesignR.string.action_hide_amounts
                                            },
                                    ),
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )
            }
        },
        bottomBar = {
            if (selectedIds.isNotEmpty()) {
                MultiSelectBottomBar(
                    selectedCount = selectedIds.size,
                    onCategorizeClick = { showBulkCategorizeSheet = true },
                    onChangeTypeClick = { showBulkTypeDialog = true },
                    onMarkReviewedClick = {
                        val selectedTxns = transactions.filter { selectedIds.contains(it.id) }
                        val updated = selectedTxns.map { it.copy(isReviewed = true) }
                        onUpdateTransactions(updated)
                        val count = selectedTxns.size
                        selectedIds = emptySet()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.multiselect_updated_count, count),
                            )
                        }
                    },
                    onDeleteClick = {
                        val selectedTxns = transactions.filter { selectedIds.contains(it.id) }
                        val updated =
                            selectedTxns.map {
                                it.copy(type = TransactionType.Ignored, isReviewed = true)
                            }
                        onUpdateTransactions(updated)
                        val count = selectedTxns.size
                        selectedIds = emptySet()
                        scope.launch {
                            val result =
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.multiselect_deleted_count, count),
                                    actionLabel = context.getString(DesignR.string.swipe_undo),
                                    duration = SnackbarDuration.Short,
                                )
                            if (result == SnackbarResult.ActionPerformed) {
                                onUpdateTransactions(selectedTxns)
                            }
                        }
                    },
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            SearchAndFilterHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedFilter = selectedQuickFilter,
                onFilterSelected = { quickFilter ->
                    currentFilter =
                        when (quickFilter) {
                            TransactionFilterType.ALL ->
                                currentFilter.copy(types = emptySet(), reviewStatus = ReviewStatusFilter.All)
                            TransactionFilterType.EXPENSE ->
                                currentFilter.copy(types = setOf(TransactionType.Expense))
                            TransactionFilterType.INCOME ->
                                currentFilter.copy(types = setOf(TransactionType.Income))
                            TransactionFilterType.TRANSFER ->
                                currentFilter.copy(types = setOf(TransactionType.Transfer))
                            TransactionFilterType.NEEDS_REVIEW ->
                                currentFilter.copy(reviewStatus = ReviewStatusFilter.NeedsReviewOnly)
                        }
                },
                activeFilterCount = activeFilter.activeFilterCount,
                onOpenFilterSheet = { showFilterSheet = true },
                suggestions = merchantSuggestions,
                onSuggestionClick = { selectedMerchant -> searchQuery = selectedMerchant },
            )

            if (activeFilter.isActive) {
                ActiveFilterSummaryStrip(
                    filter = activeFilter,
                    onClearAll = {
                        searchQuery = ""
                        currentFilter = TransactionFilter()
                    },
                )
            }

            SummaryStrip(
                incomeAmount = totalIncome,
                expenseAmount = totalExpense,
                netAmount = netTotal,
            )

            if (filteredTransactions.isEmpty()) {
                EmptyTransactionsState(
                    onClearFilters = {
                        searchQuery = ""
                        currentFilter = TransactionFilter()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    groupedByDay.forEach { (dateEpoch, dayTxns) ->
                        stickyHeader(key = dateEpoch) {
                            DayHeader(
                                dateEpoch = dateEpoch,
                                transactions = dayTxns,
                                todayText = todayStr,
                                yesterdayText = yesterdayStr,
                            )
                        }

                        items(dayTxns, key = { it.id }) { txn ->
                            val deletedMsg = stringResource(id = DesignR.string.swipe_deleted_message)
                            val undoMsg = stringResource(id = DesignR.string.swipe_undo)
                            val isSelected = selectedIds.contains(txn.id)
                            val isSelectionMode = selectedIds.isNotEmpty()

                            SwipeableTransactionRow(
                                transaction = txn,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds =
                                            if (isSelected) {
                                                selectedIds - txn.id
                                            } else {
                                                selectedIds + txn.id
                                            }
                                    } else {
                                        onTransactionClick(txn)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedIds = setOf(txn.id)
                                    }
                                },
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                enableSwipe = !isSelectionMode,
                                onDelete = { toDelete ->
                                    onDeleteTransaction(toDelete)
                                    scope.launch {
                                        val result =
                                            snackbarHostState.showSnackbar(
                                                message = deletedMsg,
                                                actionLabel = undoMsg,
                                                duration = SnackbarDuration.Short,
                                            )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onRestoreTransaction(toDelete)
                                        }
                                    }
                                },
                                onReview = { toReview ->
                                    onReviewTransaction(toReview.copy(isReviewed = true))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        AdvancedFilterSheet(
            transactions = transactions,
            initialFilter = currentFilter,
            onApply = { updatedFilter -> currentFilter = updatedFilter },
            onDismiss = { showFilterSheet = false },
        )
    }

    if (showBulkCategorizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBulkCategorizeSheet = false },
        ) {
            CategoryPickerSheetContent(
                selectedCategory = "",
                onCategorySelected = { chosenCat ->
                    val selectedTxns = transactions.filter { selectedIds.contains(it.id) }
                    val updated =
                        selectedTxns.map {
                            it.copy(categorySource = chosenCat, isReviewed = true)
                        }
                    onUpdateTransactions(updated)
                    val count = selectedTxns.size
                    selectedIds = emptySet()
                    showBulkCategorizeSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.multiselect_updated_count, count),
                        )
                    }
                },
                onDismiss = { showBulkCategorizeSheet = false },
            )
        }
    }

    if (showBulkTypeDialog) {
        var chosenType by remember { mutableStateOf(TransactionType.Expense) }
        AlertDialog(
            onDismissRequest = { showBulkTypeDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.multiselect_change_type_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Column {
                    listOf(
                        TransactionType.Expense,
                        TransactionType.Income,
                        TransactionType.Transfer,
                    ).forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { chosenType = type }
                                    .padding(vertical = 8.dp),
                        ) {
                            RadioButton(
                                selected = chosenType == type,
                                onClick = { chosenType = type },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = type.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedTxns = transactions.filter { selectedIds.contains(it.id) }
                        val updated = selectedTxns.map { it.copy(type = chosenType) }
                        onUpdateTransactions(updated)
                        val count = selectedTxns.size
                        selectedIds = emptySet()
                        showBulkTypeDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.multiselect_updated_count, count),
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkTypeDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun SearchAndFilterHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: TransactionFilterType?,
    onFilterSelected: (TransactionFilterType) -> Unit,
    activeFilterCount: Int = 0,
    onOpenFilterSheet: () -> Unit = {},
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(text = stringResource(id = R.string.transactions_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                        )
                    }
                }
            },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (suggestions.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = { Text(text = suggestion) },
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = activeFilterCount > 0,
                onClick = onOpenFilterSheet,
                label = {
                    Text(
                        text =
                            if (activeFilterCount > 0) {
                                "${stringResource(id = R.string.filter_button_all_filters)} ($activeFilterCount)"
                            } else {
                                stringResource(id = R.string.filter_button_all_filters)
                            },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )

            TransactionFilterType.entries.forEach { filter ->
                val label =
                    when (filter) {
                        TransactionFilterType.ALL -> stringResource(id = R.string.transactions_filter_all)
                        TransactionFilterType.EXPENSE -> stringResource(id = R.string.transactions_filter_expense)
                        TransactionFilterType.INCOME -> stringResource(id = R.string.transactions_filter_income)
                        TransactionFilterType.TRANSFER -> stringResource(id = R.string.transactions_filter_transfer)
                        TransactionFilterType.NEEDS_REVIEW ->
                            stringResource(id = R.string.transactions_filter_needs_review)
                    }
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(text = label) },
                    leadingIcon =
                        when (filter) {
                            TransactionFilterType.EXPENSE -> {
                                {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_trending_down),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            TransactionFilterType.INCOME -> {
                                {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_trending_up),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            TransactionFilterType.TRANSFER -> {
                                {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_category_transfer),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            TransactionFilterType.NEEDS_REVIEW -> {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            TransactionFilterType.ALL -> null
                        },
                )
            }
        }
    }
}

@Composable
fun ActiveFilterSummaryStrip(
    filter: TransactionFilter,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val parts = mutableListOf<String>()
            if (filter.types.isNotEmpty()) {
                parts.add(filter.types.joinToString("/") { it.name })
            }
            val range = filter.dateRange
            if (range !is DateRangeSelection.AllTime) {
                when (range) {
                    DateRangeSelection.Today -> parts.add(stringResource(id = R.string.filter_date_today))
                    DateRangeSelection.Yesterday -> parts.add(stringResource(id = R.string.transactions_yesterday))
                    DateRangeSelection.ThisWeek -> parts.add(stringResource(id = R.string.filter_date_this_week))
                    DateRangeSelection.ThisMonth -> parts.add(stringResource(id = R.string.filter_date_this_month))
                    DateRangeSelection.LastMonth -> parts.add(stringResource(id = R.string.filter_date_last_month))
                    is DateRangeSelection.LastNDays -> parts.add("Last ${range.days} Days")
                    is DateRangeSelection.Custom -> parts.add("Custom Date")
                    else -> Unit
                }
            }
            if (filter.categories.isNotEmpty()) {
                parts.add(filter.categories.joinToString(", "))
            }
            if (filter.amountRange !is AmountRangeFilter.Any) {
                parts.add(stringResource(id = R.string.filter_section_amount))
            }
            if (filter.channels.isNotEmpty()) {
                parts.add(filter.channels.joinToString(", ") { it.name })
            }
            val status = filter.reviewStatus
            if (status != ReviewStatusFilter.All) {
                val statusLabel =
                    if (status == ReviewStatusFilter.NeedsReviewOnly) {
                        stringResource(id = R.string.filter_status_needs_review)
                    } else {
                        stringResource(id = R.string.filter_status_reviewed)
                    }
                parts.add(statusLabel)
            }

            Text(
                text = "Active: ${parts.joinToString(" • ")}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onClearAll,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(id = R.string.transactions_clear_filters),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun SummaryStrip(
    incomeAmount: Long,
    expenseAmount: Long,
    netAmount: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isPrivacy = LocalPrivacyMode.current
            val masked = "₹ ••••••"
            val incomeText = if (isPrivacy) masked else MoneyFormatter.formatMinorUnits(incomeAmount)
            val expenseText = if (isPrivacy) masked else MoneyFormatter.formatMinorUnits(expenseAmount)
            val netText = if (isPrivacy) masked else MoneyFormatter.formatMinorUnits(netAmount)

            Text(
                text = stringResource(id = R.string.transactions_summary_income, incomeText),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = stringResource(id = R.string.transactions_summary_expense, expenseText),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = stringResource(id = R.string.transactions_summary_net, netText),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun DayHeader(
    dateEpoch: Long,
    transactions: List<Transaction>,
    todayText: String,
    yesterdayText: String,
    modifier: Modifier = Modifier,
) {
    val dayTotalMinor =
        transactions
            .filter { it.type == TransactionType.Expense }
            .sumOf { it.amount }

    val formattedDate = formatDate(dateEpoch, todayText, yesterdayText)
    val isPrivacy = LocalPrivacyMode.current
    val formattedTotal =
        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(dayTotalMinor)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (dayTotalMinor > 0) {
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsState(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.transactions_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.transactions_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClearFilters) {
            Text(text = stringResource(id = R.string.transactions_clear_filters))
        }
    }
}

fun filterTransactions(
    transactions: List<Transaction>,
    query: String,
    filter: TransactionFilterType,
): List<Transaction> {
    return transactions.filter { txn ->
        val matchesQuery = TransactionSearchHelper.matches(txn, query)

        val matchesFilter =
            when (filter) {
                TransactionFilterType.ALL -> true
                TransactionFilterType.EXPENSE -> txn.type == TransactionType.Expense
                TransactionFilterType.INCOME -> txn.type == TransactionType.Income
                TransactionFilterType.TRANSFER ->
                    txn.type == TransactionType.Transfer ||
                        txn.categorySource.equals("Transfer", ignoreCase = true)
                TransactionFilterType.NEEDS_REVIEW -> !txn.isReviewed
            }

        matchesQuery && matchesFilter
    }
}

fun groupTransactionsByDay(transactions: List<Transaction>): Map<Long, List<Transaction>> {
    return transactions
        .sortedByDescending { it.dateTime }
        .groupBy { txn ->
            val date = Instant.ofEpochMilli(txn.dateTime).atZone(ZoneId.systemDefault()).toLocalDate()
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
}

fun formatDate(
    epochMillis: Long,
    todayText: String,
    yesterdayText: String,
): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> todayText
        today.minusDays(1) -> yesterdayText
        else -> {
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
            date.format(formatter)
        }
    }
}
