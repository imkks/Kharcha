package com.kharcha.feature.transactions.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionFilterMatcher
import com.kharcha.core.common.TransactionSearchHelper
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.DonutChart
import com.kharcha.core.designsystem.component.SwipeableTransactionRow
import com.kharcha.core.designsystem.component.buildCategorySlices
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.AmountRangeFilter
import com.kharcha.core.model.DateRangeSelection
import com.kharcha.core.model.ReviewStatusFilter
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
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

enum class TransactionsSubTab(val titleRes: Int) {
    TRANSACTIONS(R.string.transactions_tab_transactions),
    CATEGORIES(R.string.transactions_tab_categories),
    MERCHANTS(R.string.transactions_tab_merchants),
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
    monthlyBudgetMinor: Long = 0L,
    onSetBudgetClick: () -> Unit = {},
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
    var showMonthPickerSheet by remember { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var showBulkCategorizeSheet by remember { mutableStateOf(false) }
    var showBulkTypeDialog by remember { mutableStateOf(false) }
    var selectedSubTab by rememberSaveable { mutableStateOf(TransactionsSubTab.TRANSACTIONS) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val availableMonths =
        remember(transactions) {
            val months =
                transactions
                    .map { txn ->
                        val date =
                            Instant.ofEpochMilli(txn.dateTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        YearMonth.from(date)
                    }
                    .distinct()
                    .sortedDescending()
            if (months.isEmpty()) listOf(YearMonth.now()) else months
        }

    var selectedYearMonth by rememberSaveable {
        mutableStateOf(availableMonths.firstOrNull() ?: YearMonth.now())
    }
    var isAllMonthsSelected by rememberSaveable { mutableStateOf(false) }

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

    // Filter by selected month
    val monthTransactions =
        remember(filteredTransactions, selectedYearMonth, isAllMonthsSelected) {
            if (isAllMonthsSelected) {
                filteredTransactions
            } else {
                filteredTransactions.filter { txn ->
                    val date =
                        Instant.ofEpochMilli(txn.dateTime)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    YearMonth.from(date) == selectedYearMonth
                }
            }
        }

    val monthIncome =
        remember(monthTransactions) {
            monthTransactions
                .filter { it.type == TransactionType.Income }
                .sumOf { it.amount }
        }

    val monthExpense =
        remember(monthTransactions) {
            monthTransactions
                .filter { it.type == TransactionType.Expense }
                .sumOf { it.amount }
        }

    val sortedMonthTxns =
        remember(monthTransactions) {
            monthTransactions.sortedByDescending { it.dateTime }
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedIds.isNotEmpty()) {
                val allVisibleSelected =
                    monthTransactions.isNotEmpty() &&
                        monthTransactions.all { selectedIds.contains(it.id) }
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
                                        monthTransactions.map { it.id }.toSet()
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
                    title = {
                        Column(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showMonthPickerSheet = true }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.transactions_all_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val monthLabel =
                                    if (isAllMonthsSelected) {
                                        stringResource(id = R.string.transactions_all_months)
                                    } else {
                                        selectedYearMonth.format(
                                            DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()),
                                        )
                                    }
                                Text(
                                    text = monthLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.financialColors.income,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(id = R.string.transactions_select_month),
                                    tint = MaterialTheme.financialColors.income,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(id = R.string.filter_button_all_filters),
                            )
                        }
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
            // 1. Sub-Tabs Row: Transactions | Categories | Merchants
            TabRow(
                selectedTabIndex = selectedSubTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.financialColors.income,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab.ordinal]),
                        color = MaterialTheme.financialColors.income,
                        height = 3.dp,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                TransactionsSubTab.entries.forEach { tab ->
                    val isSelected = selectedSubTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSubTab = tab },
                        text = {
                            Text(
                                text = stringResource(id = tab.titleRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color =
                                    if (isSelected) {
                                        MaterialTheme.financialColors.income
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        },
                    )
                }
            }

            // 2. Hero Month Circular Ring & Stats Overview
            HeroMonthSummary(
                totalExpenseMinor = monthExpense,
                totalIncomeMinor = monthIncome,
                monthlyBudgetMinor = monthlyBudgetMinor,
                onSetBudgetClick = onSetBudgetClick,
            )

            // 3. Tab Content in Rounded Container
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
            ) {
                when (selectedSubTab) {
                    TransactionsSubTab.TRANSACTIONS -> {
                        if (sortedMonthTxns.isEmpty()) {
                            EmptyTransactionsState(
                                onClearFilters = {
                                    searchQuery = ""
                                    currentFilter = TransactionFilter()
                                    isAllMonthsSelected = true
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                items(sortedMonthTxns, key = { it.id }) { txn ->
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

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                    TransactionsSubTab.CATEGORIES -> {
                        CategoriesTabContent(
                            transactions = monthTransactions,
                            onCategoryClick = { category ->
                                currentFilter = currentFilter.copy(categories = setOf(category))
                                selectedSubTab = TransactionsSubTab.TRANSACTIONS
                            },
                        )
                    }
                    TransactionsSubTab.MERCHANTS -> {
                        MerchantsTabContent(
                            transactions = monthTransactions,
                            onMerchantClick = { merchant ->
                                searchQuery = merchant
                                selectedSubTab = TransactionsSubTab.TRANSACTIONS
                            },
                        )
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

    if (showMonthPickerSheet) {
        MonthPickerSheet(
            availableMonths = availableMonths,
            selectedYearMonth = selectedYearMonth,
            isAllMonthsSelected = isAllMonthsSelected,
            transactions = transactions,
            onSelectMonth = { ym ->
                selectedYearMonth = ym
                isAllMonthsSelected = false
                showMonthPickerSheet = false
            },
            onSelectAllMonths = {
                isAllMonthsSelected = true
                showMonthPickerSheet = false
            },
            onDismiss = { showMonthPickerSheet = false },
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
fun HeroMonthSummary(
    totalExpenseMinor: Long,
    totalIncomeMinor: Long,
    monthlyBudgetMinor: Long,
    onSetBudgetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPrivacy = LocalPrivacyMode.current
    val expenseFormatted =
        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(totalExpenseMinor)
    val incomeFormatted =
        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(totalIncomeMinor)
    val budgetFormatted =
        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(monthlyBudgetMinor)

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero Circular Ring Gauge
        Box(
            modifier =
                Modifier
                    .size(176.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f))
                    .border(
                        width = 8.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = expenseFormatted,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Stats Row: Income | Monthly Budget
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Income
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(id = R.string.transactions_hero_income),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = incomeFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            VerticalDivider(
                modifier = Modifier.height(32.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // Monthly budget
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable(enabled = onSetBudgetClick != {}) { onSetBudgetClick() },
            ) {
                Text(
                    text = stringResource(id = R.string.transactions_hero_budget),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (monthlyBudgetMinor > 0L) {
                    Text(
                        text = budgetFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.transactions_hero_set_budget),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriesTabContent(
    transactions: List<Transaction>,
    onCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val expenses = transactions.filter { it.type == TransactionType.Expense && it.amount > 0 }
    val totalExpense = expenses.sumOf { it.amount }
    val slices = remember(transactions) { buildCategorySlices(transactions) }

    if (expenses.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.transactions_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DonutChart(
                    slices = slices,
                    totalAmountMinor = totalExpense,
                    onCategoryClick = onCategoryClick,
                )
            }
            items(slices) { slice ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { onCategoryClick(slice.categoryName) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryBadge(categoryName = slice.categoryName, size = 32.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = slice.categoryName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "${"%.1f".format(slice.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = MoneyFormatter.formatMinorUnits(slice.amountMinor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantsTabContent(
    transactions: List<Transaction>,
    onMerchantClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val expenses = transactions.filter { it.type == TransactionType.Expense && it.amount > 0 }
    val topMerchants =
        remember(expenses) {
            expenses
                .groupBy { it.merchantNormalised ?: it.merchantRaw ?: "Unknown" }
                .map { (merchant, txns) ->
                    Triple(merchant, txns.sumOf { it.amount }, txns.size)
                }
                .sortedByDescending { it.second }
        }

    if (topMerchants.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.transactions_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(topMerchants) { (merchant, amountMinor, count) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { onMerchantClick(merchant) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = merchant,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "$count transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = MoneyFormatter.formatMinorUnits(amountMinor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.expense,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerSheet(
    availableMonths: List<YearMonth>,
    selectedYearMonth: YearMonth,
    isAllMonthsSelected: Boolean,
    transactions: List<Transaction>,
    onSelectMonth: (YearMonth) -> Unit,
    onSelectAllMonths: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(id = R.string.transactions_select_month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectAllMonths() },
                color =
                    if (isAllMonthsSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.transactions_all_months),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isAllMonthsSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (isAllMonthsSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(availableMonths) { ym ->
                    val isSelected = !isAllMonthsSelected && ym == selectedYearMonth
                    val monthLabel =
                        ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                    val monthTxns =
                        transactions.filter {
                            val d =
                                Instant.ofEpochMilli(it.dateTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            YearMonth.from(d) == ym
                        }
                    val monthSpent =
                        monthTxns
                            .filter { it.type == TransactionType.Expense }
                            .sumOf { it.amount }
                    val isPrivacy = LocalPrivacyMode.current
                    val formattedSpent =
                        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(monthSpent)

                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectMonth(ym) },
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = monthLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formattedSpent,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun groupTransactionsByMonth(transactions: List<Transaction>): Map<YearMonth, List<Transaction>> {
    return transactions
        .sortedByDescending { it.dateTime }
        .groupBy { txn ->
            val date = Instant.ofEpochMilli(txn.dateTime).atZone(ZoneId.systemDefault()).toLocalDate()
            YearMonth.from(date)
        }
        .toSortedMap(compareByDescending { it })
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
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
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
