package com.kharcha.feature.transactions.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.TransactionRow
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TransactionFilterType.ALL) }

    val filteredTransactions =
        remember(transactions, searchQuery, selectedFilter) {
            filterTransactions(transactions, searchQuery, selectedFilter)
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

    val todayStr = stringResource(id = R.string.transactions_today)
    val yesterdayStr = stringResource(id = R.string.transactions_yesterday)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.transactions_title)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
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
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
            )

            SummaryStrip(
                incomeAmount = totalIncome,
                expenseAmount = totalExpense,
                netAmount = netTotal,
            )

            if (filteredTransactions.isEmpty()) {
                EmptyTransactionsState(
                    onClearFilters = {
                        searchQuery = ""
                        selectedFilter = TransactionFilterType.ALL
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
                            TransactionRow(
                                transaction = txn,
                                onClick = { onTransactionClick(txn) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchAndFilterHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: TransactionFilterType,
    onFilterSelected: (TransactionFilterType) -> Unit,
    modifier: Modifier = Modifier,
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

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            val incomeText = MoneyFormatter.formatMinorUnits(incomeAmount)
            val expenseText = MoneyFormatter.formatMinorUnits(expenseAmount)
            val netText = MoneyFormatter.formatMinorUnits(netAmount)

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
    val formattedTotal = MoneyFormatter.formatMinorUnits(dayTotalMinor)

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
    val trimmed = query.trim().lowercase(Locale.getDefault())
    return transactions.filter { txn ->
        val matchesQuery =
            if (trimmed.isEmpty()) {
                true
            } else {
                (txn.merchantRaw?.lowercase(Locale.getDefault())?.contains(trimmed) == true) ||
                    (txn.merchantNormalised?.lowercase(Locale.getDefault())?.contains(trimmed) == true) ||
                    (txn.refNumber?.lowercase(Locale.getDefault())?.contains(trimmed) == true) ||
                    (txn.channel?.name?.lowercase(Locale.getDefault())?.contains(trimmed) == true) ||
                    (txn.categorySource?.lowercase(Locale.getDefault())?.contains(trimmed) == true)
            }

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
