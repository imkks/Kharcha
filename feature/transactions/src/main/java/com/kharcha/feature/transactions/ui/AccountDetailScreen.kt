package com.kharcha.feature.transactions.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionFilterMatcher
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.DonutChart
import com.kharcha.core.designsystem.component.SpendRing
import com.kharcha.core.designsystem.component.SwipeableTransactionRow
import com.kharcha.core.designsystem.component.buildCategorySlices
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.isCounted
import com.kharcha.feature.transactions.R
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    account: Account,
    allAccounts: List<Account>,
    allTransactions: List<Transaction>,
    onBack: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onSaveAccount: (Account) -> Unit,
    modifier: Modifier = Modifier,
    onMergeAccounts: ((primaryAccountId: Long, redundantAccountIds: Set<Long>) -> Unit)? = null,
    onAddTransactionClick: () -> Unit = {},
    onSetBudgetClick: () -> Unit = {},
) {
    BackHandler { onBack() }

    var selectedSubTab by rememberSaveable { mutableStateOf(TransactionsSubTab.TRANSACTIONS) }
    var showMenuOptionsSheet by remember { mutableStateOf(false) }
    var showAccountSettingsSheet by remember { mutableStateOf(false) }
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var showLinkAccountsSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showMonthPickerSheet by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(TransactionFilter()) }

    val allAccountIds = remember(account, allAccounts) {
        val linkedIds = allAccounts.filter { it.linkedPrimaryAccountId == account.id }.map { it.id }
        (setOf(account.id) + linkedIds).toSet()
    }

    val accountTransactions = remember(allTransactions, allAccountIds) {
        allTransactions.filter { it.accountId in allAccountIds }
    }

    val availableMonths = remember(accountTransactions) {
        val months = accountTransactions.map { txn ->
            val date = Instant.ofEpochMilli(txn.dateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            YearMonth.from(date)
        }.distinct().sortedDescending()
        if (months.isEmpty()) listOf(YearMonth.now()) else months
    }

    var selectedYearMonth by rememberSaveable {
        mutableStateOf(availableMonths.firstOrNull() ?: YearMonth.now())
    }
    var isAllMonthsSelected by rememberSaveable { mutableStateOf(false) }

    val filteredTransactions = remember(accountTransactions, currentFilter) {
        accountTransactions.filter { TransactionFilterMatcher.matches(it, currentFilter) }
    }

    val allMonthTransactions = remember(accountTransactions, selectedYearMonth, isAllMonthsSelected) {
        if (isAllMonthsSelected) {
            accountTransactions
        } else {
            accountTransactions.filter { txn ->
                val date = Instant.ofEpochMilli(txn.dateTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                YearMonth.from(date) == selectedYearMonth
            }
        }
    }

    val monthTransactions = remember(filteredTransactions, selectedYearMonth, isAllMonthsSelected) {
        if (isAllMonthsSelected) {
            filteredTransactions
        } else {
            filteredTransactions.filter { txn ->
                val date = Instant.ofEpochMilli(txn.dateTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                YearMonth.from(date) == selectedYearMonth
            }
        }
    }

    val monthExpensePaise = remember(allMonthTransactions) {
        allMonthTransactions
            .filter { it.type == TransactionType.Expense && it.isCounted }
            .sumOf { it.amount }
    }

    val monthIncomePaise = remember(allMonthTransactions) {
        allMonthTransactions
            .filter { it.type == TransactionType.Income && it.isCounted }
            .sumOf { it.amount }
    }

    val sortedMonthTxns = remember(monthTransactions) {
        monthTransactions.sortedByDescending { it.dateTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.account_settings_cancel),
                            tint = Color.White,
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMonthPickerSheet = true }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                    ) {
                        // Bank icon square
                        val iconRes = when (account.type) {
                            AccountType.CreditCard -> DesignR.drawable.ic_channel_card
                            AccountType.Wallet -> DesignR.drawable.ic_channel_wallet
                            else -> DesignR.drawable.ic_bank
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.financialColors.cardBackground,
                            border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.limeAccent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val displayName = buildString {
                                append(account.bankName ?: account.name)
                                if (!account.last4.isNullOrBlank()) {
                                    append(" - ")
                                    append(account.last4)
                                }
                            }
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val monthLabel = if (isAllMonthsSelected) {
                                    stringResource(id = R.string.transactions_all_months)
                                } else {
                                    selectedYearMonth.format(
                                        DateTimeFormatter.ofPattern("MMM", Locale.getDefault()),
                                    )
                                }
                                Text(
                                    text = monthLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.textSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_channel_card),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_calendar),
                            contentDescription = stringResource(id = R.string.transactions_action_filter),
                            tint =
                                if (currentFilter.isActive) {
                                    MaterialTheme.financialColors.limeAccent
                                } else {
                                    Color.White
                                },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = { showMenuOptionsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.account_detail_menu_title),
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Three Tabs: Transactions, Categories, Merchants
            TabRow(
                selectedTabIndex = selectedSubTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.financialColors.limeAccent,
                indicator = { tabPositions ->
                    if (selectedSubTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSubTab.ordinal]),
                            color = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
                },
            ) {
                TransactionsSubTab.entries.forEach { tab ->
                    val isSelected = selectedSubTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSubTab = tab },
                        text = {
                            Text(
                                text = stringResource(id = tab.titleRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.financialColors.limeAccent
                                } else {
                                    MaterialTheme.financialColors.textSecondary
                                },
                            )
                        },
                    )
                }
            }

            when (selectedSubTab) {
                TransactionsSubTab.TRANSACTIONS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 1. Spend Ring Hero
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                SpendRing(
                                    spentMinor = monthExpensePaise,
                                    monthlyBudgetMinor = 0L,
                                    size = 190.dp,
                                    strokeWidth = 14.dp,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // 2. Three Columns: Income | Set monthly budget | Balance
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.financialColors.cardBackground,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    // Column 1: Income
                                    val isIncomeSelected = currentFilter.types == setOf(TransactionType.Income)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                currentFilter =
                                                    if (isIncomeSelected) {
                                                        currentFilter.copy(types = emptySet())
                                                    } else {
                                                        currentFilter.copy(types = setOf(TransactionType.Income))
                                                    }
                                            }
                                            .padding(4.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.transactions_hero_income),
                                            style = MaterialTheme.typography.bodySmall,
                                            color =
                                                if (isIncomeSelected) {
                                                    MaterialTheme.financialColors.income
                                                } else {
                                                    MaterialTheme.financialColors.textSecondary
                                                },
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = MoneyFormatter.formatMinorUnits(monthIncomePaise),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color =
                                                if (isIncomeSelected) {
                                                    MaterialTheme.financialColors.income
                                                } else {
                                                    Color.White
                                                },
                                        )
                                    }

                                    VerticalDivider(
                                        modifier = Modifier.height(36.dp),
                                        color = MaterialTheme.financialColors.hairlineDivider,
                                    )

                                    // Column 2: Set monthly budget
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSetBudgetClick() }
                                            .padding(4.dp),
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.financialColors.cardBackground,
                                            border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    painter = painterResource(id = DesignR.drawable.ic_category_bills),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.financialColors.limeAccent,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(id = R.string.transactions_hero_set_budget),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.financialColors.textSecondary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                        )
                                    }

                                    VerticalDivider(
                                        modifier = Modifier.height(36.dp),
                                        color = MaterialTheme.financialColors.hairlineDivider,
                                    )

                                    // Column 3: Balance
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.account_detail_balance),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.financialColors.textSecondary,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = MoneyFormatter.formatMinorUnits(account.currentBalance ?: 0L),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // 3. Transactions List
                        if (sortedMonthTxns.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.transactions_empty_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(sortedMonthTxns, key = { _, item -> item.id }) { _, txn ->
                                SwipeableTransactionRow(
                                    transaction = txn,
                                    onClick = { onTransactionClick(txn) },
                                    onDelete = { },
                                    onReview = { },
                                )
                            }
                        }
                    }
                }

                TransactionsSubTab.CATEGORIES -> {
                    val categorySlices = remember(monthTransactions) {
                        buildCategorySlices(
                            monthTransactions.filter { it.type == TransactionType.Expense },
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (categorySlices.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.transactions_empty_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                }
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    DonutChart(
                                        slices = categorySlices,
                                        totalAmountMinor = monthExpensePaise,
                                        onCategoryClick = { },
                                        chartSize = 160.dp,
                                    )
                                }
                            }
                            itemsIndexed(categorySlices, key = { _, s -> s.categoryName }) { _, slice ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.financialColors.cardBackground)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryBadge(categoryName = slice.categoryName, size = 36.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = slice.categoryName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                            )
                                            Text(
                                                text = "${(slice.percentage * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.financialColors.textSecondary,
                                            )
                                        }
                                    }
                                    Text(
                                        text = MoneyFormatter.formatMinorUnits(slice.amountMinor),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                TransactionsSubTab.MERCHANTS -> {
                    val merchantList = remember(monthTransactions) {
                        monthTransactions
                            .filter { it.type == TransactionType.Expense }
                            .groupBy { it.merchantNormalised ?: it.merchantRaw ?: "Unknown" }
                            .map { (merchant, txns) ->
                                merchant to txns.sumOf { it.amount }
                            }
                            .sortedByDescending { it.second }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (merchantList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.transactions_empty_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(merchantList, key = { _, (m, _) -> m }) { _, (merchant, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.financialColors.cardBackground)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.financialColors.cardBackground,
                                            border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    painter =
                                                        painterResource(
                                                            id = DesignR.drawable.ic_category_shopping,
                                                        ),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.financialColors.limeAccent,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = merchant,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                        )
                                    }
                                    Text(
                                        text = MoneyFormatter.formatMinorUnits(amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Menu options Bottom Sheet (Image 1)
    if (showMenuOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuOptionsSheet = false },
            containerColor = MaterialTheme.financialColors.cardBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.account_detail_menu_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                // Option 1: Export statement
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showMenuOptionsSheet = false
                            showExportSheet = true
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.account_detail_menu_export),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }

                // Option 2: Link account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showMenuOptionsSheet = false
                            showLinkAccountsSheet = true
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.account_detail_menu_link),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }

                // Option 3: Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showMenuOptionsSheet = false
                            showAccountSettingsSheet = true
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.account_detail_menu_settings),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }

                // Option 4: Add spend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showMenuOptionsSheet = false
                            onAddTransactionClick()
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.account_detail_menu_add_spend),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }

    // 2. Account settings Bottom Sheet (Image 2)
    if (showAccountSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSettingsSheet = false },
            containerColor = MaterialTheme.financialColors.cardBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.account_settings_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                // Item 1: Expense
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSaveAccount(account.copy(isCountedAsExpense = !account.isCountedAsExpense))
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.account_settings_expense_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.account_settings_expense_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                    Switch(
                        checked = account.isCountedAsExpense,
                        onCheckedChange = {
                            onSaveAccount(account.copy(isCountedAsExpense = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.financialColors.limeAccent,
                        ),
                    )
                }

                HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                // Item 2: Edit account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAccountSettingsSheet = false
                            showEditAccountDialog = true
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.account_settings_edit_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.account_settings_edit_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                // Item 3: Hide account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSaveAccount(account.copy(isArchived = !account.isArchived))
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.account_settings_hide_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.account_settings_hide_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint =
                            if (account.isArchived) {
                                MaterialTheme.financialColors.limeAccent
                            } else {
                                MaterialTheme.financialColors.textSecondary
                            },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    // 3. Edit Account Dialog
    if (showEditAccountDialog) {
        var editName by remember { mutableStateOf(account.name) }
        var editLast4 by remember { mutableStateOf(account.last4 ?: "") }

        AlertDialog(
            onDismissRequest = { showEditAccountDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.account_settings_edit_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(id = R.string.account_settings_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editLast4,
                        onValueChange = { if (it.length <= 4) editLast4 = it },
                        label = { Text(stringResource(id = R.string.account_settings_last4_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveAccount(
                            account.copy(
                                name = editName.trim().ifEmpty { account.name },
                                last4 = editLast4.trim().ifEmpty { null },
                            ),
                        )
                        showEditAccountDialog = false
                    },
                ) {
                    Text(
                        text = stringResource(id = R.string.account_settings_save),
                        color = MaterialTheme.financialColors.limeAccent,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAccountDialog = false }) {
                    Text(text = stringResource(id = R.string.account_settings_cancel))
                }
            },
        )
    }

    // 4. Link Accounts Sheet for this account
    if (showLinkAccountsSheet) {
        val candidateAccounts = remember(allAccounts, account) {
            allAccounts.filter { it.id != account.id }
        }
        var selectedSubAccountIds by remember(account, candidateAccounts) {
            mutableStateOf(
                candidateAccounts.filter { it.linkedPrimaryAccountId == account.id }.map { it.id }.toSet(),
            )
        }

        ModalBottomSheet(
            onDismissRequest = { showLinkAccountsSheet = false },
            containerColor = MaterialTheme.financialColors.cardBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.account_detail_menu_link),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Select cards or sub-accounts to link with ${account.name}. Unchecking will unlink them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.financialColors.textSecondary,
                )

                if (candidateAccounts.isEmpty()) {
                    Text(
                        text = "No other accounts available to link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.financialColors.textSecondary,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    candidateAccounts.forEach { candidate ->
                        val isChecked = candidate.id in selectedSubAccountIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedSubAccountIds = if (isChecked) {
                                        selectedSubAccountIds - candidate.id
                                    } else {
                                        selectedSubAccountIds + candidate.id
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedSubAccountIds = if (checked) {
                                            selectedSubAccountIds + candidate.id
                                        } else {
                                            selectedSubAccountIds - candidate.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.financialColors.limeAccent,
                                        checkmarkColor = MaterialTheme.financialColors.onLimeAccent,
                                    ),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = candidate.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                    )
                                    val subText = "${candidate.bankName ?: ""} ••${candidate.last4 ?: "----"}"
                                    Text(
                                        text = subText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showLinkAccountsSheet = false }) {
                        Text(text = stringResource(id = R.string.account_settings_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onMergeAccounts?.invoke(account.id, selectedSubAccountIds)
                            showLinkAccountsSheet = false
                        },
                    ) {
                        Text(
                            text = stringResource(id = R.string.account_settings_save),
                            color = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                }
            }
        }
    }

    // 5. Month Picker Sheet
    if (showMonthPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMonthPickerSheet = false },
            containerColor = MaterialTheme.financialColors.cardBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.transactions_select_month),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                // All months option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            isAllMonthsSelected = true
                            showMonthPickerSheet = false
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(id = R.string.transactions_all_months),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isAllMonthsSelected) MaterialTheme.financialColors.limeAccent else Color.White,
                    )
                    if (isAllMonthsSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                }
                availableMonths.forEach { ym ->
                    val isSelected = !isAllMonthsSelected && ym == selectedYearMonth
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isAllMonthsSelected = false
                                selectedYearMonth = ym
                                showMonthPickerSheet = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.financialColors.limeAccent else Color.White,
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.limeAccent,
                            )
                        }
                    }
                }
            }
        }
    }

    // 6. Advanced Filter Sheet
    if (showFilterSheet) {
        AdvancedFilterSheet(
            transactions = accountTransactions,
            initialFilter = currentFilter,
            onApply = { newFilter ->
                currentFilter = newFilter
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }

    // 7. Export Statement Sheet
    if (showExportSheet) {
        val monthLabel = if (isAllMonthsSelected) {
            stringResource(id = R.string.transactions_all_months)
        } else {
            selectedYearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
        }
        ExportStatementSheet(
            monthTransactions = monthTransactions,
            allTransactions = accountTransactions,
            filteredTransactions = filteredTransactions,
            isFilterActive = currentFilter.isActive,
            selectedMonthLabel = monthLabel,
            onDismiss = { showExportSheet = false },
        )
    }
}
